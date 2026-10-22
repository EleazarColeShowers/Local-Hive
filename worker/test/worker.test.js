import { test } from "node:test";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { buildPush, estateNameLowerUpdates, pollReminders, tooManyEmergencies } from "../src/index.js";
import { MAX_VIDEO_BYTES, oversizedVideoIds, signUpload } from "../src/cloudinary.js";
import { decodeValue } from "../src/firestore.js";
import { appIntentUrl, inviteCodeIn, playStoreUrl, publicResponse } from "../src/links.js";

/** In-memory stand-in for the Firestore REST client. */
function fakeDb(docs) {
  return {
    get: async (path) => docs[path] ?? null,
    idsWhere: async (collection, field, value) =>
      Object.entries(docs)
        .filter(([p, d]) => p.startsWith(`${collection}/`) && p.split("/").length === 2 && d[field] === value)
        .map(([p]) => p.split("/")[1]),
    getAll: async (paths) => Object.fromEntries(paths.filter((p) => docs[p]).map((p) => [p, docs[p]])),
    // Only what the worker uses: every document directly under `parent/collection`, with EQUAL filters.
    query: async (collection, where = [], { parent = "" } = {}) => {
      const prefix = parent ? `${parent}/${collection}/` : `${collection}/`;
      const depth = prefix.split("/").length;
      return Object.entries(docs)
        .filter(([p]) => p.startsWith(prefix) && p.split("/").length === depth)
        .filter(([, d]) => where.every(([field, op, value]) => op === "EQUAL" && d[field] === value))
        .map(([path, data]) => ({ path, data }));
    },
  };
}

const docs = {
  "estates/E1": { name: "Amali Estate", adminIds: ["admin"] },
  "users/admin": { estateId: "E1" },
  "users/member": { estateId: "E1" },
  "users/other": { estateId: "E2" },
  "chats/admin_member": { participants: ["admin", "member"], participantNames: { admin: "Eleazar" } },
  "chats/admin_member/messages/m1": { senderId: "admin", content: "Hello neighbour" },
  "notifications/n1": { senderId: "member", recipientId: "admin", type: "JOIN_REQUEST", estateId: "E1", title: "New join request", message: "x" },
  "notifications/n2": { senderId: "member", recipientId: "other", type: "JOIN_REQUEST", estateId: "E1", title: "t", message: "m" },
  "notifications/n3": { senderId: "member", recipientId: "other", type: "REQUEST_APPROVED", estateId: "E1" },
  "posts/a1": { authorId: "admin", estateId: "E1", category: "announcement", content: "Meeting Saturday" },
  "posts/p1": { authorId: "member", estateId: "E1", category: "question", content: "?" },
  "posts/a2": { authorId: "member", estateId: "E1", category: "announcement", content: "fake" },
  "users/third": { estateId: "E1" },
  "users/blocker": { estateId: "E1" },
  "users/blocker/blocks/member": { name: "Member" },
  "posts/t1": { authorId: "member", estateId: "E1", content: "@Ada look", mentions: { admin: "Ada", blocker: "Bo" } },
  "posts/t1/comments/c1": { authorId: "admin", content: "@Member thanks", mentions: { member: "Member" } },
  "notifications/m1": { senderId: "member", recipientId: "admin", type: "MENTION", estateId: "E1", postId: "t1", title: "Tagged", message: "@Ada look" },
  "notifications/m2": { senderId: "member", recipientId: "third", type: "MENTION", estateId: "E1", postId: "t1" },
  "notifications/m3": { senderId: "admin", recipientId: "member", type: "MENTION", estateId: "E1", postId: "t1", commentId: "c1" },
  "notifications/m4": { senderId: "admin", recipientId: "member", type: "MENTION", estateId: "E1", postId: "t1" },
  "notifications/m5": { senderId: "member", recipientId: "blocker", type: "MENTION", estateId: "E1", postId: "t1" },
  "notifications/m6": { senderId: "member", recipientId: "admin", type: "MENTION", estateId: "E1", postId: "../t1" },
  // Amali Estate's treasurer is "third"; "member" has sent proof, "blocker" has paid.
  "estates/E1/levies/L1": { title: "Security levy", amount: 5000 },
  "estates/E1/levies/L1/payments/member": { userId: "member", status: "pending" },
  "estates/E1/levies/L1/payments/blocker": { userId: "blocker", status: "paid" },
  "notifications/p1": { senderId: "member", recipientId: "third", type: "PAYMENT_SUBMITTED", estateId: "E1", levyId: "L1" },
  "notifications/p2": { senderId: "member", recipientId: "admin", type: "PAYMENT_SUBMITTED", estateId: "E1", levyId: "L1" },
  "notifications/p3": { senderId: "third", recipientId: "blocker", type: "PAYMENT_CONFIRMED", estateId: "E1", levyId: "L1" },
  "notifications/p4": { senderId: "third", recipientId: "member", type: "PAYMENT_CONFIRMED", estateId: "E1", levyId: "L1" },
  "notifications/p5": { senderId: "admin", recipientId: "blocker", type: "PAYMENT_CONFIRMED", estateId: "E1", levyId: "L1" },
};
docs["estates/E1"] = { ...docs["estates/E1"], treasurerId: "third" };
// Security reports and emergencies; "other" lives in E2, so can't raise one in E1.
Object.assign(docs, {
  "posts/sos1": { authorId: "member", authorName: "Ada", estateId: "E1", category: "emergency", emergencyType: "fire", location: "Block C", content: "Kitchen fire", createdAt: Date.now() },
  "posts/sos2": { authorId: "member", estateId: "E1", category: "emergency", createdAt: Date.now() - 60 * 60 * 1000 },
  "posts/sos3": { authorId: "other", estateId: "E1", category: "emergency", createdAt: Date.now() },
  "estates/E1/polls/poll1": { question: "Raise the levy?", createdBy: "admin", options: ["Yes", "No"] },
  "estates/E1/polls/poll2": { question: "Sneaky", createdBy: "member", options: ["Yes", "No"] },
  "posts/sos4": { authorId: "member", authorName: "Ada", estateId: "E1", category: "emergency", emergencyType: "fire", location: "Block C", responders: { third: "Tunde" }, incidentStatus: "open" },
  "posts/sos5": { authorId: "member", authorName: "Ada", estateId: "E1", category: "emergency", emergencyType: "fire", location: "Block C", incidentStatus: "resolved" },
  "notifications/r1": { senderId: "admin", recipientId: "member", type: "POST_REMOVED", estateId: "E1", title: "An admin removed your post", message: "Reason: spam" },
  "notifications/r2": { senderId: "member", recipientId: "third", type: "POST_REMOVED", estateId: "E1" },
  "notifications/r3": { senderId: "admin", recipientId: "other", type: "POST_REMOVED", estateId: "E1" },
  "posts/sec1": { authorId: "member", authorName: "Ada", estateId: "E1", category: "security", location: "Back fence", content: "Someone climbing in" },
});
const db = fakeDb(docs);
const status = async (promise) => promise.then(() => 200, (e) => e.status ?? 500);

test("message push goes to the other participant, with the real content", async () => {
  const push = await buildPush(db, "admin", { type: "message", chatId: "admin_member", messageId: "m1" });
  assert.deepEqual(push.recipients, ["member"]);
  assert.equal(push.payload.title, "Eleazar");
  assert.equal(push.payload.body, "Hello neighbour");
  assert.equal(push.key, "message_m1");
});

test("you can't trigger a push for someone else's message", async () => {
  assert.equal(await status(buildPush(db, "member", { type: "message", chatId: "admin_member", messageId: "m1" })), 403);
});

test("path tricks in ids are rejected", async () => {
  assert.equal(await status(buildPush(db, "admin", { type: "message", chatId: "../users", messageId: "m1" })), 400);
});

test("join request alerts only reach that estate's admins", async () => {
  const ok = await buildPush(db, "member", { type: "notification", notificationId: "n1" });
  assert.deepEqual(ok.recipients, ["admin"]);
  assert.equal(await status(buildPush(db, "member", { type: "notification", notificationId: "n2" })), 403);
});

test("only admins can send approval/denial alerts", async () => {
  assert.equal(await status(buildPush(db, "member", { type: "notification", notificationId: "n3" })), 403);
});

test("notification pushes must come from their sender", async () => {
  assert.equal(await status(buildPush(db, "admin", { type: "notification", notificationId: "n1" })), 403);
});

test("a tag in a post pushes to the tagged member and opens the post", async () => {
  const push = await buildPush(db, "member", { type: "notification", notificationId: "m1" });
  assert.deepEqual(push.recipients, ["admin"]);
  assert.equal(push.payload.deepLink, "localhive://post/t1");
});

test("a tag in a comment checks the comment, not the post", async () => {
  assert.deepEqual((await buildPush(db, "admin", { type: "notification", notificationId: "m3" })).recipients, ["member"]);
  // Without the comment, admin never tagged member in the post itself.
  assert.equal(await status(buildPush(db, "admin", { type: "notification", notificationId: "m4" })), 403);
});

test("tags can't be faked or reach someone who blocked the sender", async () => {
  assert.equal(await status(buildPush(db, "member", { type: "notification", notificationId: "m2" })), 403);
  assert.equal(await status(buildPush(db, "member", { type: "notification", notificationId: "m5" })), 403);
  assert.equal(await status(buildPush(db, "member", { type: "notification", notificationId: "m6" })), 403);
});

test("proof of payment reaches the treasurer and opens the levy", async () => {
  const push = await buildPush(db, "member", { type: "notification", notificationId: "p1" });
  assert.deepEqual(push.recipients, ["third"]);
  assert.equal(push.payload.deepLink, "localhive://levy/L1");
  // Only the treasurer can be sent proof.
  assert.equal(await status(buildPush(db, "member", { type: "notification", notificationId: "p2" })), 403);
});

test("only the treasurer confirms payments, and only real ones", async () => {
  assert.deepEqual((await buildPush(db, "third", { type: "notification", notificationId: "p3" })).recipients, ["blocker"]);
  // member's payment is still pending, so it can't be announced as confirmed.
  assert.equal(await status(buildPush(db, "third", { type: "notification", notificationId: "p4" })), 403);
  // An admin isn't the treasurer.
  assert.equal(await status(buildPush(db, "admin", { type: "notification", notificationId: "p5" })), 403);
});

test("levy reminders go to members who haven't paid, from the treasurer only", async () => {
  const push = await buildPush(db, "third", { type: "levy_reminder", estateId: "E1", levyId: "L1" });
  assert.deepEqual(push.recipients.sort(), ["admin", "member"]);
  assert.match(push.payload.body, /₦5,000/);
  assert.match(push.key, /^levy_reminder_L1_\d{4}-\d{2}-\d{2}$/);
  assert.equal(await status(buildPush(db, "admin", { type: "levy_reminder", estateId: "E1", levyId: "L1" })), 403);
  assert.equal(await status(buildPush(db, "third", { type: "levy_reminder", estateId: "E1", levyId: "nope" })), 404);
});

test("announcement reaches every other member of the estate only", async () => {
  const push = await buildPush(db, "admin", { type: "announcement", postId: "a1" });
  assert.deepEqual(push.recipients.sort(), ["blocker", "member", "third"]);
  assert.match(push.payload.title, /Amali Estate/);
  assert.equal(push.payload.deepLink, "localhive://post/a1");
});

test("non-announcements and non-admin announcements don't broadcast", async () => {
  assert.equal(await status(buildPush(db, "member", { type: "announcement", postId: "p1" })), 404);
  assert.equal(await status(buildPush(db, "member", { type: "announcement", postId: "a2" })), 403);
});

test("an emergency alerts everyone else in the estate, on Android's emergency channel", async () => {
  const push = await buildPush(db, "member", { type: "emergency", postId: "sos1" });
  assert.deepEqual(push.recipients.sort(), ["admin", "blocker", "third"]);
  assert.equal(push.payload.title, "🚨 Fire emergency at Block C");
  assert.equal(push.payload.body, "Ada needs help: Kitchen fire");
  assert.equal(push.payload.deepLink, "localhive://post/sos1");
  assert.equal(push.payload.channel, "emergency");
  assert.equal(push.key, "emergency_sos1");
});

test("emergencies only go out fresh, from their author, in the author's estate", async () => {
  assert.equal(await status(buildPush(db, "member", { type: "emergency", postId: "sos2" })), 410);
  assert.equal(await status(buildPush(db, "admin", { type: "emergency", postId: "sos1" })), 403);
  assert.equal(await status(buildPush(db, "other", { type: "emergency", postId: "sos3" })), 403);
  assert.equal(await status(buildPush(db, "member", { type: "emergency", postId: "p1" })), 404);
  assert.equal(await status(buildPush(db, "member", { type: "emergency", postId: "../sos1" })), 400);
});

test("a security report goes to the estate's admins only", async () => {
  const push = await buildPush(db, "member", { type: "security_report", postId: "sec1" });
  assert.deepEqual(push.recipients, ["admin"]);
  assert.match(push.payload.title, /Security report from Ada/);
  assert.equal(push.payload.body, "Someone climbing in (Back fence)");
  assert.equal(await status(buildPush(db, "admin", { type: "security_report", postId: "sec1" })), 403);
  assert.equal(await status(buildPush(db, "member", { type: "security_report", postId: "sos1" })), 404);
});

test("a new poll is pushed to the rest of the estate, only when an admin started it", async () => {
  const push = await buildPush(db, "admin", { type: "poll", estateId: "E1", pollId: "poll1" });
  assert.deepEqual(push.recipients.sort(), ["blocker", "member", "third"]);
  assert.equal(push.payload.body, "Raise the levy?");
  assert.equal(push.payload.deepLink, "localhive://polls");
  assert.equal(await status(buildPush(db, "member", { type: "poll", estateId: "E1", pollId: "poll2" })), 403);
  assert.equal(await status(buildPush(db, "member", { type: "poll", estateId: "E1", pollId: "poll1" })), 403);
  assert.equal(await status(buildPush(db, "admin", { type: "poll", estateId: "E1", pollId: "nope" })), 404);
});

test("light-is-back goes to members who asked for it, only right after the caller turned it on", async () => {
  const now = Date.now();
  const lightDb = fakeDb({
    ...docs,
    "users/admin": { estateId: "E1", lightAlerts: true },
    "users/third": { estateId: "E1", lightAlerts: true },
    "estates/E1/utilities/power": { isOn: true, since: now, updatedAt: now, updatedBy: "member", updatedByName: "Ada" },
  });
  const push = await buildPush(lightDb, "member", { type: "light_back", estateId: "E1" });
  assert.deepEqual(push.recipients.sort(), ["admin", "third"]);
  assert.equal(push.payload.body, "Ada just reported it.");
  assert.equal(push.payload.deepLink, "localhive://utilities");
  // Someone else's report, a confirmation that didn't change anything, or an old report.
  assert.equal(await status(buildPush(lightDb, "third", { type: "light_back", estateId: "E1" })), 409);
  const confirmDb = fakeDb({ ...docs, "estates/E1/utilities/power": { isOn: true, since: now - 5000, updatedAt: now, updatedBy: "member" } });
  assert.equal(await status(buildPush(confirmDb, "member", { type: "light_back", estateId: "E1" })), 409);
  const offDb = fakeDb({ ...docs, "estates/E1/utilities/power": { isOn: false, since: now, updatedAt: now, updatedBy: "member" } });
  assert.equal(await status(buildPush(offDb, "member", { type: "light_back", estateId: "E1" })), 409);
  assert.equal(await status(buildPush(lightDb, "other", { type: "light_back", estateId: "E1" })), 403);
});

test("a responder's push goes to whoever raised the SOS", async () => {
  const push = await buildPush(db, "third", { type: "sos_responding", postId: "sos4" });
  assert.deepEqual(push.recipients, ["member"]);
  assert.equal(push.payload.title, "🏃 Tunde is coming to help");
  assert.equal(push.key, "sos_responding_sos4_third");
  // Not listed as responding, the author themselves, or a resolved alert.
  assert.equal(await status(buildPush(db, "admin", { type: "sos_responding", postId: "sos4" })), 403);
  assert.equal(await status(buildPush(db, "member", { type: "sos_responding", postId: "sos4" })), 403);
  assert.equal(await status(buildPush(db, "member", { type: "sos_responding", postId: "sec1" })), 404);
});

test("'I'm safe now' tells the estate, only from the author of a resolved alert", async () => {
  const push = await buildPush(db, "member", { type: "sos_safe", postId: "sos5" });
  assert.deepEqual(push.recipients.sort(), ["admin", "blocker", "third"]);
  assert.equal(push.payload.title, "✅ Ada is safe now");
  assert.equal(push.payload.body, "The fire emergency at Block C is over.");
  assert.equal(await status(buildPush(db, "admin", { type: "sos_safe", postId: "sos5" })), 403);
  assert.equal(await status(buildPush(db, "member", { type: "sos_safe", postId: "sos4" })), 409);
});

test("only an admin can tell a member of their estate a post was removed", async () => {
  const push = await buildPush(db, "admin", { type: "notification", notificationId: "r1" });
  assert.deepEqual(push.recipients, ["member"]);
  assert.equal(push.payload.body, "Reason: spam");
  assert.equal(await status(buildPush(db, "member", { type: "notification", notificationId: "r2" })), 403);
  assert.equal(await status(buildPush(db, "admin", { type: "notification", notificationId: "r3" })), 403);
});

test("poll reminders go to non-voters, once a poll is half a day old and within a day of closing", async () => {
  const hour = 60 * 60 * 1000;
  const now = Date.UTC(2026, 9, 14, 12);
  const reminderDb = fakeDb({
    "estates/E1": { name: "Amali" },
    "users/admin": { estateId: "E1" },
    "users/member": { estateId: "E1" },
    "users/third": { estateId: "E1" },
    "users/other": { estateId: "E2" },
    "estates/E1/polls/due": { question: "Raise the levy?", createdAt: now - 48 * hour, closesAt: now + 5 * hour, closed: false },
    "estates/E1/polls/far": { question: "Later", createdAt: now - 48 * hour, closesAt: now + 72 * hour, closed: false },
    "estates/E1/polls/young": { question: "New", createdAt: now - 2 * hour, closesAt: now + 20 * hour, closed: false },
    "estates/E1/polls/shut": { question: "Done", createdAt: now - 48 * hour, closesAt: now + 5 * hour, closed: true },
    "estates/E1/polls/open": { question: "Forever", createdAt: now - 48 * hour, closesAt: 0, closed: false },
    "estates/E1/pollVotes/due_member": { pollId: "due", userId: "member", option: 0 },
  });
  const reminders = await pollReminders(reminderDb, now);
  assert.equal(reminders.length, 1);
  assert.equal(reminders[0].key, "poll_reminder_due");
  assert.deepEqual(reminders[0].recipients.sort(), ["admin", "third"]);
  assert.equal(reminders[0].payload.body, "Raise the levy?");
});

test("a fourth SOS in half an hour is refused", async () => {
  const now = Date.UTC(2026, 9, 16, 12);
  const minute = 60 * 1000;
  const sos = (id, author, ago) => [`posts/${id}`, { authorId: author, category: "emergency", createdAt: now - ago * minute }];
  const three = [sos("a", "u1", 1), sos("b", "u1", 10), sos("c", "u1", 20), sos("old", "u1", 45), sos("x", "u2", 1)];
  assert.equal(await tooManyEmergencies(fakeDb(Object.fromEntries(three)), "u1", now), false);
  const four = [...three, sos("d", "u1", 0)];
  assert.equal(await tooManyEmergencies(fakeDb(Object.fromEntries(four)), "u1", now), true);
  // A post stamped in the future still counts.
  const future = [...three, sos("f", "u1", -60)];
  assert.equal(await tooManyEmergencies(fakeDb(Object.fromEntries(future)), "u1", now), true);
});

test("only the treasurer's fresh bank change is announced, to everyone else", async () => {
  const now = Date.now();
  const db = fakeDb({
    "estates/E1": { name: "Amali", adminIds: ["admin"], treasurerId: "treasurer" },
    "estates/E1/private/bank": { bankName: "GTBank", accountNumber: "0123456789", updatedBy: "treasurer", updatedByName: "Ada", updatedAt: now - 1000 },
    "users/admin": { estateId: "E1" },
    "users/treasurer": { estateId: "E1" },
    "users/member": { estateId: "E1" },
  });
  const push = await buildPush(db, "treasurer", { type: "bank_changed", estateId: "E1" });
  assert.deepEqual(push.recipients.sort(), ["admin", "member"]);
  assert.match(push.payload.body, /GTBank \(account ending 6789\)/);
  assert.equal(push.inbox.type, "BANK_DETAILS_CHANGED");
  await assert.rejects(buildPush(db, "admin", { type: "bank_changed", estateId: "E1" }), /Only the treasurer/);
});

test("upload signatures cover the kind's limits", async () => {
  const now = Date.UTC(2026, 9, 13);
  const s = await signUpload({
    cloudName: "demo", apiKey: "k", apiSecret: "secret", folder: "f", now,
    limits: { allowed_formats: "jpg,png", transformation: "c_limit,w_2000,h_2000" },
  });
  const t = Math.floor(now / 1000);
  const expected = createHash("sha1")
    .update(`allowed_formats=jpg,png&folder=f&timestamp=${t}&transformation=c_limit,w_2000,h_2000secret`)
    .digest("hex");
  assert.equal(s.signature, expected);
  assert.deepEqual(s.params, { folder: "f", timestamp: String(t), allowed_formats: "jpg,png", transformation: "c_limit,w_2000,h_2000" });
});

test("estates missing or mismatching nameLower get it filled in", async () => {
  const db = fakeDb({
    "estates/old": { name: "Sunrise Estate" },
    "estates/done": { name: "Amali", nameLower: "amali" },
    "estates/stale": { name: "Green Valley", nameLower: "old name" },
    "estates/broken": { adminIds: [] },
  });
  assert.deepEqual(await estateNameLowerUpdates(db), [
    { update: "estates/old", fields: { nameLower: "sunrise estate" } },
    { update: "estates/stale", fields: { nameLower: "green valley" } },
  ]);
});

test("Cloudinary signature matches the documented algorithm", async () => {
  const now = Date.UTC(2026, 9, 13) ;
  const s = await signUpload({ cloudName: "demo", apiKey: "k", apiSecret: "secret", folder: "localhive/postImages/u1", now });
  const expected = createHash("sha1").update(`folder=localhive/postImages/u1&timestamp=${Math.floor(now / 1000)}secret`).digest("hex");
  assert.equal(s.signature, expected);
  assert.equal(s.timestamp, Math.floor(now / 1000));
});

test("Firestore values decode to plain JS", () => {
  assert.equal(decodeValue({ integerValue: "3" }), 3);
  assert.deepEqual(decodeValue({ arrayValue: { values: [{ stringValue: "a" }] } }), ["a"]);
  assert.deepEqual(decodeValue({ mapValue: { fields: { x: { booleanValue: true } } } }), { x: true });
  assert.deepEqual(decodeValue({ arrayValue: {} }), []);
});

test("only videos over the app's size limit are picked for deletion", () => {
  const ids = oversizedVideoIds([
    { public_id: "localhive/postVideos/u1/ok", bytes: MAX_VIDEO_BYTES },
    { public_id: "localhive/postVideos/u1/huge", bytes: MAX_VIDEO_BYTES + 1 },
    { public_id: "localhive/postVideos/u2/unknown" },
  ]);
  assert.deepEqual(ids, ["localhive/postVideos/u1/huge"]);
});

test("invite links: code parsing, Play Store referrer and app intent", () => {
  assert.equal(inviteCodeIn("/join/abc123"), "ABC123");
  assert.equal(inviteCodeIn("/join/ABC123/"), "ABC123");
  assert.equal(inviteCodeIn("/join/<script>"), null);
  assert.equal(inviteCodeIn("/join/"), null);
  assert.equal(
    playStoreUrl("com.eleazar.localhive", "ABC123"),
    "https://play.google.com/store/apps/details?id=com.eleazar.localhive&referrer=invite%3DABC123"
  );
  assert.match(appIntentUrl("com.eleazar.localhive", "ABC123"), /^intent:\/\/join\/ABC123#Intent;scheme=localhive;package=com\.eleazar\.localhive;S\.browser_fallback_url=https%3A%2F%2Fplay/);
});

test("public GETs: join page and assetlinks, nothing else", async () => {
  const env = { ANDROID_PACKAGE: "com.eleazar.localhive", ANDROID_CERT_SHA256: "AA:BB, CC:DD" };
  const page = publicResponse(new Request("https://x.dev/join/ABC123"), env);
  assert.match(await page.text(), /ABC123/);
  const links = await publicResponse(new Request("https://x.dev/.well-known/assetlinks.json"), env).json();
  assert.deepEqual(links[0].target.sha256_cert_fingerprints, ["AA:BB", "CC:DD"]);
  assert.equal(publicResponse(new Request("https://x.dev/notify"), env), null);
});

test("public GET: account deletion page carries the web API key", async () => {
  const page = publicResponse(new Request("https://x.dev/delete-account"), { FIREBASE_WEB_API_KEY: "key-123" });
  assert.match(page.headers.get("Content-Type"), /text\/html/);
  assert.match(page.headers.get("Content-Security-Policy"), /connect-src 'self' https:\/\/identitytoolkit\.googleapis\.com/);
  const html = await page.text();
  assert.match(html, /const KEY = "key-123"/);
  assert.match(html, /fetch\("\/delete-account"/);
});
