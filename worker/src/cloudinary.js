/**
 * Cloudinary signed-upload parameters. Signing happens here, with the API secret that never
 * leaves the Worker, so only signed-in LocalHive users can upload — and only into their own folder.
 *
 * Cloudinary's rule: sort the signed params by name, join as `k=v&k=v`, append the secret, SHA-1.
 */
export async function signUpload({ cloudName, apiKey, apiSecret, folder, now = Date.now() }) {
  const timestamp = Math.floor(now / 1000);
  const params = { folder, timestamp };
  const toSign = Object.keys(params)
    .sort()
    .map((k) => `${k}=${params[k]}`)
    .join("&");
  return {
    cloudName,
    apiKey,
    folder,
    timestamp,
    signature: await sha1Hex(toSign + apiSecret),
  };
}

async function sha1Hex(text) {
  const digest = await crypto.subtle.digest("SHA-1", new TextEncoder().encode(text));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

export const UPLOAD_KINDS = { profile: "profileImages", post: "postImages" };
