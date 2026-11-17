#!/usr/bin/env node
/*
 * One-off, for 1.07: removes the email field from every profile in users/, since neighbours can
 * read profiles. Emails stay on the sign-in accounts, which is where the app and the Worker read
 * them from now. Phones on 1.06 put it back when they sign in, so run this again once
 * strictProfiles() is on in firestore.rules.
 *
 * Run from the functions folder with a service account key for localhive-4a645:
 *
 *   GOOGLE_APPLICATION_CREDENTIALS=~/keys/localhive-admin.json node scripts/strip-profile-emails.js
 *
 * Add --dry-run to only count them. Safe to run again.
 */
const admin = require("firebase-admin");

admin.initializeApp({ projectId: "localhive-4a645" });
const db = admin.firestore();
const dryRun = process.argv.includes("--dry-run");

async function main() {
  const withEmail = (await db.collection("users").get()).docs.filter((d) => d.get("email") !== undefined);
  console.log(`${withEmail.length} profiles carry an email${dryRun ? "" : "; removing"}.`);
  if (dryRun) return;
  for (let i = 0; i < withEmail.length; i += 400) {
    const batch = db.batch();
    withEmail.slice(i, i + 400).forEach((d) => batch.update(d.ref, { email: admin.firestore.FieldValue.delete() }));
    await batch.commit();
  }
  console.log("Done.");
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
