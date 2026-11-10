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

  const profiles = await db.getAll(recipients.map((id) => `users/${id}`));
  const targets = recipients
    .map((id) => ({ id, profile: profiles[`users/${id}`], token: profiles[`users/${id}`]?.fcmToken }))
    .filter((t) => t.token);
  if (targets.length === 0) return 0;

  const badges = targets.length <= BADGE_MAX_RECIPIENTS
    ? Object.fromEntries(await Promise.all(targets.map(async ({ id }) => [id, await unreadCount(db, id).catch(() => null)])))
    : {};

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
      if (DEAD_TOKEN_ERRORS.has(code)) await db.deleteField(`users/${id}`, "fcmToken");
      return false;
    })
  );
  return results.filter(Boolean).length;
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
