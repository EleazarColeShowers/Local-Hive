/**
 * Cloudinary signed-upload parameters. Signing happens here, with the API secret that never
 * leaves the Worker, so only signed-in LocalHive users can upload — and only into their own folder.
 *
 * Cloudinary's rule: sort the signed params by name, join as `k=v&k=v`, append the secret, SHA-1.
 */
export async function signUpload({ cloudName, apiKey, apiSecret, folder, limits = {}, now = Date.now() }) {
  const timestamp = Math.floor(now / 1000);
  // Everything signed here has to be sent back with the file exactly as given, so [limits]
  // can't be dropped or loosened by whoever holds the signature.
  const params = { folder, timestamp, ...limits };
  const toSign = Object.keys(params)
    .sort()
    .map((k) => `${k}=${params[k]}`)
    .join("&");
  return {
    cloudName,
    apiKey,
    folder,
    timestamp,
    // Every signed field, for the app to send with the upload.
    params: Object.fromEntries(Object.entries(params).map(([k, v]) => [k, String(v)])),
    signature: await sha1Hex(toSign + apiSecret),
  };
}

async function sha1Hex(text) {
  const digest = await crypto.subtle.digest("SHA-1", new TextEncoder().encode(text));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

/**
 * Photos must be real images, and anything larger than the app ever sends is shrunk on arrival,
 * so a signature can't be used to fill the account with other files or huge originals.
 */
const PHOTO_LIMITS = { allowed_formats: "jpg,png,webp,heic", transformation: "c_limit,w_2000,h_2000" };
// Cloudinary's plan caps a video at 100 MB; the app keeps them under 40 MB.
const VIDEO_LIMITS = { allowed_formats: "mp4,mov,m4v,3gp,webm" };

/** Each upload kind's folder, the Cloudinary resource type its files are stored as, and its limits. */
export const UPLOAD_KINDS = {
  profile: { folder: "profileImages", resourceType: "image", limits: PHOTO_LIMITS },
  post: { folder: "postImages", resourceType: "image", limits: PHOTO_LIMITS },
  video: { folder: "postVideos", resourceType: "video", limits: VIDEO_LIMITS },
  // Proof of levy payments and receipts for estate expenses.
  receipt: { folder: "receipts", resourceType: "image", limits: PHOTO_LIMITS },
};
