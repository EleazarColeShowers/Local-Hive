/**
 * GET /terms: LocalHive's Terms of Use, which people agree to when they sign up. Apple requires
 * apps with user content to have users accept terms with zero tolerance for objectionable content.
 * Keep it true to what the app actually does.
 */
export function termsPage() {
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
const UPDATED = "6 November 2026";

const PAGE = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>LocalHive Terms of Use</title>
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
  .callout { background: var(--cream); border-radius: 12px; padding: 12px 16px; }
</style>
</head>
<body>
<main>
  <h1>LocalHive Terms of Use</h1>
  <p class="updated">Last updated ${UPDATED}</p>

  <p>These terms apply to everyone who uses LocalHive, the app for residents of an estate to share news and
    alerts, chat with neighbours, buy and sell locally, vote in polls and keep track of estate dues. By creating
    an account you agree to them. If you don't agree, please don't use LocalHive.</p>

  <h2>Who can use LocalHive</h2>
  <ul>
    <li>You must be <strong>18 or older</strong>.</li>
    <li>Give your real name and keep your account details accurate. Don't pretend to be someone else.</li>
    <li>You're responsible for what happens on your account, so keep your password to yourself.</li>
  </ul>

  <h2>Zero tolerance for objectionable content and abuse</h2>
  <p>LocalHive has <strong>no tolerance</strong> for objectionable content or abusive users. Anywhere in the app,
    including posts, comments, photos, videos, profiles and private messages, you must not:</p>
  <ul>
    <li>share, request or promote child sexual abuse material, or sexualise, groom or exploit children in any way
      (see our <a href="/child-safety">Child Safety Standards</a>);</li>
    <li>harass, bully, threaten, intimidate or stalk anyone;</li>
    <li>post hate speech, or attack people for their race, ethnicity, religion, gender, sexuality, disability or
      any similar trait;</li>
    <li>share pornography, nudity or sexual content;</li>
    <li>encourage violence, self-harm or crime, or share graphic violence;</li>
    <li>scam or defraud anyone, or sell anything illegal, stolen, counterfeit or dangerous, including weapons and drugs;</li>
    <li>share other people's private information, photos or messages without their permission;</li>
    <li>spam, or post things you know to be false, including false emergency or security alerts;</li>
    <li>break the law, or anyone else's rights, including copyright.</li>
  </ul>

  <h2>Reporting and what we do about it</h2>
  <ul>
    <li>Report anything that breaks these terms from the ⋮ menu on a post, a profile or a chat, or by
      long-pressing a comment. You can also block anyone, and use <em>Settings &gt; Send feedback</em>.</li>
    <li>We review reports within 24 hours. We remove content that breaks these terms and suspend or permanently
      remove the accounts responsible, without notice.</li>
    <li>Estate admins can also remove posts and comments in their estate.</li>
    <li>We report child sexual abuse material and other serious crimes to the authorities.</li>
  </ul>

  <h2>Your content</h2>
  <p>You own what you post. By posting it, you let LocalHive store it and show it to the people it's meant for,
    usually members of your estate or the person you're messaging, for as long as it's on LocalHive. You're
    responsible for what you post, and must have the right to share it.</p>

  <h2>Emergencies and security alerts</h2>
  <p class="callout"><strong>LocalHive is not an emergency service.</strong> An SOS or security alert tells your
    neighbours, but notifications can be delayed or fail to arrive. If someone is in danger, call the police or
    emergency services first. Only send an SOS in a real emergency.</p>

  <h2>Buying, selling and estate dues</h2>
  <ul>
    <li>Sales, services and deals between members are between those members. LocalHive isn't part of them and
      doesn't check what's offered, so meet safely and take care before paying anyone.</li>
    <li>LocalHive doesn't take payments. Dues, levies and receipts in the app are records kept by your estate;
      money is paid outside the app, and your estate's admins and treasurer are responsible for it.</li>
  </ul>

  <h2>Ending your account</h2>
  <p>You can delete your account at any time in <em>Settings</em>, or on the web at
    <a href="/delete-account">/delete-account</a>. We may suspend or remove accounts that break these terms.</p>

  <h2>The service</h2>
  <p>We work hard to keep LocalHive running, but it's provided as it is, and we can't promise it will always be
    available or free of errors. We may change or stop features. To the extent the law allows, LocalHive isn't
    liable for loss caused by other users, by content they post, or by the app being unavailable.</p>

  <h2>Changes to these terms</h2>
  <p>We may update these terms. If we make important changes we'll tell you in the app. Using LocalHive after a
    change means you accept the new terms.</p>

  <h2>Privacy and contact</h2>
  <p>How we handle your information is set out in our <a href="/privacy">Privacy Policy</a>. Questions about these
    terms: <a href="mailto:${CONTACT}">${CONTACT}</a></p>
</main>
</body>
</html>`;
