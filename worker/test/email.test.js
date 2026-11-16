import { test } from "node:test";
import assert from "node:assert/strict";
import { buildWelcomeEmail } from "../src/email.js";

const docs = {
  "estates/E1": { name: "Amali <Estate>", adminIds: ["admin"] },
  "users/admin": { estateId: "E1", email: "admin@x.com", displayName: "Eleazar Cole" },
  "users/member": { estateId: "E1", email: "member@x.com", username: "ada" },
  "users/pending": { email: "pending@x.com" },
  "users/other": { estateId: "E2", email: "other@x.com" },
};
const db = { get: async (path) => docs[path] ?? null };

test("account welcome goes to the caller, addressed by first name", async () => {
  const email = await buildWelcomeEmail(db, "admin", { type: "account" });
  assert.equal(email.to, "admin@x.com");
  assert.equal(email.key, "welcome_admin");
  assert.match(email.html, /Welcome, Eleazar!/);
});

test("estate welcome for a member who joined", async () => {
  const email = await buildWelcomeEmail(db, "member", { type: "estate", estateId: "E1" });
  assert.equal(email.to, "member@x.com");
  assert.equal(email.key, "welcome_E1_member");
  assert.match(email.subject, /Welcome to Amali <Estate>/);
  // Names are escaped in the HTML.
  assert.match(email.html, /Amali &lt;Estate&gt;/);
});

test("estate creator gets the admin version", async () => {
  const email = await buildWelcomeEmail(db, "admin", { type: "estate", estateId: "E1" });
  assert.match(email.subject, /is live on LocalHive/);
});

test("an admin can trigger it for the member they approved", async () => {
  const email = await buildWelcomeEmail(db, "admin", { type: "estate", estateId: "E1", userId: "member" });
  assert.equal(email.to, "member@x.com");
});

test("nobody else can trigger it for someone", async () => {
  await assert.rejects(buildWelcomeEmail(db, "other", { type: "estate", estateId: "E1", userId: "member" }), /Not your estate/);
});

test("nothing is sent before they're in the estate", async () => {
  assert.equal(await buildWelcomeEmail(db, "pending", { type: "estate", estateId: "E1" }), null);
});

test("unknown types are refused", async () => {
  await assert.rejects(buildWelcomeEmail(db, "admin", { type: "spam" }), /Unknown email type/);
});
