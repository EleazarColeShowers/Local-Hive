// Security rules tests, run against the Firestore emulator: `npm test` in this folder.
// Each test starts from the same small estate, seeded with the rules switched off.
import { test, before, beforeEach, after } from "node:test";
import { readFileSync } from "node:fs";
import { assertFails, assertSucceeds, initializeTestEnvironment } from "@firebase/rules-unit-testing";
import {
  arrayRemove, arrayUnion, deleteDoc, doc, getDoc, getDocs, collection, query, where, increment, setDoc, updateDoc, writeBatch,
} from "firebase/firestore";

let env;
const HOUR = 60 * 60 * 1000;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-localhive",
    firestore: { rules: readFileSync(new URL("../../firestore.rules", import.meta.url), "utf8") },
  });
});

after(async () => env.cleanup());

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = Date.now();
    const seed = {
      "estates/E1": { name: "Amali", adminIds: ["admin"], ownerId: "admin", treasurerId: "treasurer", memberCount: 5, joinMode: "both" },
      "users/admin": { estateId: "E1" },
      "users/treasurer": { estateId: "E1" },
      "users/ada": { estateId: "E1" },
      "users/bayo": { estateId: "E1" },
      "users/chi": { estateId: "E1" },
      "users/stranger": {},
      "users/kicked": {},
      "inviteCodes/ABC234": { estateId: "E1" },
      "estates/E1/removed/kicked": { name: "Kicked", removedBy: "admin", at: now },
      "posts/p1": { authorId: "ada", estateId: "E1", content: "Hi", likedBy: [], likeCount: 0, commentCount: 0, createdAt: now },
      "posts/sec1": { authorId: "ada", estateId: "E1", category: "security", incidentStatus: "open", content: "Fence", createdAt: now },
      "posts/sos1": { authorId: "ada", estateId: "E1", category: "emergency", incidentStatus: "open", emergencyType: "fire", content: "Fire", createdAt: now },
      "estates/E1/levies/L1": { title: "Security", amount: 5000 },
      "estates/E1/levies/L2": { title: "Old levy", amount: 5000, hasPayments: true },
      "estates/E1/levies/L1/payments/bayo": { userId: "bayo", status: "paid", amount: 5000, reviewedAt: now },
      "estates/E1/expenses/new": { title: "Diesel", amount: 100, createdAt: now - HOUR },
      "estates/E1/expenses/old": { title: "Paint", amount: 100, createdAt: now - 3 * 24 * HOUR },
      // No. 12 has 4 flats: Ada lives in flat 1, Bayo in flat 2. Chi lives at No. 14.
      "estates/E1/buildings/B12": { name: "No. 12", flats: 4 },
      "estates/E1/buildings/B14": { name: "No. 14", flats: 2 },
      "estates/E1/residents/ada": { buildingId: "B12", flat: 1, setBy: "ada", at: now },
      "estates/E1/residents/bayo": { buildingId: "B12", flat: 2, setBy: "bayo", at: now },
      "estates/E1/residents/chi": { buildingId: "B14", flat: 1, setBy: "chi", at: now },
      "estates/E1/flatLevies/F1": { title: "Water", amount: 2000, dueDate: 0, note: "", createdBy: "treasurer", createdAt: now, hasPayments: false },
      "config/app": { minVersion: "1.07" },
    };
    for (const [path, data] of Object.entries(seed)) await setDoc(doc(db, path), data);
  });
});

const as = (uid) => env.authenticatedContext(uid).firestore();

// ───── Membership ─────

test("joining with a code moves the count up by one, in the same batch (as 1.06 does it)", async () => {
  const db = as("stranger");
  const batch = writeBatch(db);
  batch.set(doc(db, "joinRequests/E1_stranger"), { userId: "stranger", estateId: "E1", status: "approved", inviteCode: "ABC234" });
  batch.set(doc(db, "users/stranger"), { estateId: "E1" }, { merge: true });
  batch.update(doc(db, "estates/E1"), { memberCount: increment(1) });
  await assertSucceeds(batch.commit());
});

test("nobody can push the member count around without joining or leaving", async () => {
  await assertFails(updateDoc(doc(as("stranger"), "estates/E1"), { memberCount: increment(1) }));
  await assertFails(updateDoc(doc(as("stranger"), "estates/E1"), { memberCount: increment(-1) }));
  await assertFails(updateDoc(doc(as("ada"), "estates/E1"), { memberCount: increment(-1) }));
});

test("leaving moves the count down by one (as 1.06 does it)", async () => {
  const db = as("ada");
  const batch = writeBatch(db);
  batch.update(doc(db, "users/ada"), { estateId: null });
  batch.update(doc(db, "estates/E1"), { memberCount: increment(-1) });
  await assertSucceeds(batch.commit());
});

test("someone an admin removed can't let themselves back in with a code", async () => {
  const db = as("kicked");
  await assertFails(setDoc(doc(db, "joinRequests/E1_kicked"), { userId: "kicked", estateId: "E1", status: "approved", inviteCode: "ABC234" }));
  // Asking is still allowed; an admin decides.
  await assertSucceeds(setDoc(doc(db, "joinRequests/E1_kicked"), { userId: "kicked", estateId: "E1", status: "pending" }));
});

test("only admins see and manage the removed list", async () => {
  await assertFails(getDoc(doc(as("ada"), "estates/E1/removed/kicked")));
  await assertSucceeds(getDoc(doc(as("admin"), "estates/E1/removed/kicked")));
  await assertFails(deleteDoc(doc(as("kicked"), "estates/E1/removed/kicked")));
  await assertSucceeds(deleteDoc(doc(as("admin"), "estates/E1/removed/kicked")));
});

// ───── Posts ─────

test("a like adds only the caller and counts one (as 1.06 does it)", async () => {
  const db = as("bayo");
  await assertSucceeds(updateDoc(doc(db, "posts/p1"), { likedBy: arrayUnion("bayo"), likeCount: increment(1) }));
  await assertSucceeds(updateDoc(doc(db, "posts/p1"), { likedBy: arrayRemove("bayo"), likeCount: increment(-1) }));
});

test("likes can't be inflated or forged", async () => {
  await assertFails(updateDoc(doc(as("bayo"), "posts/p1"), { likedBy: arrayUnion("bayo"), likeCount: 50 }));
  await assertFails(updateDoc(doc(as("bayo"), "posts/p1"), { likedBy: arrayUnion("bayo", "chi"), likeCount: increment(2) }));
  // The author has no back door either.
  await assertFails(updateDoc(doc(as("ada"), "posts/p1"), { likeCount: 99 }));
  await assertFails(updateDoc(doc(as("bayo"), "posts/p1"), { commentCount: 40 }));
  await assertSucceeds(updateDoc(doc(as("bayo"), "posts/p1"), { commentCount: increment(1) }));
});

test("only an admin moves a security report along; an author can only close their own emergency", async () => {
  const stamp = (by) => ({ incidentStatus: "resolved", statusUpdatedBy: by, statusUpdatedByName: "x", statusUpdatedAt: Date.now() });
  await assertFails(updateDoc(doc(as("ada"), "posts/sec1"), stamp("admin")));
  await assertFails(updateDoc(doc(as("ada"), "posts/sec1"), stamp("ada")));
  await assertSucceeds(updateDoc(doc(as("admin"), "posts/sec1"), stamp("admin")));
  await assertSucceeds(updateDoc(doc(as("ada"), "posts/sos1"), stamp("ada")));
});

test("an author can still edit their post", async () => {
  await assertSucceeds(updateDoc(doc(as("ada"), "posts/p1"), { content: "Hello", mentions: {}, editedAt: Date.now() }));
});

// ───── Per-member levies ─────

test("a levy with a confirmed payment can't be deleted", async () => {
  await assertFails(deleteDoc(doc(as("treasurer"), "estates/E1/levies/L2")));
  await assertSucceeds(deleteDoc(doc(as("treasurer"), "estates/E1/levies/L1")));
});

test("a confirmed payment stays on the books", async () => {
  await assertFails(deleteDoc(doc(as("treasurer"), "estates/E1/levies/L1/payments/bayo")));
});

test("taking back a confirmed payment needs a reason", async () => {
  const db = as("treasurer");
  const ref = doc(db, "estates/E1/levies/L1/payments/bayo");
  const base = { userId: "bayo", amount: 5000, reviewedAt: Date.now() };
  await assertFails(setDoc(ref, { ...base, status: "rejected" }));
  await assertSucceeds(setDoc(ref, { ...base, status: "rejected", undoReason: "Transfer bounced" }));
});

test("residents send proof for the levy's amount (as 1.06 does it), and nothing less", async () => {
  const ref = doc(as("ada"), "estates/E1/levies/L1/payments/ada");
  const proof = { userId: "ada", userName: "Ada", levyId: "L1", status: "pending", proofUrl: "https://x", submittedAt: Date.now() };
  await assertFails(setDoc(ref, { ...proof, amount: 1 }));
  await assertSucceeds(setDoc(ref, { ...proof, amount: 5000 }));
});

test("the treasurer confirms proof (as 1.06 does it)", async () => {
  await env.withSecurityRulesDisabled((ctx) =>
    setDoc(doc(ctx.firestore(), "estates/E1/levies/L1/payments/ada"), { userId: "ada", status: "pending", amount: 5000 }));
  const now = Date.now();
  await assertSucceeds(setDoc(doc(as("treasurer"), "estates/E1/levies/L1/payments/ada"), {
    userId: "ada", userName: "Ada", levyId: "L1", amount: 5000, status: "paid", proofUrl: null,
    submittedAt: now, reviewedAt: now, confirmedByName: "T",
  }));
});

// ───── Expenses and the log ─────

test("an expense can be deleted for a day, then it stays", async () => {
  await assertSucceeds(deleteDoc(doc(as("treasurer"), "estates/E1/expenses/new")));
  await assertFails(deleteDoc(doc(as("treasurer"), "estates/E1/expenses/old")));
  await assertFails(updateDoc(doc(as("treasurer"), "estates/E1/expenses/new"), { amount: 1 }));
});

test("expenses must be stamped with the real time", async () => {
  const db = as("treasurer");
  await assertFails(setDoc(doc(db, "estates/E1/expenses/x"), { title: "x", amount: 5, createdAt: Date.now() - 5 * 24 * HOUR }));
  await assertSucceeds(setDoc(doc(db, "estates/E1/expenses/y"), { title: "x", amount: 5, createdAt: Date.now() }));
});

test("the finance log is the treasurer's to add to, everyone's to read, and nobody's to change", async () => {
  const entry = { action: "expense_added", summary: "Diesel ₦100", amount: 100, by: "treasurer", byName: "T", at: Date.now() };
  await assertFails(setDoc(doc(as("ada"), "estates/E1/financeLog/e1"), { ...entry, by: "ada" }));
  await assertSucceeds(setDoc(doc(as("treasurer"), "estates/E1/financeLog/e1"), entry));
  await assertSucceeds(getDoc(doc(as("ada"), "estates/E1/financeLog/e1")));
  await assertFails(deleteDoc(doc(as("treasurer"), "estates/E1/financeLog/e1")));
  await assertFails(updateDoc(doc(as("treasurer"), "estates/E1/financeLog/e1"), { summary: "nothing" }));
});

// ───── Buildings and flats ─────

test("members pick a flat that exists, for themselves; the treasurer can correct it", async () => {
  const pick = (by, building, flat) => ({ buildingId: building, buildingName: "No. 12", flat, userName: "Chi", setBy: by, at: Date.now() });
  await assertSucceeds(setDoc(doc(as("chi"), "estates/E1/residents/chi"), pick("chi", "B12", 4)));
  await assertFails(setDoc(doc(as("chi"), "estates/E1/residents/chi"), pick("chi", "B12", 5)));
  await assertFails(setDoc(doc(as("chi"), "estates/E1/residents/chi"), pick("chi", "nope", 1)));
  await assertFails(setDoc(doc(as("chi"), "estates/E1/residents/ada"), pick("chi", "B12", 3)));
  await assertSucceeds(setDoc(doc(as("treasurer"), "estates/E1/residents/ada"), pick("treasurer", "B12", 3)));
  await assertFails(setDoc(doc(as("stranger"), "estates/E1/residents/stranger"), pick("stranger", "B12", 3)));
});

test("only the treasurer or an admin manages buildings", async () => {
  const b = { name: "No. 16", flats: 6, createdBy: "x", createdAt: Date.now() };
  await assertFails(setDoc(doc(as("ada"), "estates/E1/buildings/B16"), b));
  await assertSucceeds(setDoc(doc(as("treasurer"), "estates/E1/buildings/B16"), b));
  await assertSucceeds(setDoc(doc(as("admin"), "estates/E1/buildings/B18"), b));
  await assertFails(setDoc(doc(as("admin"), "estates/E1/buildings/B20"), { ...b, flats: 0 }));
});

/** Ada sending one proof for [flats] of No. 12, the way the app batches it. */
function sendProof(db, flats, { building = "B12", amountPerFlat = 2000, group = "g1" } = {}) {
  const now = Date.now();
  const batch = writeBatch(db);
  batch.set(doc(db, `estates/E1/flatLevies/F1/proofs/${group}`), {
    payerId: "ada", payerName: "Ada", buildingId: building, flats: flats.map((f) => `${building}_${f}`),
    amount: amountPerFlat * flats.length, proofUrl: "https://x", submittedAt: now,
  });
  for (const flat of flats) {
    batch.set(doc(db, `estates/E1/flatLevies/F1/flats/${building}_${flat}`), {
      buildingId: building, buildingName: "No. 12", flat, levyId: "F1", amount: amountPerFlat, status: "pending",
      payerId: "ada", payerName: "Ada", groupId: group, submittedAt: now,
    });
  }
  return batch.commit();
}

test("a resident pays for flats in their own building, at the levy's amount", async () => {
  await assertSucceeds(sendProof(as("ada"), [1, 3]));
});

test("nobody pays into another building, a flat that doesn't exist, or short", async () => {
  await assertFails(sendProof(as("ada"), [1], { building: "B14" }));
  await assertFails(sendProof(as("ada"), [5]));
  await assertFails(sendProof(as("ada"), [1], { amountPerFlat: 100 }));
});

test("a flat someone else already paid for can't be paid over", async () => {
  await assertSucceeds(sendProof(as("ada"), [3]));
  await assertFails(sendProof(as("ada"), [3], { group: "g2" }));
});

test("neighbours in the building see which flats paid, but not the proof", async () => {
  await assertSucceeds(sendProof(as("ada"), [1]));
  await assertSucceeds(getDoc(doc(as("bayo"), "estates/E1/flatLevies/F1/flats/B12_1")));
  await assertSucceeds(getDocs(query(collection(as("bayo"), "estates/E1/flatLevies/F1/flats"), where("buildingId", "==", "B12"))));
  await assertFails(getDoc(doc(as("chi"), "estates/E1/flatLevies/F1/flats/B12_1")));
  await assertFails(getDoc(doc(as("bayo"), "estates/E1/flatLevies/F1/proofs/g1")));
  await assertSucceeds(getDoc(doc(as("ada"), "estates/E1/flatLevies/F1/proofs/g1")));
  await assertSucceeds(getDoc(doc(as("treasurer"), "estates/E1/flatLevies/F1/proofs/g1")));
});

test("the treasurer confirms a proof, logging it and marking the levy as paid into", async () => {
  await assertSucceeds(sendProof(as("ada"), [1, 3]));
  const db = as("treasurer");
  const now = Date.now();
  const confirm = (withLog, withFlag) => {
    const batch = writeBatch(db);
    for (const flat of [1, 3]) {
      batch.set(doc(db, `estates/E1/flatLevies/F1/flats/B12_${flat}`), {
        buildingId: "B12", buildingName: "No. 12", flat, levyId: "F1", amount: 2000, status: "paid",
        payerId: "ada", payerName: "Ada", groupId: "g1", submittedAt: now, reviewedAt: now, confirmedByName: "T",
      });
    }
    if (withFlag) batch.update(doc(db, "estates/E1/flatLevies/F1"), { hasPayments: true });
    if (withLog) {
      batch.set(doc(db, `estates/E1/financeLog/flat_F1_g1_${now}`), {
        action: "payment_confirmed", summary: "x", amount: 4000, by: "treasurer", byName: "T", at: now,
      });
    }
    return batch.commit();
  };
  await assertFails(confirm(false, true));
  await assertFails(confirm(true, false));
  await assertSucceeds(confirm(true, true));
  // Paid into, so it stays.
  const del = writeBatch(db);
  del.delete(doc(db, "estates/E1/flatLevies/F1"));
  del.set(doc(db, "estates/E1/financeLog/levy_F1_deleted"), { action: "levy_deleted", summary: "x", by: "treasurer", at: Date.now() });
  await assertFails(del.commit());
});

test("a per-flat levy is created and deleted with its log entry", async () => {
  const db = as("treasurer");
  const levy = { title: "Gate", amount: 1000, dueDate: 0, note: "", createdBy: "treasurer", createdAt: Date.now(), hasPayments: false };
  await assertFails(setDoc(doc(db, "estates/E1/flatLevies/F2"), levy));
  const batch = writeBatch(db);
  batch.set(doc(db, "estates/E1/flatLevies/F2"), levy);
  batch.set(doc(db, "estates/E1/financeLog/levy_F2_created"), { action: "levy_created", summary: "Gate", amount: 1000, by: "treasurer", at: Date.now() });
  await assertSucceeds(batch.commit());
});

test("anyone can read the app config, even signed out", async () => {
  await assertSucceeds(getDoc(doc(env.unauthenticatedContext().firestore(), "config/app")));
  await assertFails(setDoc(doc(as("admin"), "config/app"), { minVersion: "0" }));
});

test("an admin removes a member onto the removed list, clearing their flat (as 1.07 does it)", async () => {
  const db = as("admin");
  const batch = writeBatch(db);
  batch.update(doc(db, "users/ada"), { estateId: null });
  batch.update(doc(db, "estates/E1"), { memberCount: increment(-1) });
  batch.delete(doc(db, "joinRequests/E1_ada"));
  batch.delete(doc(db, "estates/E1/residents/ada"));
  batch.set(doc(db, "estates/E1/removed/ada"), { name: "Ada", removedBy: "admin", at: Date.now() });
  await assertSucceeds(batch.commit());
});

test("approving a removed member lets them back in (1.07), and 1.06's approval still works", async () => {
  await env.withSecurityRulesDisabled((ctx) =>
    setDoc(doc(ctx.firestore(), "joinRequests/E1_kicked"), { userId: "kicked", estateId: "E1", status: "pending" }));
  const db = as("admin");
  const batch = writeBatch(db);
  batch.update(doc(db, "joinRequests/E1_kicked"), { status: "approved", reviewedAt: Date.now() });
  batch.update(doc(db, "users/kicked"), { estateId: "E1" });
  batch.update(doc(db, "estates/E1"), { memberCount: increment(1) });
  batch.delete(doc(db, "estates/E1/removed/kicked"));
  await assertSucceeds(batch.commit());

  await env.withSecurityRulesDisabled((ctx) =>
    setDoc(doc(ctx.firestore(), "joinRequests/E1_stranger"), { userId: "stranger", estateId: "E1", status: "pending" }));
  const old = writeBatch(db);
  old.update(doc(db, "joinRequests/E1_stranger"), { status: "approved", reviewedAt: Date.now() });
  old.update(doc(db, "users/stranger"), { estateId: "E1" });
  old.update(doc(db, "estates/E1"), { memberCount: increment(1) });
  await assertSucceeds(old.commit());
});
