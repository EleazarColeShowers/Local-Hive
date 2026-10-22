/**
 * GET /delete-account: the web page Google Play asks for, where someone can delete their LocalHive
 * account and data without the app. They sign in with their password right here (Firebase's
 * REST sign-in), and the page then calls POST /delete-account with that fresh token: the same
 * deletion, with the same checks, as Settings > Delete account in the app.
 */
export function deleteAccountPage(env) {
  return new Response(page(env.FIREBASE_WEB_API_KEY || ""), { headers: HEADERS });
}

const HEADERS = {
  "Content-Type": "text/html; charset=utf-8",
  "Cache-Control": "public, max-age=300",
  "Content-Security-Policy": "default-src 'none'; style-src 'unsafe-inline'; script-src 'unsafe-inline'; "
    + "connect-src 'self' https://identitytoolkit.googleapis.com; form-action 'none'; frame-ancestors 'none'",
  "X-Content-Type-Options": "nosniff",
  "Referrer-Policy": "no-referrer",
};

function page(apiKey) {
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Delete your LocalHive account</title>
<style>
  :root { --green: #1E7D3A; --red: #B3261E; --cream: #FAF3E1; --ink: #1D2A20; --muted: #5B665E; --card: #FFFFFF; --line: #D9DDD6; }
  @media (prefers-color-scheme: dark) {
    :root { --red: #F2B8B5; --cream: #121712; --ink: #EEF2EC; --muted: #A9B3AA; --card: #1C231D; --line: #3A443B; }
  }
  * { box-sizing: border-box; }
  body { margin: 0; min-height: 100vh; background: var(--cream); color: var(--ink);
    font: 16px/1.5 -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; padding: 24px 16px; }
  main { max-width: 520px; margin: 0 auto; background: var(--card); border-radius: 20px; padding: 28px 24px;
    box-shadow: 0 8px 30px rgba(0,0,0,.08); }
  h1 { font-size: 22px; margin: 0 0 8px; }
  h2 { font-size: 16px; margin: 24px 0 6px; }
  p, li { color: var(--muted); }
  p { margin: 0 0 12px; }
  ul { margin: 0 0 12px; padding-left: 20px; }
  label { display: block; font-weight: 600; margin: 14px 0 6px; }
  input { width: 100%; padding: 12px; border-radius: 10px; border: 1px solid var(--line); background: transparent;
    color: var(--ink); font: inherit; }
  .check { display: flex; gap: 10px; align-items: flex-start; font-weight: 400; color: var(--muted); }
  .check input { width: auto; margin-top: 4px; }
  button { width: 100%; padding: 14px; margin-top: 18px; border: 0; border-radius: 999px; background: var(--red);
    color: #fff; font: 600 16px/1 inherit; cursor: pointer; }
  button:disabled { opacity: .5; cursor: default; }
  button.link { background: none; color: var(--green); width: auto; padding: 8px 0; margin: 4px 0 0; font-weight: 600; }
  #status { margin-top: 16px; font-weight: 600; }
  #status.error { color: var(--red); }
  #status.ok { color: var(--green); }
</style>
</head>
<body>
<main>
  <h1>Delete your LocalHive account</h1>
  <p>You can delete your LocalHive account and its data here, or in the app under
    <strong>Profile &gt; Settings &gt; Delete account</strong>.</p>

  <h2>What gets deleted</h2>
  <ul>
    <li>Your profile: name, photo, bio, address, phone and occupation</li>
    <li>Your posts and alerts, with every comment on them</li>
    <li>Your comments and likes on other people's posts</li>
    <li>Your notifications, join requests and blocked list</li>
    <li>The photos, videos and receipts you uploaded</li>
    <li>Your sign-in (email and password)</li>
  </ul>

  <h2>What stays</h2>
  <ul>
    <li>Messages you sent stay in the other person's chat, shown as from "Deleted user"</li>
    <li>Your estate's records of dues you paid and poll votes you cast, which the estate needs for its accounts</li>
  </ul>
  <p>Deletion happens straight away and can't be undone. If you own an estate that has other members,
    make one of them an admin first.</p>

  <h2>Delete my account</h2>
  <form id="form" novalidate>
    <label for="email">Email</label>
    <input id="email" type="email" autocomplete="email" required>
    <label for="password">Password</label>
    <input id="password" type="password" autocomplete="current-password" required>
    <button type="button" class="link" id="forgot">Forgot your password?</button>
    <label class="check"><input id="confirm" type="checkbox">
      I understand my account and data will be permanently deleted.</label>
    <button type="submit" id="submit" disabled>Delete my account</button>
  </form>
  <div id="status" role="status"></div>
</main>
<script>
  const KEY = ${JSON.stringify(apiKey)};
  const $ = (id) => document.getElementById(id);
  const say = (text, kind) => { $("status").textContent = text; $("status").className = kind || ""; };
  $("confirm").addEventListener("change", () => { $("submit").disabled = !$("confirm").checked; });

  const SIGN_IN_ERRORS = {
    INVALID_LOGIN_CREDENTIALS: "That email and password don't match a LocalHive account.",
    INVALID_PASSWORD: "That email and password don't match a LocalHive account.",
    EMAIL_NOT_FOUND: "That email and password don't match a LocalHive account.",
    INVALID_EMAIL: "Enter a valid email address.",
    MISSING_PASSWORD: "Enter your password.",
    USER_DISABLED: "This account has been disabled.",
  };

  async function identity(method, body) {
    const res = await fetch("https://identitytoolkit.googleapis.com/v1/accounts:" + method + "?key=" + KEY, {
      method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body),
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      const code = String(data.error?.message || "").split(" ")[0];
      if (code.startsWith("TOO_MANY_ATTEMPTS")) throw new Error("Too many tries. Wait a few minutes and try again.");
      throw new Error(SIGN_IN_ERRORS[code] || "Something went wrong. Please try again.");
    }
    return data;
  }

  $("forgot").addEventListener("click", async () => {
    const email = $("email").value.trim();
    if (!email) return say("Enter your email above first.", "error");
    try {
      await identity("sendOobCode", { requestType: "PASSWORD_RESET", email });
    } catch (e) { /* Same answer either way, so this can't reveal who has an account. */ }
    say("If there's an account for " + email + ", we've emailed a link to reset its password.", "ok");
  });

  $("form").addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!$("confirm").checked) return;
    $("submit").disabled = true;
    say("Deleting your account…");
    try {
      const { idToken } = await identity("signInWithPassword", {
        email: $("email").value.trim(), password: $("password").value, returnSecureToken: true,
      });
      const res = await fetch("/delete-account", {
        method: "POST", headers: { Authorization: "Bearer " + idToken, "Content-Type": "application/json" }, body: "{}",
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.error || "Something went wrong. Please try again.");
      $("form").hidden = true;
      say("Your LocalHive account and data have been deleted.", "ok");
    } catch (e) {
      say(e.message, "error");
      $("submit").disabled = !$("confirm").checked;
    }
  });
</script>
</body>
</html>`;
}
