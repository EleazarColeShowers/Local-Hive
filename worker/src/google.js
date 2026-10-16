import { createRemoteJWKSet, importPKCS8, jwtVerify, SignJWT } from "jose";

// Firebase ID tokens are signed with these rotating keys; jose caches and refreshes them.
const FIREBASE_JWKS = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com")
);

/**
 * Returns the caller's Firebase uid and when they last entered their password (seconds since
 * the epoch), or throws if the ID token isn't a valid one for [projectId].
 */
export async function verifyFirebaseUser(request, projectId) {
  const header = request.headers.get("Authorization") || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : null;
  if (!token) throw new HttpError(401, "Missing token");
  try {
    const { payload } = await jwtVerify(token, FIREBASE_JWKS, {
      issuer: `https://securetoken.google.com/${projectId}`,
      audience: projectId,
    });
    if (!payload.sub) throw new Error("no subject");
    return { uid: payload.sub, authTime: Number(payload.auth_time) || 0 };
  } catch {
    throw new HttpError(401, "Invalid token");
  }
}

// App Check tokens prove a request came from the genuine LocalHive app on a real device.
const APP_CHECK_JWKS = createRemoteJWKSet(new URL("https://firebaseappcheck.googleapis.com/v1/jwks"));

/**
 * Checks the request's App Check token. Only refuses requests once APP_CHECK_REQUIRED is "true",
 * so it can be switched on after every installed app sends one; until then it reports what it saw.
 */
export async function verifyAppCheck(request, env) {
  const required = env.APP_CHECK_REQUIRED === "true";
  const token = request.headers.get("X-Firebase-AppCheck");
  let valid = false;
  if (token && env.FIREBASE_PROJECT_NUMBER) {
    try {
      await jwtVerify(token, APP_CHECK_JWKS, {
        issuer: `https://firebaseappcheck.googleapis.com/${env.FIREBASE_PROJECT_NUMBER}`,
        audience: `projects/${env.FIREBASE_PROJECT_NUMBER}`,
        algorithms: ["RS256"],
      });
      valid = true;
    } catch {
      valid = false;
    }
  }
  if (required && !valid) throw new HttpError(401, "This request didn't come from the LocalHive app");
  return valid;
}

const SCOPES = [
  "https://www.googleapis.com/auth/datastore",
  "https://www.googleapis.com/auth/firebase.messaging",
  // Deleting the sign-in account itself, when a user deletes their account.
  "https://www.googleapis.com/auth/identitytoolkit",
].join(" ");

let cachedAccess = null;

/**
 * OAuth access token for the service account, so the Worker can read Firestore and send FCM
 * as the project itself. Cached per isolate until shortly before it expires.
 */
export async function serviceAccessToken(serviceAccountJson) {
  if (cachedAccess && cachedAccess.expiresAt > Date.now() + 60_000) return cachedAccess.token;

  const account = JSON.parse(serviceAccountJson);
  const key = await importPKCS8(account.private_key, "RS256");
  const now = Math.floor(Date.now() / 1000);
  const assertion = await new SignJWT({ scope: SCOPES })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(account.client_email)
    .setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(key);

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!response.ok) throw new Error(`Token exchange failed: ${response.status} ${await response.text()}`);
  const body = await response.json();
  cachedAccess = { token: body.access_token, expiresAt: Date.now() + body.expires_in * 1000 };
  return cachedAccess.token;
}

export class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}
