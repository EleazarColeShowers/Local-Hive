/**
 * GET /privacy: LocalHive's privacy policy, the page linked from Google Play. Keep it in step
 * with the Play Console Data safety form and with what account.js deletes.
 */
export function privacyPage() {
  return new Response(PAGE, { headers: HEADERS });
}

const HEADERS = {
  "Content-Type": "text/html; charset=utf-8",
  "Cache-Control": "public, max-age=3600",
  "Content-Security-Policy": "default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'",
  "X-Content-Type-Options": "nosniff",
  "Referrer-Policy": "no-referrer",
};

const CONTACT = "eleato2020@gmail.com";
const UPDATED = "22 October 2026";

const PAGE = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>LocalHive Privacy Policy</title>
<style>
  :root { --green: #1E7D3A; --cream: #FAF3E1; --ink: #1D2A20; --muted: #5B665E; --card: #FFFFFF; --line: #E4E7E1; }
  @media (prefers-color-scheme: dark) {
    :root { --green: #7BD38F; --cream: #121712; --ink: #EEF2EC; --muted: #A9B3AA; --card: #1C231D; --line: #2E372F; }
  }
  * { box-sizing: border-box; }
  body { margin: 0; background: var(--cream); color: var(--ink);
    font: 16px/1.6 -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; padding: 24px 16px; }
  main { max-width: 720px; margin: 0 auto; background: var(--card); border-radius: 20px; padding: 32px 24px;
    box-shadow: 0 8px 30px rgba(0,0,0,.08); }
  h1 { font-size: 26px; margin: 0 0 4px; }
  h2 { font-size: 18px; margin: 28px 0 8px; padding-top: 16px; border-top: 1px solid var(--line); }
  p, li { color: var(--muted); }
  strong { color: var(--ink); }
  ul { padding-left: 20px; }
  li { margin-bottom: 4px; }
  a { color: var(--green); }
  table { width: 100%; border-collapse: collapse; font-size: 15px; }
  th, td { text-align: left; vertical-align: top; padding: 8px 6px; border-bottom: 1px solid var(--line); }
  th { color: var(--ink); }
  td { color: var(--muted); }
  .updated { margin: 0 0 20px; font-size: 14px; }
</style>
</head>
<body>
<main>
  <h1>LocalHive Privacy Policy</h1>
  <p class="updated">Last updated ${UPDATED}</p>

  <p>LocalHive is an app for residents of an estate to share news and alerts, chat with neighbours,
    vote in polls and keep track of estate dues. This policy explains what information the app collects,
    why, who can see it, and how you can delete it. "We" and "us" mean the developer of LocalHive.</p>

  <h2>Information you give us</h2>
  <table>
    <tr><th>What</th><th>Why we need it</th></tr>
    <tr><td><strong>Name and email address</strong> (required)</td>
      <td>To create and sign in to your account, and to show neighbours who you are.</td></tr>
    <tr><td><strong>Address, phone number, occupation and bio</strong> (optional)</td>
      <td>Shown on your profile to members of your estate, and your address fills in the location of an emergency alert.</td></tr>
    <tr><td><strong>Posts, comments, alerts, polls and votes</strong></td>
      <td>To share them with your estate.</td></tr>
    <tr><td><strong>Chat messages</strong></td>
      <td>To deliver them to the neighbour you're talking to.</td></tr>
    <tr><td><strong>Photos and videos</strong></td>
      <td>Pictures and videos you attach to posts, your profile photo, and photos of payment receipts.</td></tr>
    <tr><td><strong>Financial information</strong></td>
      <td>Records of estate dues you pay and the receipts you upload as proof, and, if you're a treasurer,
        the estate's bank details and expenses. LocalHive doesn't take payments and never asks for your card details.</td></tr>
  </table>

  <h2>Information collected automatically</h2>
  <ul>
    <li><strong>A user ID</strong> created for your account.</li>
    <li><strong>Device identifiers:</strong> a push notification token so we can send you alerts and messages,
      and an app instance ID used by Firebase Analytics.</li>
    <li><strong>App interactions:</strong> which screens you open and features you use, through Firebase Analytics,
      so we can see what's used and improve the app.</li>
    <li><strong>Crash logs and diagnostics</strong>, so we can find and fix problems.</li>
    <li><strong>App integrity checks</strong> (Firebase App Check with Google Play Integrity), to block requests
      that don't come from the real app.</li>
  </ul>
  <p>LocalHive does not use your device's GPS or location, your contacts, calendar or microphone.
    Firebase Analytics may estimate your rough region from your IP address; it does not store the address itself.</p>

  <h2>Who can see your information</h2>
  <ul>
    <li><strong>Members of your estate</strong> can see your profile, your posts, comments and alerts.
      People outside your estate can't.</li>
    <li><strong>Chat messages</strong> are seen only by you and the person you're chatting with.</li>
    <li><strong>Your payment records and receipts</strong> are seen only by you and your estate's treasurer.</li>
    <li><strong>Your blocked list</strong> is private to you.</li>
    <li><strong>Estate admins</strong> can see join requests sent to their estate.</li>
  </ul>

  <h2>Service providers</h2>
  <p>We don't sell your information or share it for advertising. LocalHive has no ads. We use these services
    to run the app, and they handle data only on our behalf:</p>
  <ul>
    <li><strong>Google Firebase</strong> (Authentication, Cloud Firestore, Cloud Messaging, Analytics, App Check):
      accounts, app data, push notifications and analytics.
      <a href="https://firebase.google.com/support/privacy">Firebase privacy</a></li>
    <li><strong>Cloudinary:</strong> stores photos and videos.
      <a href="https://cloudinary.com/privacy">Cloudinary privacy</a></li>
    <li><strong>Cloudflare:</strong> runs our server, which sends notifications and handles uploads and account deletion.
      <a href="https://www.cloudflare.com/privacypolicy/">Cloudflare privacy</a></li>
  </ul>
  <p>We may disclose information if the law requires it.</p>

  <h2>Security</h2>
  <p>All data is encrypted in transit (HTTPS). Access to your data is limited by security rules on our servers,
    so, for example, only members of an estate can read its posts.</p>

  <h2>How long we keep it</h2>
  <p>We keep your information while you have an account. Videos over the app's size limit are deleted
    automatically. When you delete your account, your data is deleted straight away, as described below.</p>

  <h2>Deleting your account and data</h2>
  <p>You can delete your account in the app under <strong>Profile &gt; Settings &gt; Delete account</strong>,
    or on the web at <a href="/delete-account">localhive.localhhive.workers.dev/delete-account</a>.
    This deletes your profile, your posts and alerts (with all comments on them), your comments and likes,
    notifications, join requests, blocked list, uploaded photos, videos and receipts, and your sign-in.</p>
  <p>Messages you sent stay in the other person's chat, shown as from "Deleted user". Your estate keeps its
    records of dues you paid and poll votes you cast, which it needs for its accounts.</p>
  <p>You can also edit your profile at any time, and leave an estate from its settings.</p>

  <h2>Children</h2>
  <p>LocalHive is meant for adult residents and isn't directed at children under 13. If you believe a child
    has given us information, contact us and we'll delete it.</p>

  <h2>Changes to this policy</h2>
  <p>If we change this policy, we'll update the date at the top of this page.</p>

  <h2>Contact</h2>
  <p>Questions or requests about your data: <a href="mailto:${CONTACT}">${CONTACT}</a></p>
</main>
</body>
</html>`;
