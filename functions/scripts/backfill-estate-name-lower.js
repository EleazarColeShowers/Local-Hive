/**
 * One-off: gives every estate a nameLower field, which estate search now runs on.
 * Estates without it don't appear in search, so run this once after the app update.
 *
 *   gcloud auth application-default login
 *   node functions/scripts/backfill-estate-name-lower.js          # dry run
 *   node functions/scripts/backfill-estate-name-lower.js --write  # apply
 *
 * Safe to run more than once: estates that already match are skipped.
 */
const admin = require("firebase-admin");

admin.initializeApp({ projectId: "localhive-4a645" });
const db = admin.firestore();
const write = process.argv.includes("--write");

async function main() {
  const snapshot = await db.collection("estates").get();
  // Matches the app and the rules: the stored name, lowercased.
  const updates = snapshot.docs
    .map((doc) => ({ ref: doc.ref, name: doc.get("name"), current: doc.get("nameLower") }))
    .filter(({ name, current }) => typeof name === "string" && current !== name.toLowerCase());

  console.log(`${snapshot.size} estates, ${updates.length} need nameLower`);
  updates.forEach(({ ref, name }) => console.log(`  ${ref.id}  ${name}`));
  if (!write) {
    console.log("Dry run. Pass --write to apply.");
    return;
  }

  // Batches hold at most 500 writes.
  for (let i = 0; i < updates.length; i += 500) {
    const batch = db.batch();
    updates.slice(i, i + 500).forEach(({ ref, name }) => batch.update(ref, { nameLower: name.toLowerCase() }));
    await batch.commit();
  }
  console.log(`Updated ${updates.length} estates`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
