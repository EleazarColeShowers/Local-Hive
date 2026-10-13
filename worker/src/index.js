import { HttpError, serviceAccessToken, verifyFirebaseUser } from "./google.js";
import { Firestore } from "./firestore.js";
import { signUpload, UPLOAD_KINDS } from "./cloudinary.js";
import { sendToUsers } from "./push.js";
import { deleteAccount, requireRecentLogin } from "./account.js";

/**
 * LocalHive's backend on Cloudflare's free tier, doing what Cloud Functions would on Firebase's
 * paid plan:
 *
 *   POST /upload-signature  { kind: "post" | "profile" }            -> Cloudinary signed-upload params
 *   POST /notify            { type: "message", chatId, messageId }
 *                           { type: "notification", notificationId }
 *                           { type: "announcement", postId }        -> sends the push
 *   POST /delete-account    {}                                       -> deletes the caller's account
 *
 * Every request needs a Firebase ID token. /notify never trusts the request body for content:
 * it reads the message, notification or post itself, checks the caller is its sender, and
 * sends each push at most once.
 */
export default {
  async fetch(request, env) {
    if (request.method !== "POST") return json({ error: "Not found" }, 404);
    try {
      const { uid, authTime } = await verifyFirebaseUser(request, env.FIREBASE_PROJECT_ID);
      const body = await request.json().catch(() => ({}));
      const path = new URL(request.url).pathname;

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
};

async function uploadSignature(env, uid, { kind }) {
  const folder = UPLOAD_KINDS[kind];
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

/** Every photo the user uploaded, which lives under their own folder for each upload kind. */
async function deleteCloudinaryFolders(env, uid) {
  if (!env.CLOUDINARY_API_KEY || !env.CLOUDINARY_API_SECRET) return;
  const auth = `Basic ${btoa(`${env.CLOUDINARY_API_KEY}:${env.CLOUDINARY_API_SECRET}`)}`;
  for (const folder of Object.values(UPLOAD_KINDS)) {
    const prefix = encodeURIComponent(`localhive/${folder}/${uid}/`);
    const response = await fetch(
      `https://api.cloudinary.com/v1_1/${env.CLOUDINARY_CLOUD_NAME}/resources/image/upload?prefix=${prefix}`,
      { method: "DELETE", headers: { Authorization: auth } }
    );
    if (!response.ok) throw new Error(`Cloudinary delete ${folder}: ${response.status} ${await response.text()}`);
  }
}

async function notify(env, uid, body) {
  const db = new Firestore(env.FIREBASE_PROJECT_ID, await serviceAccessToken(env.FIREBASE_SERVICE_ACCOUNT));
  const push = await buildPush(db, uid, body);
  if (!push) return { sent: 0 };

  // Once-only: a retry, or someone replaying the request, can't send the same push again.
  if (!(await db.createOnce(`pushes/${push.key}`, { by: uid, at: Date.now() }))) return { sent: 0, duplicate: true };

  const sent = await sendToUsers(db, env, push.recipients, push.payload);
  console.log(`push ${push.key}: ${sent}/${push.recipients.length} delivered`);
  return { sent };
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
          deepLink: "localhive://notifications",
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
          body: post.content || "Tap to read",
          deepLink: `localhive://post/${postId}`,
          tag: `announcement-${postId}`,
        },
      };
    }

    default:
      throw new HttpError(400, "Unknown notify type");
  }
}

/**
 * The app raises notifications for join-request traffic (requesters notify that estate's admins,
 * and admins tell requesters the outcome) and when an admin removes a member. Anything else is refused.
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
  return false;
}

/** Firestore ids from the app: no slashes, so a reference can't climb into another path. */
function isId(value) {
  return typeof value === "string" && /^[A-Za-z0-9_-]{1,200}$/.test(value);
}

function json(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: { "Content-Type": "application/json" } });
}
