// The rules with strictFinance() and strictProfiles() switched on, as they will be once everyone
// is on 1.07: what 1.07 writes still passes, and what only 1.06 writes is refused.
import { test, before, beforeEach, after } from "node:test";
import { readFileSync } from "node:fs";
import { assertFails, assertSucceeds, initializeTestEnvironment } from "@firebase/rules-unit-testing";
import { doc, increment, serverTimestamp, setDoc, writeBatch } from "firebase/firestore";

let env;

before(async () => {
  const rules = readFileSync(new URL("../../firestore.rules", import.meta.url), "utf8")
    .replace(/function strictFinance\(\) \{\s*return false;/, "function strictFinance() {\n        return true;")
    .replace(/function strictProfiles\(\) \{\s*return false;/, "function strictProfiles() {\n      return true;")
    .replace(/function postingLimitsOn\(\) \{\s*return false;/, "function postingLimitsOn() {\n      return true;");
  if (!rules.includes("function postingLimitsOn() {\n      return true;")) throw new Error("Couldn't switch posting limits on");
  if (!rules.includes("return true;")) throw new Error("Couldn't switch strict mode on");
  env = await initializeTestEnvironment({ projectId: "demo-localhive-strict", firestore: { rules } });
});

after(async () => env.cleanup());

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "estates/E1"), { name: "Amali", adminIds: ["admin"], treasurerId: "treasurer", memberCount: 2 });
    await setDoc(doc(db, "users/treasurer"), { estateId: "E1" });
    await setDoc(doc(db, "users/ada"), { estateId: "E1" });
    await setDoc(doc(db, "posts/p0"), { authorId: "treasurer", estateId: "E1", content: "Hi", createdAt: Date.now() });
    await setDoc(doc(db, "estates/E1/levies/L1"), { title: "Security", amount: 5000, hasPayments: false });
    await setDoc(doc(db, "estates/E1/levies/L1/payments/ada"), { userId: "ada", status: "pending", amount: 5000 });
  });
});

const as = (uid) => env.authenticatedContext(uid).firestore();
const logEntry = (now) => ({ action: "x", summary: "x", amount: 0, by: "treasurer", byName: "T", at: now });

test("1.07 confirms a payment with its log entry and the paid-into mark", async () => {
  const db = as("treasurer");
  const now = Date.now();
  const batch = writeBatch(db);
  batch.set(doc(db, "estates/E1/levies/L1/payments/ada"), { userId: "ada", amount: 5000, status: "paid", reviewedAt: now });
  batch.set(doc(db, `estates/E1/financeLog/pay_L1_ada_${now}`), logEntry(now));
  batch.update(doc(db, "estates/E1/levies/L1"), { hasPayments: true });
  await assertSucceeds(batch.commit());
});

test("1.06's confirmation, without a log entry, is refused", async () => {
  const now = Date.now();
  await assertFails(setDoc(doc(as("treasurer"), "estates/E1/levies/L1/payments/ada"), { userId: "ada", amount: 5000, status: "paid", reviewedAt: now }));
});

test("expenses need their log entry", async () => {
  const db = as("treasurer");
  const now = Date.now();
  const expense = { title: "Diesel", amount: 100, createdAt: now };
  await assertFails(setDoc(doc(db, "estates/E1/expenses/x1"), expense));
  const batch = writeBatch(db);
  batch.set(doc(db, "estates/E1/expenses/x2"), expense);
  batch.set(doc(db, "estates/E1/financeLog/expense_x2_added"), logEntry(now));
  await assertSucceeds(batch.commit());
});

test("profiles can't carry an email", async () => {
  await assertFails(setDoc(doc(as("ada"), "users/ada"), { email: "ada@example.com" }, { merge: true }));
  await assertSucceeds(setDoc(doc(as("ada"), "users/ada"), { bio: "Hi" }, { merge: true }));
});

// A post as 1.08 makes it: the post, and the caller's stamp naming it, in one batch.
function postWithStamp(db, id, extra = {}) {
  const batch = writeBatch(db);
  batch.set(doc(db, `posts/${id}`), { authorId: "ada", estateId: "E1", content: "Hello", createdAt: Date.now(), ...extra });
  batch.set(doc(db, "postingLimits/ada"), { lastPostAt: serverTimestamp(), lastPostId: id }, { merge: true });
  return batch.commit();
}

test("a post needs its stamp, and a second one within 20 seconds is refused", async () => {
  const db = as("ada");
  await assertFails(setDoc(doc(db, "posts/bare"), { authorId: "ada", estateId: "E1", content: "x", createdAt: Date.now() }));
  await assertSucceeds(postWithStamp(db, "first"));
  await assertFails(postWithStamp(db, "second"));
});

test("one stamp can't carry several posts", async () => {
  const db = as("ada");
  const batch = writeBatch(db);
  for (const id of ["s1", "s2"]) batch.set(doc(db, `posts/${id}`), { authorId: "ada", estateId: "E1", content: "spam", createdAt: Date.now() });
  batch.set(doc(db, "postingLimits/ada"), { lastPostAt: serverTimestamp(), lastPostId: "s1" });
  await assertFails(batch.commit());
});

test("an SOS goes through straight after a post, without a stamp", async () => {
  const db = as("ada");
  await assertSucceeds(postWithStamp(db, "first"));
  await assertSucceeds(setDoc(doc(db, "posts/sos"), {
    authorId: "ada", estateId: "E1", category: "emergency", incidentStatus: "open", emergencyType: "fire",
    location: "", content: "Fire", createdAt: Date.now(),
  }));
});

test("a stamp can't be backdated or set by someone else", async () => {
  await assertFails(setDoc(doc(as("ada"), "postingLimits/ada"), { lastPostAt: new Date(0), lastPostId: "x" }));
  await assertFails(setDoc(doc(as("treasurer"), "postingLimits/ada"), { lastPostAt: serverTimestamp(), lastPostId: "x" }));
});

test("comments need their stamp too", async () => {
  const db = as("ada");
  const comment = { authorId: "ada", postId: "p0", content: "Nice", createdAt: Date.now() };
  await assertFails(setDoc(doc(db, "posts/p0/comments/bare"), comment));
  const batch = writeBatch(db);
  batch.set(doc(db, "posts/p0/comments/c1"), comment);
  batch.update(doc(db, "posts/p0"), { commentCount: increment(1) });
  batch.set(doc(db, "postingLimits/ada"), { lastCommentAt: serverTimestamp(), lastCommentId: "c1" }, { merge: true });
  await assertSucceeds(batch.commit());
});

// A comment as the app sends it, with its stamp, in one batch.
function commentWithStamp(db, id, extra = {}) {
  const batch = writeBatch(db);
  batch.set(doc(db, `posts/p0/comments/${id}`), { authorId: "ada", postId: "p0", content: "", createdAt: Date.now(), ...extra });
  batch.update(doc(db, "posts/p0"), { commentCount: increment(1) });
  batch.set(doc(db, "postingLimits/ada"), { lastCommentAt: serverTimestamp(), lastCommentId: id }, { merge: true });
  return batch.commit();
}

test("a comment can carry a photo from the app's own Cloudinary account, and nothing else", async () => {
  const db = as("ada");
  await assertFails(commentWithStamp(db, "elsewhere", { imageUrl: "https://example.com/x.jpg" }));
  await assertFails(commentWithStamp(db, "otherAccount", { imageUrl: "https://res.cloudinary.com/demo/image/upload/x.jpg" }));
  await assertFails(commentWithStamp(db, "notText", { imageUrl: 42 }));
  await assertFails(commentWithStamp(db, "long", { imageUrl: "https://res.cloudinary.com/bsgju3dx/" + "x".repeat(1000) }));
  await assertSucceeds(commentWithStamp(db, "photo", {
    imageUrl: "https://res.cloudinary.com/bsgju3dx/image/upload/v1/localhive/postImages/ada/abc.jpg",
  }));
});
