#!/usr/bin/env node
/*
 * Sets up the App Review demo: a reviewer account already inside a demo estate, three neighbours,
 * and a few posts, a levy, a poll, expenses and a chat, so a reviewer sees a lived-in estate.
 *
 * Run from the functions folder with a service account key for localhive-4a645
 * (Firebase console > Project settings > Service accounts > Generate new private key):
 *
 *   GOOGLE_APPLICATION_CREDENTIALS=~/keys/localhive-admin.json node scripts/seed-demo.js
 *
 * Safe to run again: it rebuilds the same documents and keeps the reviewer's password unless
 * DEMO_PASSWORD is set. Remove everything with --delete.
 */
const crypto = require("crypto");
const admin = require("firebase-admin");

admin.initializeApp({ projectId: "localhive-4a645" });
const auth = admin.auth();
const db = admin.firestore();

const ESTATE_ID = "demo-estate";
const INVITE_CODE = "DEMO26";
const REVIEWER = {
  uid: "demo-reviewer",
  email: "appreview@example.com",
  displayName: "Alex Reviewer",
  username: "alexreviewer",
  address: "House 1, Palm Close",
  occupation: "Product tester",
  bio: "Here to try out LocalHive.",
};
const NEIGHBOURS = [
  { uid: "demo-ada", email: "ada.demo@example.com", displayName: "Ada Okafor", username: "adaokafor",
    address: "House 4, Palm Close", occupation: "Pharmacist", bio: "Mum of two, happy to help." },
  { uid: "demo-tunde", email: "tunde.demo@example.com", displayName: "Tunde Bello", username: "tundebello",
    address: "House 9, Palm Close", occupation: "Electrical engineer", bio: "Ask me about the transformer." },
  { uid: "demo-ngozi", email: "ngozi.demo@example.com", displayName: "Ngozi Eze", username: "ngozieze",
    address: "House 12, Cedar Avenue", occupation: "Caterer", bio: "Small chops for every occasion." },
];
const EVERYONE = [REVIEWER, ...NEIGHBOURS];
const [ADA, TUNDE, NGOZI] = NEIGHBOURS;

const now = Date.now();
const HOUR = 60 * 60 * 1000;
const DAY = 24 * HOUR;
const ago = (ms) => now - ms;

async function ensureAuthUser(person, password) {
  try {
    await auth.getUser(person.uid);
    const update = { email: person.email, displayName: person.displayName, emailVerified: true };
    if (password) update.password = password;
    await auth.updateUser(person.uid, update);
    return false;
  } catch (e) {
    if (e.code !== "auth/user-not-found") throw e;
    await auth.createUser({
      uid: person.uid,
      email: person.email,
      displayName: person.displayName,
      emailVerified: true,
      // Neighbours never sign in, so they get a password nobody knows.
      password: password || crypto.randomBytes(24).toString("base64url"),
    });
    return true;
  }
}

async function seed() {
  let password = process.env.DEMO_PASSWORD;
  const reviewerIsNew = await ensureAuthUser(REVIEWER, null);
  if (reviewerIsNew || password) {
    password = password || "Hive-" + crypto.randomBytes(6).toString("base64url");
    await auth.updateUser(REVIEWER.uid, { password });
  }
  for (const n of NEIGHBOURS) await ensureAuthUser(n, null);

  const batch = db.batch();
  const estate = db.collection("estates").doc(ESTATE_ID);

  for (const p of EVERYONE) {
    const { uid, ...profile } = p;
    batch.set(db.collection("users").doc(uid), { ...profile, estateId: ESTATE_ID }, { merge: true });
  }

  // The reviewer owns the estate and keeps its money, so every admin and treasurer screen opens.
  batch.set(estate, {
    name: "Palm Gardens Demo Estate",
    nameLower: "palm gardens demo estate",
    address: "Palm Close, Lekki, Lagos",
    adminIds: [REVIEWER.uid],
    ownerId: REVIEWER.uid,
    treasurerId: REVIEWER.uid,
    memberCount: EVERYONE.length,
    createdAt: ago(30 * DAY),
    joinMode: "both",
  });
  batch.set(estate.collection("private").doc("invite"), { code: INVITE_CODE });
  batch.set(db.collection("inviteCodes").doc(INVITE_CODE), { estateId: ESTATE_ID });
  batch.set(estate.collection("private").doc("bank"), {
    bankName: "Demo Bank",
    accountNumber: "0123456789",
    accountName: "Palm Gardens Residents Association",
    updatedBy: REVIEWER.uid,
    updatedByName: REVIEWER.displayName,
    updatedAt: ago(20 * DAY),
  });

  // Posts, newest last. Each gets a fixed id so a rerun replaces it rather than adding another.
  const posts = [
    { id: "demo-post-welcome", author: REVIEWER, category: "announcement", at: ago(6 * DAY),
      content: "Welcome to Palm Gardens on LocalHive! Use this feed for estate news, the Finance tab for dues, and Polls for decisions. Please be kind to your neighbours.",
      likedBy: [ADA.uid, TUNDE.uid, NGOZI.uid] },
    { id: "demo-post-security", author: TUNDE, category: "security", at: ago(4 * DAY),
      location: "Back gate, Cedar Avenue", incidentStatus: "resolved",
      statusUpdatedByName: REVIEWER.displayName, statusUpdatedAt: ago(3 * DAY),
      content: "The back gate was left open overnight. Security, please check it is locked after 10pm.",
      likedBy: [ADA.uid] },
    { id: "demo-post-event", author: ADA, category: "event", at: ago(2 * DAY),
      content: "Residents' meeting this Saturday at 4pm by the playground. We'll talk about the new streetlights and the security levy. Light refreshments provided!",
      likedBy: [REVIEWER.uid, NGOZI.uid] },
    { id: "demo-post-sale", author: NGOZI, category: "for_sale", at: ago(30 * HOUR),
      content: "Taking orders for small chops and jollof trays for weekend parties. Message me for prices.",
      likedBy: [ADA.uid] },
    { id: "demo-post-question", author: ADA, category: "question", at: ago(5 * HOUR),
      content: "Does anyone have a reliable plumber they can recommend? My kitchen tap won't stop dripping.",
      likedBy: [] },
  ];
  const comments = {
    "demo-post-welcome": [
      { id: "demo-c1", author: ADA, at: ago(6 * DAY - 2 * HOUR), content: "Great to have this, thank you!" },
    ],
    "demo-post-event": [
      { id: "demo-c2", author: TUNDE, at: ago(2 * DAY - 3 * HOUR), content: "I'll be there. I can bring quotes for the streetlights." },
      { id: "demo-c3", author: REVIEWER, at: ago(2 * DAY - 5 * HOUR), content: "Thanks Tunde, that will help a lot." },
    ],
    "demo-post-question": [
      { id: "demo-c4", author: TUNDE, at: ago(3 * HOUR), content: "Mr Femi did ours last month. I'll send you his number." },
    ],
  };
  for (const p of posts) {
    const doc = {
      authorId: p.author.uid, authorName: p.author.displayName, authorImageUrl: null,
      estateId: ESTATE_ID, content: p.content, imageUrl: null, videoUrl: null, videoThumbnailUrl: null,
      createdAt: p.at, likeCount: p.likedBy.length, likedBy: p.likedBy,
      commentCount: (comments[p.id] || []).length, category: p.category, mentions: {},
    };
    if (p.category === "security") {
      Object.assign(doc, {
        location: p.location, emergencyType: "", incidentStatus: p.incidentStatus,
        statusUpdatedBy: REVIEWER.uid, statusUpdatedByName: p.statusUpdatedByName,
        statusUpdatedAt: p.statusUpdatedAt,
      });
    }
    const ref = db.collection("posts").doc(p.id);
    batch.set(ref, doc);
    for (const c of comments[p.id] || []) {
      batch.set(ref.collection("comments").doc(c.id), {
        postId: p.id, authorId: c.author.uid, authorName: c.author.displayName,
        content: c.content, createdAt: c.at, mentions: {},
      });
    }
  }

  // A levy, with one neighbour paid, one waiting for the treasurer, and one not yet paid.
  const levy = estate.collection("levies").doc("demo-levy-security");
  batch.set(levy, {
    title: "Security levy - November", amount: 15000, dueDate: now + 14 * DAY,
    note: "Covers the night guards and gate repairs.", createdBy: REVIEWER.uid, createdAt: ago(5 * DAY),
  });
  batch.set(levy.collection("payments").doc(ADA.uid), {
    userId: ADA.uid, userName: ADA.displayName, levyId: levy.id, amount: 15000, status: "paid",
    proofUrl: null, submittedAt: ago(4 * DAY), reviewedAt: ago(3 * DAY), confirmedByName: REVIEWER.displayName,
  });
  batch.set(levy.collection("payments").doc(TUNDE.uid), {
    userId: TUNDE.uid, userName: TUNDE.displayName, levyId: levy.id, amount: 15000, status: "pending",
    proofUrl: null, submittedAt: ago(1 * DAY),
  });

  batch.set(estate.collection("expenses").doc("demo-expense-gate"), {
    title: "Back gate lock replaced", amount: 8500, spentAt: ago(3 * DAY),
    note: "New padlock and hinge.", receiptUrl: null, createdBy: REVIEWER.uid, createdAt: ago(3 * DAY),
  });
  batch.set(estate.collection("expenses").doc("demo-expense-guards"), {
    title: "Night guards, first half of November", amount: 40000, spentAt: ago(1 * DAY),
    note: "", receiptUrl: null, createdBy: REVIEWER.uid, createdAt: ago(1 * DAY),
  });

  // An open poll the neighbours have voted on; the reviewer hasn't, so they can try voting.
  const poll = estate.collection("polls").doc("demo-poll-streetlights");
  const votes = { [ADA.uid]: 0, [TUNDE.uid]: 0, [NGOZI.uid]: 1 };
  const counts = {};
  for (const option of Object.values(votes)) counts[option] = (counts[option] || 0) + 1;
  batch.set(poll, {
    question: "Should we install solar streetlights on Palm Close?",
    options: ["Yes, this quarter", "Yes, but next year", "No"],
    createdBy: REVIEWER.uid, createdByName: REVIEWER.displayName, createdAt: ago(2 * DAY),
    closesAt: 0, closed: false, counts, voteCount: Object.keys(votes).length,
  });
  for (const [uid, option] of Object.entries(votes)) {
    batch.set(estate.collection("pollVotes").doc(`${poll.id}_${uid}`), {
      pollId: poll.id, userId: uid, option, at: ago(DAY),
    });
  }

  batch.set(estate.collection("utilities").doc("power"), {
    isOn: true, since: ago(3 * HOUR), updatedAt: ago(3 * HOUR),
    updatedBy: TUNDE.uid, updatedByName: TUNDE.displayName,
  });

  // A conversation with a neighbour, waiting on the reviewer to read it.
  const chatId = [REVIEWER.uid, ADA.uid].sort().join("_");
  const chat = db.collection("chats").doc(chatId);
  const messages = [
    { id: "demo-m1", from: ADA, at: ago(26 * HOUR), content: "Hi Alex, thanks for setting up the estate group!" },
    { id: "demo-m2", from: REVIEWER, at: ago(25 * HOUR), content: "My pleasure. Let me know if anything isn't working." },
    { id: "demo-m3", from: ADA, at: ago(2 * HOUR), content: "Will you be at the meeting on Saturday?" },
  ];
  const last = messages[messages.length - 1];
  batch.set(chat, {
    participants: [REVIEWER.uid, ADA.uid],
    participantNames: { [REVIEWER.uid]: REVIEWER.displayName, [ADA.uid]: ADA.displayName },
    lastMessage: last.content, lastMessageTime: last.at, lastMessageSenderId: last.from.uid,
    unreadCounts: { [REVIEWER.uid]: 1, [ADA.uid]: 0 },
    readUpTo: { [ADA.uid]: last.at, [REVIEWER.uid]: messages[1].at },
    typing: { [ADA.uid]: 0, [REVIEWER.uid]: 0 },
  });
  for (const m of messages) {
    batch.set(chat.collection("messages").doc(m.id), {
      chatId, senderId: m.from.uid, content: m.content, timestamp: m.at,
    });
  }

  await batch.commit();

  console.log("Demo estate ready: Palm Gardens Demo Estate (invite code " + INVITE_CODE + ")");
  console.log("App Review sign-in:");
  console.log("  Email:    " + REVIEWER.email);
  console.log("  Password: " + (password || "(unchanged from the last run; set DEMO_PASSWORD to reset it)"));
}

async function remove() {
  await db.recursiveDelete(db.collection("estates").doc(ESTATE_ID));
  await db.recursiveDelete(db.collection("chats").doc([REVIEWER.uid, ADA.uid].sort().join("_")));
  const posts = await db.collection("posts").where("estateId", "==", ESTATE_ID).get();
  for (const p of posts.docs) await db.recursiveDelete(p.ref);
  await db.collection("inviteCodes").doc(INVITE_CODE).delete();
  for (const p of EVERYONE) {
    await db.recursiveDelete(db.collection("users").doc(p.uid));
    await auth.deleteUser(p.uid).catch((e) => { if (e.code !== "auth/user-not-found") throw e; });
  }
  console.log("Demo estate and accounts removed.");
}

(process.argv.includes("--delete") ? remove() : seed()).catch((e) => {
  console.error(e.message || e);
  process.exit(1);
});
