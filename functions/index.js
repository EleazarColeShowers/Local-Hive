const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

setGlobalOptions({ region: "us-central1", maxInstances: 10 });

/**
 * Sends a data-only push so the app always runs its own onMessageReceived and can
 * honour the in-app "push notifications" toggle. A payload carrying a `notification`
 * block would be drawn by the system tray while the app is backgrounded, bypassing it.
 */
async function sendToUsers(userIds, { title, body, deepLink, tag }) {
  const recipients = [...new Set(userIds)].filter(Boolean);
  if (recipients.length === 0) return;

  const snapshots = await db.getAll(
    ...recipients.map((id) => db.collection("users").doc(id))
  );

  const targets = snapshots
    .filter((doc) => doc.exists && doc.get("fcmToken"))
    .map((doc) => ({ userId: doc.id, token: doc.get("fcmToken") }));

  if (targets.length === 0) {
    logger.info("No registered devices for recipients", { recipients });
    return;
  }

  const response = await messaging.sendEach(
    targets.map(({ token }) => ({
      token,
      data: { title, body, deepLink, tag },
      android: { priority: "high" },
    }))
  );

  await clearStaleTokens(targets, response);
}

/** Drops tokens Firebase reports as dead so we stop paying to retry them. */
async function clearStaleTokens(targets, response) {
  const dead = ["messaging/registration-token-not-registered", "messaging/invalid-argument"];

  const stale = response.responses
    .map((result, i) => ({ result, target: targets[i] }))
    .filter(({ result }) => !result.success && dead.includes(result.error?.code));

  if (stale.length === 0) return;

  await Promise.all(
    stale.map(({ target }) =>
      db.collection("users").doc(target.userId).update({
        fcmToken: admin.firestore.FieldValue.delete(),
      })
    )
  );
  logger.info(`Cleared ${stale.length} stale FCM token(s)`);
}

/** Join requests, approvals and denials are written to /notifications by the app. */
exports.onNotificationCreated = onDocumentCreated(
  "notifications/{notificationId}",
  async (event) => {
    const notification = event.data?.data();
    if (!notification?.recipientId) return;

    await sendToUsers([notification.recipientId], {
      title: notification.title || "LocalHive",
      body: notification.message || "",
      deepLink: "localhive://notifications",
      tag: `notification-${event.params.notificationId}`,
    });
  }
);

/** A direct message notifies every participant in the chat except the sender. */
exports.onMessageCreated = onDocumentCreated(
  "chats/{chatId}/messages/{messageId}",
  async (event) => {
    const message = event.data?.data();
    if (!message?.senderId) return;

    const chatId = event.params.chatId;
    const chat = await db.collection("chats").doc(chatId).get();
    if (!chat.exists) return;

    const participants = chat.get("participants") || [];
    const recipients = participants.filter((id) => id !== message.senderId);
    const senderName = (chat.get("participantNames") || {})[message.senderId] || "A neighbour";

    await sendToUsers(recipients, {
      title: senderName,
      body: message.content || "Sent you a message",
      deepLink: `localhive://chat/${chatId}`,
      // One tag per chat, so a burst of messages collapses into a single entry.
      tag: `chat-${chatId}`,
    });
  }
);
