import { test } from "node:test";
import assert from "node:assert/strict";
import { deleteAccount, requireRecentLogin, DELETED_NAME } from "../src/account.js";

/** In-memory Firestore that understands just enough of query and commit for deleteAccount. */
function memoryDb(initial) {
  const docs = structuredClone(initial);
  const collectionOf = (path) => path.split("/").at(-2);
  const matches = (data, [field, op, value]) =>
    op === "EQUAL" ? data[field] === value
    : op === "IN" ? value.includes(data[field])
    : op === "ARRAY_CONTAINS" ? (data[field] || []).includes(value)
    : false;
  return {
    docs,
    get: async (path) => docs[path] ?? null,
    query: async (collection, where = [], { parent = "", group = false } = {}) =>
      Object.entries(docs)
        .filter(([path]) => collectionOf(path) === collection)
        .filter(([path]) => group || (parent ? path.startsWith(`${parent}/`) && path.split("/").length === parent.split("/").length + 2 : path.split("/").length === 2))
        .filter(([, data]) => where.every((w) => matches(data, w)))
        .map(([path, data]) => ({ path, data })),
    commit: async (writes) => {
      for (const w of writes) {
        if (w.delete) { delete docs[w.delete]; continue; }
        const path = w.update || w.transform;
        assert.ok(docs[path], `write to missing ${path}`);
        for (const [k, v] of Object.entries(w.fields || {})) {
          const [head, ...rest] = k.split(".");
          if (rest.length) docs[path][head] = { ...docs[path][head], [rest.join(".")]: v };
          else docs[path][head] = v;
        }
        for (const [k, n] of Object.entries(w.increment || {})) docs[path][k] = (docs[path][k] || 0) + n;
        for (const [k, vals] of Object.entries(w.arrayRemove || {})) docs[path][k] = (docs[path][k] || []).filter((x) => !vals.includes(x));
      }
    },
  };
}

const base = () => ({
  "estates/E1": { name: "Amali", ownerId: "owner", adminIds: ["owner", "admin"], memberCount: 4 },
  "estates/E1/private/invite": { code: "ABC123" },
  "inviteCodes/ABC123": { estateId: "E1" },
  "users/owner": { estateId: "E1" },
  "users/admin": { estateId: "E1" },
  "users/me": { estateId: "E1", fcmToken: "t" },
  "users/other": { estateId: "E1" },
  "users/me/blocks/other": { blockedAt: 1 },
  "posts/mine": { authorId: "me", estateId: "E1", likedBy: ["me", "other"], likeCount: 2, commentCount: 1 },
  "posts/mine/comments/c1": { authorId: "other", postId: "mine" },
  "posts/theirs": { authorId: "other", estateId: "E1", likedBy: ["me", "other"], likeCount: 2, commentCount: 3 },
  "posts/theirs/comments/c2": { authorId: "me", postId: "theirs" },
  "posts/theirs/comments/c3": { authorId: "me", postId: "theirs" },
  "posts/theirs/comments/c4": { authorId: "other", postId: "theirs" },
  "joinRequests/E1_me": { userId: "me", estateId: "E1" },
  "notifications/n1": { recipientId: "me" },
  "notifications/n2": { recipientId: "other" },
  "chats/me_other": { participants: ["me", "other"], participantNames: { me: "Me", other: "Other" } },
  "chats/me_other/messages/m1": { senderId: "me", content: "hi" },
});

const noAuth = { deleteAuthUser: async () => {} };

test("deletes everything a member made and fixes the counts left behind", async () => {
  const db = memoryDb(base());
  const authDeleted = [];
  await deleteAccount(db, "me", { deleteAuthUser: async (id) => authDeleted.push(id) });
  const d = db.docs;

  assert.equal(d["users/me"], undefined);
  assert.equal(d["users/me/blocks/other"], undefined);
  assert.equal(d["posts/mine"], undefined);
  assert.equal(d["posts/mine/comments/c1"], undefined, "comments on my post go with it");
  assert.equal(d["posts/theirs/comments/c2"], undefined);
  assert.ok(d["posts/theirs/comments/c4"], "other people's comments elsewhere stay");
  assert.equal(d["posts/theirs"].commentCount, 1);
  assert.deepEqual(d["posts/theirs"].likedBy, ["other"]);
  assert.equal(d["posts/theirs"].likeCount, 1);
  assert.equal(d["joinRequests/E1_me"], undefined);
  assert.equal(d["notifications/n1"], undefined);
  assert.ok(d["notifications/n2"]);
  assert.equal(d["estates/E1"].memberCount, 3);
  assert.deepEqual(d["estates/E1"].adminIds, ["owner", "admin"]);
  assert.equal(d["chats/me_other"].participantNames.me, DELETED_NAME);
  assert.deepEqual(d["chats/me_other"].deletedParticipants, ["me"]);
  assert.ok(d["chats/me_other/messages/m1"], "the other person keeps the conversation");
  assert.deepEqual(authDeleted, ["me"]);
});

test("an admin loses admin on the way out", async () => {
  const db = memoryDb(base());
  await deleteAccount(db, "admin", noAuth);
  assert.deepEqual(db.docs["estates/E1"].adminIds, ["owner"]);
  assert.equal(db.docs["estates/E1"].memberCount, 3);
});

test("the owner hands the estate to another admin", async () => {
  const db = memoryDb(base());
  await deleteAccount(db, "owner", noAuth);
  assert.equal(db.docs["estates/E1"].ownerId, "admin");
  assert.deepEqual(db.docs["estates/E1"].adminIds, ["admin"]);
});

test("an owner with members but no other admin is refused, and nothing changes", async () => {
  const docs = base();
  docs["estates/E1"].adminIds = ["owner"];
  const db = memoryDb(docs);
  let authCalled = false;
  await assert.rejects(
    deleteAccount(db, "owner", { deleteAuthUser: async () => { authCalled = true; } }),
    (e) => e.status === 409
  );
  assert.deepEqual(db.docs, docs);
  assert.equal(authCalled, false);
});

test("the last person out takes the estate and its invite code with them", async () => {
  const db = memoryDb({
    "estates/E9": { ownerId: "solo", adminIds: ["solo"], memberCount: 1 },
    "estates/E9/private/invite": { code: "ZZZ999" },
    "inviteCodes/ZZZ999": { estateId: "E9" },
    "users/solo": { estateId: "E9" },
  });
  await deleteAccount(db, "solo", noAuth);
  assert.deepEqual(db.docs, {});
});

test("repeating a delete that already removed the data still deletes the login", async () => {
  const db = memoryDb({});
  const authDeleted = [];
  await deleteAccount(db, "ghost", { deleteAuthUser: async (id) => authDeleted.push(id) });
  assert.deepEqual(authDeleted, ["ghost"]);
});

test("a failing photo cleanup doesn't stop the delete", async () => {
  const db = memoryDb(base());
  const authDeleted = [];
  await deleteAccount(db, "me", {
    deleteAuthUser: async (id) => authDeleted.push(id),
    deleteMedia: async () => { throw new Error("cloudinary down"); },
  });
  assert.deepEqual(authDeleted, ["me"]);
});

test("deleting needs a password entered in the last five minutes", () => {
  const now = 1_000_000_000_000;
  assert.doesNotThrow(() => requireRecentLogin(now / 1000 - 60, now));
  assert.throws(() => requireRecentLogin(now / 1000 - 600, now), (e) => e.status === 401);
});

test("deleting the treasurer's account leaves the estate without a treasurer", async () => {
  const initial = base();
  initial["estates/E1"].treasurerId = "me";
  const db = memoryDb(initial);
  await deleteAccount(db, "me", noAuth);
  assert.equal(db.docs["estates/E1"].treasurerId, "");
  assert.equal(db.docs["estates/E1"].memberCount, 3);
});

test("deleting someone else leaves the treasurer in place", async () => {
  const initial = base();
  initial["estates/E1"].treasurerId = "other";
  const db = memoryDb(initial);
  await deleteAccount(db, "me", noAuth);
  assert.equal(db.docs["estates/E1"].treasurerId, "other");
});
