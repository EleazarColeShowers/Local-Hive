#!/usr/bin/env node
/*
 * One-off, for the 1.07 rules: marks every levy that already has a confirmed payment with
 * hasPayments: true, so the rules stop it being deleted along with its payment records.
 * Levies created from 1.07 on get the mark when their first payment is confirmed.
 *
 * Run from the functions folder with a service account key for localhive-4a645
 * (Firebase console > Project settings > Service accounts > Generate new private key):
 *
 *   GOOGLE_APPLICATION_CREDENTIALS=~/keys/localhive-admin.json node scripts/mark-paid-levies.js
 *
 * Add --dry-run to only list what it would mark. Safe to run again.
 */
const admin = require("firebase-admin");

admin.initializeApp({ projectId: "localhive-4a645" });
const db = admin.firestore();
const dryRun = process.argv.includes("--dry-run");

async function main() {
  let marked = 0;
  for (const estate of (await db.collection("estates").get()).docs) {
    for (const levy of (await estate.ref.collection("levies").get()).docs) {
      if (levy.get("hasPayments") === true) continue;
      const paid = await levy.ref.collection("payments").where("status", "==", "paid").limit(1).get();
      if (paid.empty) continue;
      console.log(`${dryRun ? "Would mark" : "Marking"} ${estate.get("name") || estate.id} · ${levy.get("title") || levy.id}`);
      if (!dryRun) await levy.ref.update({ hasPayments: true });
      marked++;
    }
  }
  console.log(`${marked} ${marked === 1 ? "levy" : "levies"} ${dryRun ? "to mark" : "marked"}.`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
