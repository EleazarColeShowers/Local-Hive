import { HttpError } from "./google.js";

/**
 * Welcome emails, sent through Resend (https://resend.com, free for 3,000 a month).
 *
 *   { type: "account" }                         -> "Welcome to LocalHive", to the caller
 *   { type: "estate", estateId, userId? }       -> "Welcome to <estate>", to userId (default: the caller)
 *
 * Each is sent at most once per person (per estate), however often the app asks. Until the
 * RESEND_API_KEY secret is set, nothing is sent and the app carries on as normal.
 */

const BRAND = "#1E7D3A";
const HONEY = "#E09E00";
const CREAM = "#FAF3E1";
const INK = "#2C2C2C";

/** Works out who to email and with what, or returns null when there's nothing to send. */
export async function buildWelcomeEmail(db, uid, body) {
  switch (body.type) {
    case "account": {
      const user = await db.get(`users/${uid}`);
      if (!user?.email) return null;
      return { key: `welcome_${uid}`, to: user.email, ...accountWelcome(firstName(user)) };
    }
    case "estate": {
      const { estateId } = body;
      const userId = body.userId || uid;
      if (!isId(estateId) || !isId(userId)) throw new HttpError(400, "Bad estate reference");
      const [estate, user] = await Promise.all([db.get(`estates/${estateId}`), db.get(`users/${userId}`)]);
      if (!estate || !user) throw new HttpError(404, "No such estate or member");
      const admins = estate.adminIds || [];
      // Someone joining themselves, or an admin who just let them in.
      if (userId !== uid && !admins.includes(uid)) throw new HttpError(403, "Not your estate");
      // Only once they're actually in it.
      if (user.estateId !== estateId || !user.email) return null;
      const content = admins.includes(userId)
        ? estateCreated(firstName(user), estate.name || "your estate")
        : estateWelcome(firstName(user), estate.name || "your estate");
      return { key: `welcome_${estateId}_${userId}`, to: user.email, ...content };
    }
    default:
      throw new HttpError(400, "Unknown email type");
  }
}

/** Sends [email] once. Returns true if it went out now. */
export async function sendWelcomeEmail(db, env, email) {
  if (!env.RESEND_API_KEY) return false;
  if (!(await db.createOnce(`emails/${email.key}`, { to: email.to, at: Date.now() }))) return false;
  const response = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: { Authorization: `Bearer ${env.RESEND_API_KEY}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      from: env.EMAIL_FROM,
      to: [email.to],
      subject: email.subject,
      html: email.html,
      text: email.text,
    }),
  });
  if (!response.ok) {
    // Let a later attempt try again rather than lose the email.
    await db.commit([{ delete: `emails/${email.key}` }]).catch(() => {});
    throw new Error(`Resend ${email.key}: ${response.status} ${await response.text()}`);
  }
  console.log(`email ${email.key} sent`);
  return true;
}

function accountWelcome(name) {
  const subject = "Welcome to LocalHive 🐝";
  const intro = "Thanks for joining LocalHive, the app that keeps you connected to your neighbours and on top of what's happening in your estate.";
  const points = [
    ["Join your estate", "Use the invite code from your estate admin, or search for your estate and ask to join."],
    ["Stay in the loop", "Announcements, security alerts, polls and dues, all in one place."],
    ["Get help fast", "One tap on SOS alerts your neighbours and estate security."],
  ];
  const outro = "Not in an estate on LocalHive yet? You can create one for your community in a couple of minutes.";
  return render({ subject, heading: `Welcome, ${name}!`, intro, points, outro });
}

function estateWelcome(name, estate) {
  const subject = `Welcome to ${estate} on LocalHive`;
  const intro = `You're now a member of ${estate}. Here's what you can do with your neighbours:`;
  const points = [
    ["Read the feed", "See announcements from your estate admins and posts from neighbours."],
    ["Vote in polls", "Have your say on decisions for the estate."],
    ["Pay dues", "See what you owe and upload your payment receipts."],
    ["Register visitors", "Give guests a gate pass so security knows to expect them."],
  ];
  const outro = "Say hello to your neighbours with your first post. We're glad you're here.";
  return render({ subject, heading: `Welcome to ${estate}, ${name}!`, intro, points, outro });
}

function estateCreated(name, estate) {
  const subject = `${estate} is live on LocalHive`;
  const intro = `You've set up ${estate} and you're its admin. A few things to get your neighbours on board:`;
  const points = [
    ["Invite residents", "Share your estate's invite link or code from Estate admin."],
    ["Approve join requests", "Neighbours who search for the estate will wait for your OK."],
    ["Add your security team", "Guards can check visitor passes at the gate."],
    ["Set up dues", "Add levies and your bank details so residents can pay and upload receipts."],
  ];
  const outro = "Your first announcement is a great way to welcome everyone in.";
  return render({ subject, heading: `${estate} is ready, ${name}!`, intro, points, outro });
}

function render({ subject, heading, intro, points, outro }) {
  const list = points
    .map(([title, text]) => `
      <tr><td style="padding:0 0 14px 0;">
        <div style="font-weight:700;color:${BRAND};font-size:15px;">${escape(title)}</div>
        <div style="color:${INK};font-size:15px;line-height:1.5;">${escape(text)}</div>
      </td></tr>`)
    .join("");
  const html = `<!doctype html>
<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${escape(subject)}</title></head>
<body style="margin:0;padding:0;background:${CREAM};font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:${CREAM};padding:24px 12px;">
    <tr><td align="center">
      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#ffffff;border-radius:16px;overflow:hidden;">
        <tr><td style="background:${BRAND};padding:20px 28px;color:#ffffff;font-size:20px;font-weight:800;">
          Local<span style="color:${HONEY};">Hive</span>
        </td></tr>
        <tr><td style="padding:28px;">
          <h1 style="margin:0 0 12px 0;font-size:22px;color:${INK};">${escape(heading)}</h1>
          <p style="margin:0 0 22px 0;color:${INK};font-size:15px;line-height:1.5;">${escape(intro)}</p>
          <table role="presentation" width="100%" cellpadding="0" cellspacing="0">${list}</table>
          <p style="margin:8px 0 0 0;color:${INK};font-size:15px;line-height:1.5;">${escape(outro)}</p>
          <p style="margin:22px 0 0 0;color:${INK};font-size:15px;">The LocalHive team</p>
        </td></tr>
      </table>
      <p style="color:#8a8a8a;font-size:12px;margin:16px 0 0 0;">You're getting this because you signed up for LocalHive.</p>
    </td></tr>
  </table>
</body></html>`;
  const text = [heading, "", intro, "", ...points.map(([t, d]) => `- ${t}: ${d}`), "", outro, "", "The LocalHive team"].join("\n");
  return { subject, html, text };
}

function firstName(user) {
  const name = String(user.displayName || user.username || "").trim();
  return name ? name.split(/\s+/)[0] : "neighbour";
}

function escape(s) {
  return String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
}

function isId(s) {
  return typeof s === "string" && /^[A-Za-z0-9_-]{1,200}$/.test(s);
}
