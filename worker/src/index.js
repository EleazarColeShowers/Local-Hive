import { HttpError, serviceAccessToken, verifyAppCheck, verifyFirebaseUser } from "./google.js";
import { Firestore } from "./firestore.js";
import { oversizedVideoIds, signUpload, UPLOAD_KINDS } from "./cloudinary.js";
import { sendToUsers } from "./push.js";
import { deleteAccount, requireRecentLogin } from "./account.js";
import { publicResponse } from "./links.js";

/**
 * LocalHive's backend on Cloudflare's free tier, doing what Cloud Functions would on Firebase's
 * paid plan:
 *
 *   POST /upload-signature  { kind: "post" | "profile" | "video" | "receipt" } -> Cloudinary signed-upload params
 *   POST /notify            { type: "message", chatId, messageId }
 *                           { type: "notification", notificationId }
 *                           { type: "announcement", postId }
 *                           { type: "emergency", postId }
 *                           { type: "security_report", postId }
 *                           { type: "poll", estateId, pollId }
 *                           { type: "light_back", estateId }
 *                           { type: "sos_responding", postId }
 *                           { type: "sos_safe", postId }
 *                           { type: "levy_reminder", estateId, levyId } -> sends the push
 *   POST /delete-account    {}                                       -> deletes the caller's account
 *   GET  /join/{code}, /.well-known/assetlinks.json                  -> invite links (see links.js)
 *   GET  /delete-account                                             -> delete an account from the web
 *
 * Once an hour (a cron trigger) it also reminds members about polls closing within a day, and
 * fills in the lowercase name estate search runs on for estates that lack it.
 *
 * Every POST needs a Firebase ID token. /notify never trusts the request body for content:
 * it reads the message, notification or post itself, checks the caller is its sender, and
 * sends each push at most once.
 */
export default {
  async fetch(request, env) {
    if (request.method === "GET") return publicResponse(request, env) ?? json({ error: "Not found" }, 404);
    if (request.method !== "POST") return json({ error: "Not found" }, 404);
    try {
      const { uid, authTime } = await verifyFirebaseUser(request, env.FIREBASE_PROJECT_ID);
      const path = new URL(request.url).pathname;
      // The web deletion page can't get an App Check token. Deleting needs the password typed
      // in the last few minutes anyway, which is the check that matters there.
      if (path !== "/delete-account") await verifyAppCheck(request, env);
      const body = await request.json().catch(() => ({}));

      if (path === "/upload-signature") return json(await uploadSignature(env, uid, body));
      if (path === "/notify") return json(await notify(env, uid, body));
      if (path === "/delete-account") return json(await removeAccount(env, uid, authTime));
      return json({ error: "Not found" }, 404);
    } catch (e) {
      const status = e instanceof HttpError ? e.status : 500;
      if (status === 500) console.error(e);
      return json({ error: status === 500 ? "Server error" : e.message }, status);
    }
  },

  async scheduled(event, env, ctx) {
    ctx.waitUntil(sendPollReminders(env, event.scheduledTime));
    ctx.waitUntil(fillEstateNameLower(env));
    ctx.waitUntil(deleteOversizedVideos(env));
  },
};

/**
 * Estate search runs on nameLower, so an estate without it can't be found. Estates from before
 * it existed, or created by an older app, get it here. Matches what the app and rules write.
 */
export async function estateNameLowerUpdates(db) {
  return (await db.query("estates"))
    .filter(({ data }) => typeof data.name === "string" && data.nameLower !== data.name.toLowerCase())
    .map(({ path, data }) => ({ update: path, fields: { nameLower: data.name.toLowerCase() } }));
}

async function fillEstateNameLower(env) {
  const db = new Firestore(env.FIREBASE_PROJECT_ID, await serviceAccessToken(env.FIREBASE_SERVICE_ACCOUNT));
  const writes = await estateNameLowerUpdates(db);
  if (writes.length === 0) return;
  await db.commit(writes);
  console.log(`nameLower: filled in for ${writes.length} estates`);
}

async function sendPollReminders(env, now) {
  const db = new Firestore(env.FIREBASE_PROJECT_ID, await serviceAccessToken(env.FIREBASE_SERVICE_ACCOUNT));
  for (const reminder of await pollReminders(db, now)) {
    if (!(await db.createOnce(`pushes/${reminder.key}`, { by: "cron", at: now }))) continue;
    const sent = await sendToUsers(db, env, reminder.recipients, reminder.payload);
    console.log(`push ${reminder.key}: ${sent}/${reminder.recipients.length} delivered`);
  }
}

/** How close to its deadline a poll gets its reminder, and how long it must have been open first. */
const REMINDER_WINDOW_MS = 24 * 60 * 60 * 1000;
const REMINDER_MIN_AGE_MS = 12 * 60 * 60 * 1000;

/**
 * One reminder per poll that closes within a day and has been open at least half a day, for the
 * members who haven't voted yet.
 */
export async function pollReminders(db, now) {
  const reminders = [];
  for (const { path: estatePath } of await db.query("estates")) {
    const estateId = estatePath.split("/").pop();
    const polls = await db.query("polls", [], { parent: estatePath });
    const due = polls.filter(({ data }) =>
      data.closed !== true
      && Number(data.closesAt) > now
      && Number(data.closesAt) - now <= REMINDER_WINDOW_MS
      && now - Number(data.createdAt || 0) >= REMINDER_MIN_AGE_MS
    );
    if (due.length === 0) continue;
    const members = await db.idsWhere("users", "estateId", estateId);
    for (const { path, data } of due) {
      const pollId = path.split("/").pop();
      const votes = await db.query("pollVotes", [["pollId", "EQUAL", pollId]], { parent: estatePath });
      const voted = new Set(votes.map((v) => v.data.userId));
      const recipients = members.filter((id) => !voted.has(id));
      if (recipients.length === 0) continue;
      reminders.push({
        key: `poll_reminder_${pollId}`,
        recipients,
        payload: {
          title: "🗳️ Last chance to vote",
          body: data.question || "A poll in your estate closes soon",
          deepLink: "localhive://polls",
          tag: `poll-${pollId}`,
        },
      });
    }
  }
  return reminders;
}

async function uploadSignature(env, uid, { kind }) {
  const folder = Object.hasOwn(UPLOAD_KINDS, kind) ? UPLOAD_KINDS[kind].folder : null;
  if (!folder) throw new HttpError(400, "Unknown upload kind");
  if (!env.CLOUDINARY_CLOUD_NAME || !env.CLOUDINARY_API_KEY || !env.CLOUDINARY_API_SECRET) {
    throw new Error("Cloudinary isn't configured");
  }
  return signUpload({
    cloudName: env.CLOUDINARY_CLOUD_NAME,
    apiKey: env.CLOUDINARY_API_KEY,
    apiSecret: env.CLOUDINARY_API_SECRET,
    // Per-user folder, so an upload can always be traced back to who made it.
    folder: `localhive/${folder}/${uid}`,
    limits: UPLOAD_KINDS[kind].limits,
  });
}

async function removeAccount(env, uid, authTime) {
  requireRecentLogin(authTime);
  const accessToken = await serviceAccessToken(env.FIREBASE_SERVICE_ACCOUNT);
  const db = new Firestore(env.FIREBASE_PROJECT_ID, accessToken);
  const result = await deleteAccount(db, uid, {
    deleteAuthUser: async (id) => {
      const response = await fetch(
        `https://identitytoolkit.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/accounts:delete`,
        {
          method: "POST",
          headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
          body: JSON.stringify({ localId: id }),
        }
      );
      if (!response.ok) throw new Error(`Auth delete: ${response.status} ${await response.text()}`);
    },
    deleteMedia: (id) => deleteCloudinaryFolders(env, id),
  });
  console.log(`account ${uid} deleted`);
  return result;
}

/** Every photo and video the user uploaded, which lives under their own folder for each upload kind. */
async function deleteCloudinaryFolders(env, uid) {
  if (!env.CLOUDINARY_API_KEY || !env.CLOUDINARY_API_SECRET) return;
  const auth = `Basic ${btoa(`${env.CLOUDINARY_API_KEY}:${env.CLOUDINARY_API_SECRET}`)}`;
  for (const { folder, resourceType } of Object.values(UPLOAD_KINDS)) {
    const prefix = encodeURIComponent(`localhive/${folder}/${uid}/`);
    const response = await fetch(
      `https://api.cloudinary.com/v1_1/${env.CLOUDINARY_CLOUD_NAME}/resources/${resourceType}/upload?prefix=${prefix}`,
      { method: "DELETE", headers: { Authorization: auth } }
    );
    if (!response.ok) throw new Error(`Cloudinary delete ${folder}: ${response.status} ${await response.text()}`);
  }
}

/** Removes post videos over the app's size limit, which a signed upload can't refuse up front. */
async function deleteOversizedVideos(env) {
  if (!env.CLOUDINARY_API_KEY || !env.CLOUDINARY_API_SECRET) return;
  const auth = `Basic ${btoa(`${env.CLOUDINARY_API_KEY}:${env.CLOUDINARY_API_SECRET}`)}`;
  const base = `https://api.cloudinary.com/v1_1/${env.CLOUDINARY_CLOUD_NAME}/resources/video/upload`;
  const prefix = encodeURIComponent(`localhive/${UPLOAD_KINDS.video.folder}/`);
  const tooBig = [];
  let cursor = "";
  do {
    const response = await fetch(`${base}?prefix=${prefix}&max_results=500${cursor && `&next_cursor=${cursor}`}`, {
      headers: { Authorization: auth },
    });
    if (!response.ok) throw new Error(`Cloudinary list videos: ${response.status} ${await response.text()}`);
    const page = await response.json();
    tooBig.push(...oversizedVideoIds(page.resources || []));
    cursor = page.next_cursor ? encodeURIComponent(page.next_cursor) : "";
  } while (cursor);
  // The delete call takes up to 100 ids at a time.
  for (let i = 0; i < tooBig.length; i += 100) {
    const ids = tooBig.slice(i, i + 100).map((id) => `public_ids[]=${encodeURIComponent(id)}`).join("&");
    const response = await fetch(`${base}?${ids}`, { method: "DELETE", headers: { Authorization: auth } });
    if (!response.ok) throw new Error(`Cloudinary delete videos: ${response.status} ${await response.text()}`);
  }
  if (tooBig.length > 0) console.log(`videos: deleted ${tooBig.length} over the size limit`);
}

async function notify(env, uid, body) {
  const db = new Firestore(env.FIREBASE_PROJECT_ID, await serviceAccessToken(env.FIREBASE_SERVICE_ACCOUNT));
  const push = await buildPush(db, uid, body);
  if (!push) return { sent: 0 };

  // Once-only: a retry, or someone replaying the request, can't send the same push again.
  if (!(await db.createOnce(`pushes/${push.key}`, { by: uid, at: Date.now() }))) return { sent: 0, duplicate: true };

  if (push.inbox) await leaveInInbox(db, push.key, push.recipients, push.inbox);
  const sent = await sendToUsers(db, env, push.recipients, push.payload);
  console.log(`push ${push.key}: ${sent}/${push.recipients.length} delivered`);
  return { sent };
}

/** One notification document per recipient, for notices the app can't raise itself. */
async function leaveInInbox(db, key, recipients, { type, title, message, estateId, senderId }) {
  const createdAt = Date.now();
  await Promise.all(recipients.map((recipientId) =>
    db.createOnce(`notifications/${key}_${recipientId}`, {
      recipientId, senderId, type, title, message, estateId, createdAt, isRead: false,
    })
  ));
}

/** Works out who to notify and with what, or returns null when there's nothing to send. */
export async function buildPush(db, uid, body) {
  switch (body.type) {
    case "message": {
      const { chatId, messageId } = body;
      if (!isId(chatId) || !isId(messageId)) throw new HttpError(400, "Bad message reference");
      const [chat, message] = await Promise.all([
        db.get(`chats/${chatId}`),
        db.get(`chats/${chatId}/messages/${messageId}`),
      ]);
      if (!chat || !message) throw new HttpError(404, "No such message");
      if (message.senderId !== uid || !(chat.participants || []).includes(uid)) {
        throw new HttpError(403, "Not your message");
      }
      return {
        key: `message_${messageId}`,
        recipients: (chat.participants || []).filter((id) => id !== uid),
        payload: {
          title: (chat.participantNames || {})[uid] || "A neighbour",
          body: message.content || "Sent you a message",
          deepLink: `localhive://chat/${chatId}`,
          // One tag per chat, so a burst of messages collapses into a single entry.
          tag: `chat-${chatId}`,
        },
      };
    }

    case "notification": {
      const { notificationId } = body;
      if (!isId(notificationId)) throw new HttpError(400, "Bad notification reference");
      const note = await db.get(`notifications/${notificationId}`);
      if (!note) throw new HttpError(404, "No such notification");
      if (note.senderId !== uid) throw new HttpError(403, "Not your notification");
      if (!(await notificationAllowed(db, uid, note))) throw new HttpError(403, "Not allowed");
      return {
        key: `notification_${notificationId}`,
        recipients: [note.recipientId],
        payload: {
          title: note.title || "LocalHive",
          body: note.message || "",
          deepLink: notificationLink(note),
          tag: `notification-${notificationId}`,
        },
      };
    }

    case "announcement": {
      const { postId } = body;
      if (!isId(postId)) throw new HttpError(400, "Bad post reference");
      const post = await db.get(`posts/${postId}`);
      if (!post || post.category !== "announcement") throw new HttpError(404, "No such announcement");
      if (post.authorId !== uid) throw new HttpError(403, "Not your announcement");
      const estate = await db.get(`estates/${post.estateId}`);
      if (!estate || !(estate.adminIds || []).includes(uid)) throw new HttpError(403, "Only admins can announce");
      const members = await db.idsWhere("users", "estateId", post.estateId);
      return {
        key: `announcement_${postId}`,
        recipients: members.filter((id) => id !== uid),
        payload: {
          title: `📣 ${estate.name || "Estate"} announcement`,
          body: post.content || (post.videoUrl ? "Shared a video" : "Tap to read"),
          deepLink: `localhive://post/${postId}`,
          tag: `announcement-${postId}`,
        },
      };
    }

    case "emergency": {
      const { post } = await incidentPost(db, uid, body.postId, "emergency");
      // Only a fresh SOS goes out, so an old one can't be dug up and sent again.
      if (!(Date.now() - Number(post.createdAt || 0) < EMERGENCY_MAX_AGE_MS)) throw new HttpError(410, "Too old to alert");
      if (await tooManyEmergencies(db, uid)) {
        throw new HttpError(429, "You've raised several emergencies in the last half hour. Call for help directly.");
      }
      const members = await db.idsWhere("users", "estateId", post.estateId);
      const type = EMERGENCY_TYPES[post.emergencyType];
      const where = post.location ? ` at ${post.location}` : "";
      return {
        key: `emergency_${body.postId}`,
        recipients: members.filter((id) => id !== uid),
        payload: {
          title: `🚨 ${type ? `${type} emergency` : "Emergency"}${where}`,
          body: `${post.authorName || "A neighbour"} needs help${post.content ? `: ${post.content}` : "."}`,
          deepLink: `localhive://post/${body.postId}`,
          tag: `emergency-${body.postId}`,
          channel: "emergency",
        },
      };
    }

    case "security_report": {
      const { post, estate } = await incidentPost(db, uid, body.postId, "security");
      const where = post.location ? ` (${post.location})` : "";
      return {
        key: `security_report_${body.postId}`,
        recipients: (estate.adminIds || []).filter((id) => id !== uid),
        payload: {
          title: `🛡️ Security report from ${post.authorName || "a neighbour"}`,
          body: `${post.content || "Tap to read"}${where}`,
          deepLink: `localhive://post/${body.postId}`,
          tag: `security-${body.postId}`,
        },
      };
    }

    case "poll": {
      const { estateId, pollId } = body;
      if (!isId(estateId) || !isId(pollId)) throw new HttpError(400, "Bad poll reference");
      const [estate, poll] = await Promise.all([db.get(`estates/${estateId}`), db.get(`estates/${estateId}/polls/${pollId}`)]);
      if (!estate || !poll) throw new HttpError(404, "No such poll");
      if (poll.createdBy !== uid || !(estate.adminIds || []).includes(uid)) throw new HttpError(403, "Only admins can start polls");
      const members = await db.idsWhere("users", "estateId", estateId);
      return {
        key: `poll_${pollId}`,
        recipients: members.filter((id) => id !== uid),
        payload: {
          title: `🗳️ New poll in ${estate.name || "your estate"}`,
          body: poll.question || "Tap to vote",
          deepLink: "localhive://polls",
          tag: `poll-${pollId}`,
        },
      };
    }

    case "light_back": {
      const { estateId } = body;
      if (!isId(estateId)) throw new HttpError(400, "Bad estate reference");
      const [estate, power, reporter] = await Promise.all([
        db.get(`estates/${estateId}`),
        db.get(`estates/${estateId}/utilities/power`),
        db.get(`users/${uid}`),
      ]);
      if (!estate || reporter?.estateId !== estateId) throw new HttpError(403, "Not in this estate");
      // Only a report that just turned the light on, by the caller.
      if (!power || power.isOn !== true || power.updatedBy !== uid || power.since !== power.updatedAt
          || !(Date.now() - Number(power.since) < LIGHT_BACK_MAX_AGE_MS)) {
        throw new HttpError(409, "The light didn't just come back");
      }
      const members = await db.idsWhere("users", "estateId", estateId);
      const profiles = await db.getAll(members.filter((id) => id !== uid).map((id) => `users/${id}`));
      return {
        // At most one an hour, however often the supply flickers.
        key: `light_back_${estateId}_${Math.floor(Number(power.since) / LIGHT_BACK_INTERVAL_MS)}`,
        recipients: Object.entries(profiles).filter(([, p]) => p.lightAlerts === true).map(([path]) => path.split("/")[1]),
        payload: {
          title: `💡 Light is back in ${estate.name || "your estate"}`,
          body: `${power.updatedByName || "A neighbour"} just reported it.`,
          deepLink: "localhive://utilities",
          tag: `light-${estateId}`,
        },
      };
    }

    case "sos_responding": {
      const { post } = await emergencyFor(db, body.postId);
      const name = (post.responders || {})[uid];
      if (!name || post.authorId === uid) throw new HttpError(403, "Not responding to this alert");
      const responder = await db.get(`users/${uid}`);
      if (responder?.estateId !== post.estateId) throw new HttpError(403, "Not in this estate");
      if (post.incidentStatus === "resolved") throw new HttpError(409, "Already resolved");
      return {
        key: `sos_responding_${body.postId}_${uid}`,
        recipients: [post.authorId],
        payload: {
          title: `🏃 ${name} is coming to help`,
          body: "They saw your emergency alert and are on their way.",
          deepLink: `localhive://post/${body.postId}`,
          tag: `emergency-${body.postId}`,
        },
      };
    }

    case "sos_safe": {
      const { post } = await emergencyFor(db, body.postId);
      if (post.authorId !== uid) throw new HttpError(403, "Not your alert");
      if (post.incidentStatus !== "resolved") throw new HttpError(409, "Not resolved");
      const members = await db.idsWhere("users", "estateId", post.estateId);
      const type = EMERGENCY_TYPES[post.emergencyType];
      return {
        key: `sos_safe_${body.postId}`,
        recipients: members.filter((id) => id !== uid),
        payload: {
          title: `✅ ${post.authorName || "Your neighbour"} is safe now`,
          body: `The ${type ? `${type.toLowerCase()} ` : ""}emergency${post.location ? ` at ${post.location}` : ""} is over.`,
          deepLink: `localhive://post/${body.postId}`,
          tag: `emergency-${body.postId}`,
        },
      };
    }

    case "levy_reminder": {
      const { estateId, levyId } = body;
      if (!isId(estateId) || !isId(levyId)) throw new HttpError(400, "Bad levy reference");
      const [estate, levy] = await Promise.all([db.get(`estates/${estateId}`), db.get(`estates/${estateId}/levies/${levyId}`)]);
      if (!estate || !levy) throw new HttpError(404, "No such levy");
      if (!estate.treasurerId || estate.treasurerId !== uid) throw new HttpError(403, "Only the treasurer can send reminders");
      const [members, payments] = await Promise.all([
        db.idsWhere("users", "estateId", estateId),
        db.query("payments", [], { parent: `estates/${estateId}/levies/${levyId}` }),
      ]);
      const paid = new Set(payments.filter((p) => p.data.status === "paid").map((p) => p.path.split("/").pop()));
      return {
        // Once a day per levy, so a nervous tap can't flood the estate.
        key: `levy_reminder_${levyId}_${new Date().toISOString().slice(0, 10)}`,
        recipients: members.filter((id) => id !== uid && !paid.has(id)),
        payload: {
          title: `Reminder: ${levy.title || "Estate levy"}`,
          body: `${naira(levy.amount)} is still due. Tap to pay or send proof of payment.`,
          deepLink: `localhive://levy/${levyId}`,
          tag: `levy-${levyId}`,
        },
      };
    }

    case "bank_changed": {
      const { estateId } = body;
      if (!isId(estateId)) throw new HttpError(400, "Bad estate reference");
      const [estate, bank] = await Promise.all([db.get(`estates/${estateId}`), db.get(`estates/${estateId}/private/bank`)]);
      if (!estate || !bank) throw new HttpError(404, "No bank details");
      if (!estate.treasurerId || estate.treasurerId !== uid || bank.updatedBy !== uid) {
        throw new HttpError(403, "Only the treasurer changes the bank details");
      }
      if (!(Date.now() - Number(bank.updatedAt || 0) < BANK_CHANGE_MAX_AGE_MS)) throw new HttpError(410, "Not a fresh change");
      const members = await db.idsWhere("users", "estateId", estateId);
      const ending = String(bank.accountNumber || "").slice(-4);
      const title = `🏦 ${estate.name || "Your estate"}'s bank details changed`;
      const message = `${bank.updatedByName || "The treasurer"} set dues to be paid to ${bank.bankName || "a new account"}` +
        `${ending ? ` (account ending ${ending})` : ""}. If you weren't expecting this, check with your estate before paying.`;
      return {
        key: `bank_changed_${estateId}_${bank.updatedAt}`,
        recipients: members.filter((id) => id !== uid),
        payload: { title, body: message, deepLink: "localhive://notifications", tag: `bank-${estateId}` },
        // Also left in everyone's notifications, so it's still there after the push is swiped away.
        inbox: { type: "BANK_DETAILS_CHANGED", title, message, estateId, senderId: uid },
      };
    }

    default:
      throw new HttpError(400, "Unknown notify type");
  }
}

/** A bank change older than this is too stale to announce. */
const BANK_CHANGE_MAX_AGE_MS = 60 * 60 * 1000;

/**
 * How many SOS alerts one person can raise in [EMERGENCY_WINDOW_MS]. Generous enough for a real
 * emergency that needs a follow-up, but stops anyone setting off the estate's sirens on repeat.
 */
const EMERGENCY_LIMIT = 3;
const EMERGENCY_WINDOW_MS = 30 * 60 * 1000;

/** True once the caller's recent emergencies, counting the one being sent, go over the limit. */
export async function tooManyEmergencies(db, uid, now = Date.now()) {
  const mine = await db.query("posts", [["authorId", "EQUAL", uid], ["category", "EQUAL", "emergency"]]);
  // The app stamps createdAt; a future one still counts, so it can't be used to dodge the limit.
  const recent = mine.filter(({ data }) => Number(data.createdAt || 0) > now - EMERGENCY_WINDOW_MS);
  return recent.length > EMERGENCY_LIMIT;
}

/** A "light is back" report older than this is too stale to push. */
const LIGHT_BACK_MAX_AGE_MS = 10 * 60 * 1000;
const LIGHT_BACK_INTERVAL_MS = 60 * 60 * 1000;

async function emergencyFor(db, postId) {
  if (!isId(postId)) throw new HttpError(400, "Bad post reference");
  const post = await db.get(`posts/${postId}`);
  if (!post || post.category !== "emergency") throw new HttpError(404, "No such emergency");
  return { post };
}

/** How long after an SOS is raised it can still be pushed. */
const EMERGENCY_MAX_AGE_MS = 15 * 60 * 1000;

const EMERGENCY_TYPES = { break_in: "Break-in", fire: "Fire", medical: "Medical" };

/**
 * A security report or emergency the caller raised in the estate they still belong to, with
 * that estate.
 */
async function incidentPost(db, uid, postId, category) {
  if (!isId(postId)) throw new HttpError(400, "Bad post reference");
  const post = await db.get(`posts/${postId}`);
  if (!post || post.category !== category) throw new HttpError(404, "No such post");
  if (post.authorId !== uid) throw new HttpError(403, "Not your post");
  const [author, estate] = await Promise.all([db.get(`users/${uid}`), db.get(`estates/${post.estateId}`)]);
  if (!estate || author?.estateId !== post.estateId) throw new HttpError(403, "Not in this estate");
  return { post, estate };
}

/**
 * The app raises notifications for join-request traffic (requesters notify that estate's admins,
 * and admins tell requesters the outcome), when an admin removes a member or one of their posts,
 * when someone tags a neighbour in a post or comment, and for levy payments. Anything else is refused.
 */
async function notificationAllowed(db, uid, note) {
  if (!note.estateId || !note.recipientId) return false;
  const estate = await db.get(`estates/${note.estateId}`);
  if (!estate) return false;
  const admins = estate.adminIds || [];
  if (note.type === "JOIN_REQUEST") return admins.includes(note.recipientId);
  if (note.type === "REQUEST_APPROVED" || note.type === "REQUEST_DENIED") return admins.includes(uid);
  // Only once the removal has actually happened.
  if (note.type === "MEMBER_REMOVED") {
    if (!admins.includes(uid)) return false;
    const member = await db.get(`users/${note.recipientId}`);
    return !!member && member.estateId !== note.estateId;
  }
  if (note.type === "MENTION") return mentionAllowed(db, uid, note);
  // An admin telling a member of their estate that they removed one of the member's posts.
  if (note.type === "POST_REMOVED") {
    if (!admins.includes(uid) || note.recipientId === uid) return false;
    const member = await db.get(`users/${note.recipientId}`);
    return member?.estateId === note.estateId;
  }
  if (String(note.type || "").startsWith("PAYMENT_")) return paymentNoticeAllowed(db, uid, note, estate);
  return false;
}

/** Where tapping a notification's push lands. */
function notificationLink(note) {
  if (note.type === "MENTION" && isId(note.postId)) return `localhive://post/${note.postId}`;
  if (String(note.type || "").startsWith("PAYMENT_") && isId(note.levyId)) return `localhive://levy/${note.levyId}`;
  return "localhive://notifications";
}

/**
 * Payment notices only go between a resident and the treasurer, about a payment that really is
 * in the state the notice describes.
 */
async function paymentNoticeAllowed(db, uid, note, estate) {
  if (!isId(note.levyId) || !estate.treasurerId) return false;
  const payer = note.type === "PAYMENT_SUBMITTED" ? uid : note.recipientId;
  if (!isId(payer)) return false;
  const payment = await db.get(`estates/${note.estateId}/levies/${note.levyId}/payments/${payer}`);
  if (!payment) return false;
  switch (note.type) {
    case "PAYMENT_SUBMITTED":
      return note.recipientId === estate.treasurerId && payment.status === "pending";
    case "PAYMENT_CONFIRMED":
      return uid === estate.treasurerId && payment.status === "paid";
    case "PAYMENT_REJECTED":
      return uid === estate.treasurerId && payment.status === "rejected";
    default:
      return false;
  }
}

/** Whole naira with thousands separators, as the app shows it. */
function naira(amount) {
  return `₦${Number(amount || 0).toLocaleString("en-NG", { maximumFractionDigits: 0 })}`;
}

/**
 * A tag is only pushed when the sender really wrote that post or comment, really tagged the
 * recipient in it, and both are in the post's estate, and only if the recipient hasn't blocked them.
 */
async function mentionAllowed(db, uid, note) {
  if (!isId(note.postId) || note.recipientId === uid) return false;
  if (note.commentId != null && !isId(note.commentId)) return false;
  const [post, sender, recipient, blocked] = await Promise.all([
    db.get(`posts/${note.postId}`),
    db.get(`users/${uid}`),
    db.get(`users/${note.recipientId}`),
    db.get(`users/${note.recipientId}/blocks/${uid}`),
  ]);
  if (!post || post.estateId !== note.estateId || blocked) return false;
  if (sender?.estateId !== note.estateId || recipient?.estateId !== note.estateId) return false;
  const source = note.commentId ? await db.get(`posts/${note.postId}/comments/${note.commentId}`) : post;
  return !!source && source.authorId === uid && Object.hasOwn(source.mentions || {}, note.recipientId);
}

/** Firestore ids from the app: no slashes, so a reference can't climb into another path. */
function isId(value) {
  return typeof value === "string" && /^[A-Za-z0-9_-]{1,200}$/.test(value);
}

function json(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: { "Content-Type": "application/json" } });
}
