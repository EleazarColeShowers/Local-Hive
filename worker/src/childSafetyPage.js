/**
 * GET /child-safety: LocalHive's standards against child sexual abuse and exploitation (CSAE),
 * the page Google Play asks social apps to publish. Keep it true to what the app actually does.
 */
export function childSafetyPage() {
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
const UPDATED = "4 November 2026";

const PAGE = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>LocalHive Child Safety Standards</title>
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
  .updated { margin: 0 0 20px; font-size: 14px; }
</style>
</head>
<body>
<main>
  <h1>LocalHive Child Safety Standards</h1>
  <p class="updated">Last updated ${UPDATED}</p>

  <p>LocalHive is an app for adult residents of an estate to share news and alerts, chat with neighbours,
    vote in polls and keep track of estate dues. We have zero tolerance for child sexual abuse and
    exploitation (CSAE), including child sexual abuse material (CSAM). This page sets out our standards
    and what we do when they are broken.</p>

  <h2>What is not allowed</h2>
  <p>Anywhere in LocalHive, including posts, comments, photos, videos, profiles and private messages, you
    must not:</p>
  <ul>
    <li>share, request or link to child sexual abuse material of any kind, including drawn, edited or
      AI-generated images;</li>
    <li>sexualise a child in any way, in words or images;</li>
    <li>groom a child, or contact a child to arrange sexual contact, online or in person;</li>
    <li>sextort, threaten or blackmail anyone using intimate images of a child;</li>
    <li>traffic a child, or advertise or arrange the sexual exploitation of a child.</li>
  </ul>
  <p>LocalHive is for adults. You must be 18 or older to use it.</p>

  <h2>How to report it</h2>
  <ul>
    <li><strong>Report it in the app.</strong> Tap ⋮ on a post, long-press a comment, or open the ⋮ menu
      on someone's profile or in a chat, then choose <em>Report</em> and
      <em>Child sexual abuse or exploitation</em>. You can also use <em>Settings &gt; Send feedback</em>.
      Reports come straight to us and the person isn't told who sent them.</li>
    <li><strong>Email us</strong> at <a href="mailto:${CONTACT}">${CONTACT}</a> with the estate's name,
      the person's name as it appears in the app, and what you saw. Please don't send us the image itself.</li>
    <li><strong>Tell your estate's admins</strong>, who can remove any post or comment in the estate straight away.</li>
    <li><strong>Block the person</strong> from their profile, so they can no longer message you.</li>
  </ul>
  <p>If a child is in immediate danger, contact your local police or emergency services first.</p>

  <h2>What we do</h2>
  <ul>
    <li>We review every CSAE report as our top priority, aiming to act within 24 hours.</li>
    <li>We remove the content and permanently ban the accounts involved.</li>
    <li>We report child sexual abuse material and exploitation to the National Center for Missing &amp;
      Exploited Children (NCMEC) and to law enforcement in the country concerned, as the law requires, and
      keep the evidence they need for that.</li>
    <li>We cooperate fully with law enforcement investigations.</li>
  </ul>

  <h2>Following the law</h2>
  <p>LocalHive complies with the child safety laws that apply to it, including laws requiring
    CSAM to be reported to the authorities.</p>

  <h2>Child safety contact</h2>
  <p>Our designated contact for child safety, including questions from Google Play and from law
    enforcement: <a href="mailto:${CONTACT}">${CONTACT}</a></p>
</main>
</body>
</html>`;
