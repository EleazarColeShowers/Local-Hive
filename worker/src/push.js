import { serviceAccessToken } from "./google.js";

/** FCM reports these for tokens that will never work again; we drop them from the profile. */
/** The siren bundled with the iOS app; it must stay under 30 seconds for iOS to play it. */
const EMERGENCY_SOUND = "sos_alarm.wav";

const DEAD_TOKEN_ERRORS = new Set(["UNREGISTERED", "INVALID_ARGUMENT"]);

/**
 * Sends a push to each user with a registered device. Android gets it data-only, so the app always
 * runs its own onMessageReceived and can honour the in-app "push notifications" toggle — a payload
 * with a `notification` block would be drawn by the system tray instead, bypassing it. iOS only
 * shows a push that carries an alert, so Apple devices get one; there the app honours the toggle
 * by taking its token off the profile instead.
 *
 * An SOS (`channel: "emergency"`) sounds the siren bundled in both apps: Android picks it by
 * channel, and iOS plays the named sound, marked time-sensitive so it breaks through Focus.
 */
export async function sendToUsers(db, env, userIds, { title, body, deepLink, tag, channel }) {
  const recipients = [...new Set(userIds)].filter(Boolean);
  if (recipients.length === 0) return 0;

  const profiles = await db.getAll(recipients.map((id) => `users/${id}`));
  const targets = recipients
    .map((id) => ({ id, token: profiles[`users/${id}`]?.fcmToken }))
    .filter((t) => t.token);
  if (targets.length === 0) return 0;

  const accessToken = await serviceAccessToken(env.FIREBASE_SERVICE_ACCOUNT);
  const url = `https://fcm.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/messages:send`;

  const results = await Promise.all(
    targets.map(async ({ id, token }) => {
      const response = await fetch(url, {
        method: "POST",
        headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
        body: JSON.stringify({
          message: {
            token,
            // `channel` lets Android give emergencies their own, louder notification channel.
            data: { title, body: truncate(body, 300), deepLink, tag, ...(channel ? { channel } : {}) },
            android: { priority: "HIGH" },
            apns: {
              headers: channel === "emergency" ? { "apns-priority": "10", "apns-push-type": "alert" } : undefined,
              payload: {
                aps: {
                  alert: { title, body: truncate(body, 300) },
                  // With Apple's critical-alerts approval, an SOS could play on silent instead:
                  // sound: channel === "emergency" ? { critical: 1, name: EMERGENCY_SOUND, volume: 1.0 } : "default",
                  sound: channel === "emergency" ? EMERGENCY_SOUND : "default",
                  "thread-id": tag,
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

function truncate(text, max) {
  return text.length > max ? `${text.slice(0, max - 1)}…` : text;
}
