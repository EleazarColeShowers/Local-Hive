// The rules with strictFinance() and strictProfiles() switched on, as they will be once everyone
// is on 1.07: what 1.07 writes still passes, and what only 1.06 writes is refused.
import { test, before, beforeEach, after } from "node:test";
import { readFileSync } from "node:fs";
import { assertFails, assertSucceeds, initializeTestEnvironment } from "@firebase/rules-unit-testing";
import { doc, increment, setDoc, writeBatch } from "firebase/firestore";

let env;

before(async () => {
  const rules = readFileSync(new URL("../../firestore.rules", import.meta.url), "utf8")
    .replace(/function strictFinance\(\) \{\s*return false;/, "function strictFinance() {\n        return true;")
    .replace(/function strictProfiles\(\) \{\s*return false;/, "function strictProfiles() {\n      return true;");
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
