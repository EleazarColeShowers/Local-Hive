import { deleteAccountPage } from "./deletePage.js";

/**
 * The public, signed-out side of the Worker: invite links people tap in WhatsApp or SMS.
 *
 *   GET /join/{code}                   -> a page that opens the app, or the Play Store with the code
 *   GET /.well-known/assetlinks.json   -> lets Android open /join links straight in the app
 *   GET /delete-account                -> delete your account from the web (see deletePage.js)
 *
 * The page never looks the code up, so it can't be used to test which codes exist; the app
 * checks the code once the person is signed in.
 */
export function publicResponse(request, env) {
  const path = new URL(request.url).pathname;
  if (path === "/.well-known/assetlinks.json") return assetLinks(env);
  if (path === "/delete-account" || path === "/delete-account/") return deleteAccountPage(env);
  const code = inviteCodeIn(path);
  if (code) return new Response(joinPage(code, env.ANDROID_PACKAGE), { headers: PAGE_HEADERS });
  return null;
}

/** The invite code in a /join/{code} path, or null. Codes are short letters and digits. */
export function inviteCodeIn(path) {
  const match = /^\/join\/([A-Za-z0-9]{4,12})\/?$/.exec(path);
  return match ? match[1].toUpperCase() : null;
}

/**
 * The Play Store listing, carrying the code through the install so the app can open the join
 * screen on first launch (the Install Referrer API hands it back as "invite=CODE").
 */
export function playStoreUrl(pkg, code) {
  return `https://play.google.com/store/apps/details?id=${pkg}&referrer=${encodeURIComponent(`invite=${code}`)}`;
}

/**
 * Chrome's intent link: opens the app at the join screen if it's installed, or the Play Store
 * listing if not. Used when Android hasn't verified the app for these links (and so showed
 * this page instead of opening the app directly).
 */
export function appIntentUrl(pkg, code) {
  return `intent://join/${code}#Intent;scheme=localhive;package=${pkg};`
    + `S.browser_fallback_url=${encodeURIComponent(playStoreUrl(pkg, code))};end`;
}

function assetLinks(env) {
  const fingerprints = (env.ANDROID_CERT_SHA256 || "").split(",").map((f) => f.trim()).filter(Boolean);
  const body = fingerprints.length === 0 ? [] : [{
    relation: ["delegate_permission/common.handle_all_urls"],
    target: { namespace: "android_app", package_name: env.ANDROID_PACKAGE, sha256_cert_fingerprints: fingerprints },
  }];
  return new Response(JSON.stringify(body), {
    headers: { "Content-Type": "application/json", "Cache-Control": "public, max-age=3600" },
  });
}

const PAGE_HEADERS = {
  "Content-Type": "text/html; charset=utf-8",
  "Cache-Control": "public, max-age=300",
  "Content-Security-Policy": "default-src 'none'; style-src 'unsafe-inline'; script-src 'unsafe-inline'",
  "X-Content-Type-Options": "nosniff",
  "Referrer-Policy": "no-referrer",
};

export function joinPage(code, pkg) {
  const play = playStoreUrl(pkg, code);
  const app = appIntentUrl(pkg, code);
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Join your estate on LocalHive</title>
<meta property="og:title" content="You're invited to LocalHive">
<meta property="og:description" content="Join your estate's LocalHive with code ${code}.">
<style>
  :root { --green: #1E7D3A; --honey: #F4B400; --cream: #FAF3E1; --ink: #1D2A20; --muted: #5B665E; --card: #FFFFFF; }
  @media (prefers-color-scheme: dark) {
    :root { --cream: #121712; --ink: #EEF2EC; --muted: #A9B3AA; --card: #1C231D; }
  }
  * { box-sizing: border-box; }
  body { margin: 0; min-height: 100vh; background: var(--cream); color: var(--ink);
    font: 16px/1.5 -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    display: flex; align-items: center; justify-content: center; padding: 24px 16px; }
  main { width: 100%; max-width: 400px; background: var(--card); border-radius: 20px; padding: 32px 24px; text-align: center;
    box-shadow: 0 8px 30px rgba(0,0,0,.08); }
  h1 { font-size: 22px; margin: 0 0 8px; }
  p { margin: 0 0 20px; color: var(--muted); }
  .code { font: 700 28px/1 ui-monospace, Menlo, monospace; letter-spacing: 4px; padding: 14px; border-radius: 12px;
    border: 2px dashed var(--honey); margin: 0 0 24px; }
  a.button { display: block; padding: 14px; border-radius: 999px; background: var(--green); color: #fff; font-weight: 600;
    text-decoration: none; margin-bottom: 12px; }
  a.secondary { background: transparent; color: var(--green); border: 2px solid var(--green); }
  .other { display: none; } .ios .other { display: block; } .ios .android { display: none; }
  small { color: var(--muted); }
</style>
</head>
<body>
<main>
  <h1>You're invited to LocalHive</h1>
  <p>Your neighbours use LocalHive for estate news, alerts and dues. Join them with this invite code:</p>
  <div class="code">${code}</div>
  <div class="android">
    <a class="button" href="${app}">Open in LocalHive</a>
    <a class="button secondary" href="${play}">Get it on Google Play</a>
    <small>New to LocalHive? Install it, sign up, and you'll go straight to joining.</small>
  </div>
  <div class="other">
    <p>LocalHive is on Android for now; the iPhone app is coming soon.</p>
    <a class="button secondary" href="${play}">View on Google Play</a>
  </div>
</main>
<script>
  if (!/Android/i.test(navigator.userAgent)) document.body.classList.add("ios");
</script>
</body>
</html>`;
}
