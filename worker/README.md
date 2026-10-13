# LocalHive Worker

A free Cloudflare Worker that stands in for Cloud Functions and Firebase Storage, both of
which need Firebase's paid Blaze plan. It:

- **signs photo uploads** to Cloudinary, after checking the user's Firebase login, so the
  Cloudinary secret never ships in the app;
- **sends push notifications** for chat messages, join requests and announcements. It reads
  the message, notification or post from Firestore itself, checks the caller is its sender,
  and sends each push at most once.

Everything here is on free tiers that don't ask for a card.

## One-time setup

**1. Cloudinary** (photos). Sign up at https://cloudinary.com/users/register_free. On the
dashboard, note the **Cloud name**, **API key** and **API secret**.

**2. Firebase service account** (lets the Worker read Firestore and send pushes).
Firebase console → Project settings → Service accounts → **Generate new private key**.
Save the JSON file somewhere outside the project. It's a password, so never commit it.

**3. Cloudflare** (runs the Worker). Sign up at https://dash.cloudflare.com/sign-up, then:

```sh
cd worker
npm install
npx wrangler login                                    # opens the browser once
npx wrangler secret put FIREBASE_SERVICE_ACCOUNT < /path/to/service-account.json
npx wrangler secret put CLOUDINARY_API_KEY            # paste when prompted
npx wrangler secret put CLOUDINARY_API_SECRET         # paste when prompted
```

Put your Cloudinary cloud name in `wrangler.toml` (`CLOUDINARY_CLOUD_NAME = "…"`), then:

```sh
npx wrangler deploy
```

It prints a URL like `https://localhive.<you>.workers.dev`.

**4. Point the app at it.** In the project's `local.properties`, add:

```
localhive.apiUrl=https://localhive.<you>.workers.dev
```

Rebuild the app. Without this line the app still runs, just without photo uploads or pushes.

You can delete the service-account JSON file once it's stored as a secret.

## Development

```sh
npm test          # unit tests for the permission checks and upload signing
npx wrangler tail # live logs from the deployed Worker
```

## Free-tier headroom

- Cloudflare Workers: 100,000 requests/day.
- Cloudinary: 25 monthly credits (roughly 25 GB of storage plus bandwidth combined).
  Photos are compressed on the phone first (1600px max for posts, 800px for avatars).
- FCM: free and unlimited.
