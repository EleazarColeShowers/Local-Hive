#!/usr/bin/env node
/*
 * One-off, for 1.09: gives every profile in users/ a nameLower (the display name, or else the
 * username, trimmed and lowercased), which the directory searches by in big estates. 1.09 writes
 * it whenever a profile is saved; this covers profiles saved before that. Phones on older versions
 * don't keep it up to date when someone renames, so run it again once everyone has 1.09.
 *
 * Run from the functions folder with a service account key for localhive-4a645:
 *
 *   GOOGLE_APPLICATION_CREDENTIALS=~/keys/localhive-admin.json node scripts/fill-name-lower.js
 *
 * Add --dry-run to only count them. Safe to run again.
 */
const admin = require("firebase-admin");

admin.initializeApp({ projectId: "localhive-4a645" });
const db = admin.firestore();
const dryRun = process.argv.includes("--dry-run");

/** Same as User.nameLower() in the app. */
function nameLower(data) {
  const display = typeof data.displayName === "string" && data.displayName.trim() ? data.displayName : null;
  const name = display ?? (typeof data.username === "string" ? data.username : null);
  return name == null ? null : name.trim().toLowerCase();
}

async function main() {
  const changes = (await db.collection("users").get()).docs
    .map((d) => ({ ref: d.ref, current: d.get("nameLower"), wanted: nameLower(d.data()) }))
    .filter((c) => c.wanted != null && c.current !== c.wanted);
  console.log(`${changes.length} profiles need nameLower${dryRun ? "" : "; filling in"}.`);
  if (dryRun) return;
  for (let i = 0; i < changes.length; i += 400) {
    const batch = db.batch();
    changes.slice(i, i + 400).forEach((c) => batch.update(c.ref, { nameLower: c.wanted }));
    await batch.commit();
  }
  console.log("Done.");
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
