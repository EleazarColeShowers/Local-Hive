import { serviceAccessToken } from "./google.js";

/** FCM reports these for tokens that will never work again; we drop them from the profile. */
/** The siren bundled with the iOS app; it must stay under 30 seconds for iOS to play it. */
const EMERGENCY_SOUND = "sos_alarm.wav";

const DEAD_TOKEN_ERRORS = new Set(["UNREGISTERED", "INVALID_ARGUMENT"]);

/** How loud an SOS can be, quietest first. */
const SOS_LEVELS = ["vibrate", "sound", "siren"];

/**
 * How loud an SOS is for one person: the sound they chose in Settings, only vibrating while they've
 * paused it, and never louder than [maxSound], which drops to "sound" for a sender whose false
 * alarms cost them the siren.
 */
export function sosSoundFor(profile, maxSound = "siren", now = Date.now()) {
  const chosen = SOS_LEVELS.includes(profile?.sosSound) ? profile.sosSound : "siren";
  const wanted = Number(profile?.sosPausedUntil) > now ? "vibrate" : chosen;
  const cap = SOS_LEVELS.includes(maxSound) ? maxSound : "siren";
  return SOS_LEVELS[Math.min(SOS_LEVELS.indexOf(wanted), SOS_LEVELS.indexOf(cap))];
}

/** The iOS sound for each level; vibrate-only has none, so the alert arrives silently. */
const IOS_SOS_SOUNDS = { siren: EMERGENCY_SOUND, sound: "default" };

/**
 * Pushes to this many people or fewer (a message, a personal notification) carry the app icon's
 * badge number for iOS. Working it out costs two queries per person, too many for an estate-wide
 * announcement, which leaves the badge as it was.
 */
const BADGE_MAX_RECIPIENTS = 3;

/**
 * Sends a push to each user with a registered device. Android gets it data-only, so the app always
 * runs its own onMessageReceived and can honour the in-app "push notifications" toggle — a payload
 * with a `notification` block would be drawn by the system tray instead, bypassing it. iOS only
 * shows a push that carries an alert, so Apple devices get one; there the app honours the toggle
 * by taking its token off the profile instead.
 *
 * An SOS (`channel: "emergency"`) is as loud as [sosSoundFor] says for each person: Android gets
 * the level as `sosSound` and picks the matching channel, and iOS gets the siren bundled in the
 * app, the default sound or none. It's marked time-sensitive so it still shows through Focus.
 */
export async function sendToUsers(db, env, userIds, { title, body, deepLink, tag, channel, maxSound }) {
  const recipients = [...new Set(userIds)].filter(Boolean);
  if (recipients.length === 0) return 0;

  // Profiles for each person's sound settings, and their tokens, in one read.
  const docs = await db.getAll(recipients.flatMap((id) => [`users/${id}`, `pushTokens/${id}`]));
  const people = recipients
    .map((id) => ({ id, profile: docs[`users/${id}`], tokens: pushTokensOf(docs, id) }))
    .filter((p) => p.tokens.length > 0);
  if (people.length === 0) return 0;

  const badges = people.length <= BADGE_MAX_RECIPIENTS
    ? Object.fromEntries(await Promise.all(people.map(async ({ id }) => [id, await unreadCount(db, id).catch(() => null)])))
    : {};
  // One send per device: someone with a phone and a tablet gets it on both.
  const targets = people.flatMap(({ id, profile, tokens }) => tokens.map((token) => ({ id, profile, token })));

  const accessToken = await serviceAccessToken(env.FIREBASE_SERVICE_ACCOUNT);
  const url = `https://fcm.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/messages:send`;

  const results = await Promise.all(
    targets.map(async ({ id, profile, token }) => {
      const sos = channel === "emergency" ? sosSoundFor(profile, maxSound) : null;
      const sound = sos ? IOS_SOS_SOUNDS[sos] : "default";
      const response = await fetch(url, {
        method: "POST",
        headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
        body: JSON.stringify({
          message: {
            token,
            // `channel` lets Android give emergencies their own, louder notification channel.
            data: { title, body: truncate(body, 300), deepLink, tag, ...(channel ? { channel } : {}), ...(sos ? { sosSound: sos } : {}) },
            android: { priority: "HIGH" },
            apns: {
              headers: channel === "emergency" ? { "apns-priority": "10", "apns-push-type": "alert" } : undefined,
              payload: {
                aps: {
                  alert: { title, body: truncate(body, 300) },
                  // With Apple's critical-alerts approval, the siren could play on silent instead:
                  // sound: { critical: 1, name: EMERGENCY_SOUND, volume: 1.0 }
                  ...(sound ? { sound } : {}),
                  "thread-id": tag,
                  ...(Number.isInteger(badges[id]) ? { badge: badges[id] } : {}),
                  ...(channel === "emergency" ? { "interruption-level": "time-sensitive" } : {}),
                },
              },
            },
          },
        }),
      });
      if (response.ok) return true;

      const error = await response.json().catch(() => ({}));
      const code = error?.error?.details?.find((d) => d.errorCode)?.errorCode || error?.error?.status;
      console.log("FCM send failed", id, response.status, code, error?.error?.message);
      if (DEAD_TOKEN_ERRORS.has(code)) await forgetToken(db, docs, id, token);
      return false;
    })
  );
  // How many people it reached on at least one device.
  return new Set(targets.filter((_, i) => results[i]).map((t) => t.id)).size;
}

/** At most this many devices get each push, newest first (the rules allow no more in `tokens`). */
export const MAX_DEVICES_PER_PERSON = 10;

/**
 * The user's device tokens. From 1.09 each device keeps its own entry in the `tokens` map on
 * pushTokens/{uid}, which only they can read. Apps before that keep it on the profile, where
 * neighbours can see it, and test builds kept a single `token` on pushTokens; those are used
 * too until everyone has updated.
 */
export function pushTokensOf(docs, id) {
  const doc = docs[`pushTokens/${id}`];
  const devices = Object.entries(doc?.tokens || {})
    .filter(([token]) => token)
    .sort(([, a], [, b]) => Number(b) - Number(a))
    .map(([token]) => token);
  const legacy = [doc?.token, docs[`users/${id}`]?.fcmToken].filter((t) => typeof t === "string" && t);
  return [...new Set([...devices, ...legacy])].slice(0, MAX_DEVICES_PER_PERSON);
}

/** Drops a token FCM says is dead from wherever it's kept, leaving the person's other devices. */
async function forgetToken(db, docs, id, token) {
  const doc = docs[`pushTokens/${id}`];
  if (doc?.tokens && token in doc.tokens) await db.deleteField(`pushTokens/${id}`, `tokens.${token}`).catch(() => {});
  if (doc?.token === token) await db.deleteField(`pushTokens/${id}`, "token").catch(() => {});
  if (docs[`users/${id}`]?.fcmToken === token) await db.deleteField(`users/${id}`, "fcmToken").catch(() => {});
}

/**
 * What the app shows as unread: messages across the user's chats plus unread notifications. The
 * same number the app puts on its icon while it's open.
 */
async function unreadCount(db, userId) {
  const [chats, notes] = await Promise.all([
    db.query("chats", [["participants", "ARRAY_CONTAINS", userId]]),
    db.query("notifications", [["recipientId", "EQUAL", userId], ["isRead", "EQUAL", false]]),
  ]);
  const messages = chats.reduce((sum, chat) => sum + (Number(chat.data.unreadCounts?.[userId]) || 0), 0);
  return messages + notes.length;
}

function truncate(text, max) {
  return text.length > max ? `${text.slice(0, max - 1)}…` : text;
}
