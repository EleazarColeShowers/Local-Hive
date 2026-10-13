import { test } from "node:test";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { buildPush } from "../src/index.js";
import { signUpload } from "../src/cloudinary.js";
import { decodeValue } from "../src/firestore.js";

/** In-memory stand-in for the Firestore REST client. */
function fakeDb(docs) {
  return {
    get: async (path) => docs[path] ?? null,
    idsWhere: async (collection, field, value) =>
      Object.entries(docs)
        .filter(([p, d]) => p.startsWith(`${collection}/`) && p.split("/").length === 2 && d[field] === value)
        .map(([p]) => p.split("/")[1]),
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
};
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

test("announcement reaches every other member of the estate only", async () => {
  const push = await buildPush(db, "admin", { type: "announcement", postId: "a1" });
  assert.deepEqual(push.recipients.sort(), ["member"]);
  assert.match(push.payload.title, /Amali Estate/);
  assert.equal(push.payload.deepLink, "localhive://post/a1");
});

test("non-announcements and non-admin announcements don't broadcast", async () => {
  assert.equal(await status(buildPush(db, "member", { type: "announcement", postId: "p1" })), 404);
  assert.equal(await status(buildPush(db, "member", { type: "announcement", postId: "a2" })), 403);
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
