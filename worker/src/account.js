import { HttpError } from "./google.js";

/** How recently the user must have typed their password to delete their account. */
export const RECENT_LOGIN_SECONDS = 5 * 60;

/** What other people see in place of a deleted account's name. */
export const DELETED_NAME = "Deleted user";

/**
 * Removes [uid] from the estate they're in, then everything they made: posts (with the comments
 * on them), comments elsewhere, likes, join requests, notifications, blocks and the profile. Their
 * chats stay for the other person, relabelled as a deleted user. The sign-in account goes last,
 * so a failure part-way leaves something the user can retry, and every step is safe to repeat.
 *
 * Refuses, before touching anything, when [uid] owns an estate that still has members and no
 * other admin, the same rule as leaving it.
 */
export async function deleteAccount(db, uid, { deleteAuthUser, deleteMedia = async () => {} }) {
  const user = await db.get(`users/${uid}`);
  const writes = [];

  if (user?.estateId) writes.push(...(await leaveEstateWrites(db, uid, user.estateId)));

  const [posts, myComments, liked, requests, notifications, chats, blocks] = await Promise.all([
    db.query("posts", [["authorId", "EQUAL", uid]]),
    db.query("comments", [["authorId", "EQUAL", uid]], { group: true }),
    db.query("posts", [["likedBy", "ARRAY_CONTAINS", uid]]),
    db.query("joinRequests", [["userId", "EQUAL", uid]]),
    db.query("notifications", [["recipientId", "EQUAL", uid]]),
    db.query("chats", [["participants", "ARRAY_CONTAINS", uid]]),
    db.query("blocks", [], { parent: `users/${uid}` }),
  ]);

  const deletedPosts = new Set(posts.map((p) => p.path));
  // Other people's comments on the posts going away; `IN` takes at most 30 values.
  const postIds = posts.map((p) => p.path.split("/")[1]);
  const commentsOnPosts = [];
  for (let i = 0; i < postIds.length; i += 30) {
    commentsOnPosts.push(...(await db.query("comments", [["postId", "IN", postIds.slice(i, i + 30)]], { group: true })));
  }

  const deleted = new Set();
  const remove = (path) => {
    if (!deleted.has(path)) writes.push({ delete: path });
    deleted.add(path);
  };

  for (const c of [...commentsOnPosts, ...myComments]) remove(c.path);
  for (const p of posts) remove(p.path);

  // Counters on posts that stay up.
  const lostComments = {};
  for (const c of myComments) {
    const post = c.path.split("/").slice(0, 2).join("/");
    if (!deletedPosts.has(post)) lostComments[post] = (lostComments[post] || 0) + 1;
  }
  for (const [post, n] of Object.entries(lostComments)) writes.push({ transform: post, increment: { commentCount: -n } });
  for (const p of liked) {
    if (!deletedPosts.has(p.path)) {
      writes.push({ transform: p.path, increment: { likeCount: -1 }, arrayRemove: { likedBy: [uid] } });
    }
  }

  for (const r of requests) remove(r.path);
  for (const n of notifications) remove(n.path);
  for (const b of blocks) remove(b.path);

  // The other person keeps the conversation but can't reply into the void.
  for (const chat of chats) {
    writes.push({
      update: chat.path,
      fields: {
        [`participantNames.${uid}`]: DELETED_NAME,
        deletedParticipants: [...new Set([...(chat.data.deletedParticipants || []), uid])],
      },
    });
  }

  if (user) remove(`users/${uid}`);

  await db.commit(writes);
  // Photos are a nicety to clean up; their loss must not block deleting the account.
  await deleteMedia(uid).catch((e) => console.error("Media cleanup failed", uid, e));
  await deleteAuthUser(uid);
  return { deleted: true };
}

/** The writes that take [uid] out of [estateId], following the same rules as leaving in the app. */
async function leaveEstateWrites(db, uid, estateId) {
  const estatePath = `estates/${estateId}`;
  const estate = await db.get(estatePath);
  if (!estate) return [];

  const admins = estate.adminIds || [];
  const isOwner = (estate.ownerId || admins[0]) === uid;

  if (isOwner) {
    const successor = admins.find((id) => id !== uid);
    if (successor) {
      return [
        { update: estatePath, fields: { ownerId: successor } },
        { transform: estatePath, increment: { memberCount: -1 }, arrayRemove: { adminIds: [uid] } },
      ];
    }
    const members = await db.query("users", [["estateId", "EQUAL", estateId]]);
    if (members.some((m) => m.path !== `users/${uid}`)) {
      throw new HttpError(
        409,
        "You own this estate and nobody else is an admin. Make another member an admin, then delete your account."
      );
    }
    // Last one out: the estate, its invite and the code lookup all go.
    const invite = await db.get(`${estatePath}/private/invite`);
    return [
      ...(invite?.code ? [{ delete: `inviteCodes/${invite.code}` }] : []),
      { delete: `${estatePath}/private/invite` },
      { delete: estatePath },
    ];
  }

  const change = { transform: estatePath, increment: { memberCount: -1 } };
  if (admins.includes(uid)) change.arrayRemove = { adminIds: [uid] };
  return [change];
}

/** Rejects a delete unless the password was entered in the last few minutes. */
export function requireRecentLogin(authTime, now = Date.now()) {
  if (now / 1000 - authTime > RECENT_LOGIN_SECONDS) {
    throw new HttpError(401, "Please enter your password again");
  }
}
