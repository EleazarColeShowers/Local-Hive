# Putting LocalHive on the App Store

The iOS app is ready apart from what needs the paid Apple Developer Program. Everything that does
is switched off by `ENABLE_PUSH = NO` in `Configuration/Config.xcconfig`, so the app still builds
and runs on a free Apple ID in the meantime.

Already done in the project:

- Push notifications, the SOS siren (`iosApp/sos_alarm.wav`) and tapping a push to open what it's about.
- `iosApp/iosApp.entitlements`: push, plus time-sensitive notifications so an SOS breaks through Focus.
  Critical alerts (sound on silent) are there commented out; they need Apple's approval first.
- `iosApp/PrivacyInfo.xcprivacy`: the privacy manifest App Review requires.
- `ITSAppUsesNonExemptEncryption = NO` in Info.plist, since the app only uses standard HTTPS. This
  skips the export-compliance question on every upload.
- iPhone only (`TARGETED_DEVICE_FAMILY = 1`), so review doesn't ask for iPad screenshots. iPads can
  still install it and run it at phone size.

## After you pay

1. **Join the Apple Developer Program** with the Apple ID whose team is `TEAM_ID` in
   `Config.xcconfig` (developer.apple.com > Membership shows it; update it if it changes).
2. **Turn push on:** set `ENABLE_PUSH = YES` in `Config.xcconfig`. Open the project in Xcode, select
   the iosApp target > Signing & Capabilities and let automatic signing register Push Notifications
   and Time Sensitive Notifications for `com.eleazar.localhive`.
3. **Connect push to Firebase:** developer.apple.com > Certificates, IDs & Profiles > Keys > create a
   key with Apple Push Notifications service (APNs). Download the `.p8` (you only get one chance),
   then upload it in Firebase console > Project settings > Cloud Messaging > Apple app configuration,
   with the Key ID and your Team ID.
4. **Test on a real iPhone:** run from Xcode, allow notifications, and from another account send an
   SOS with the app closed. You should hear the siren. Check light-is-back and payment pushes too.
5. **Create the app in App Store Connect:** My Apps > + > New App, bundle ID `com.eleazar.localhive`,
   name LocalHive.
6. **Fill in the listing:**
   - Privacy policy URL (required), and the same account-deletion page as Google Play.
   - App Privacy answers matching `PrivacyInfo.xcprivacy`: name, email, phone, address, user ID,
     photos, posts, messages and device ID, all linked to the user, used for app functionality,
     none for tracking.
   - Screenshots for a 6.9" iPhone (e.g. iPhone 16 Pro Max simulator).
   - Age rating: the app has user-generated content and messaging between users.
7. **Give App Review a way in.** Reviewers can't join an estate on their own. Create a demo account
   already inside a demo estate with a few posts, a levy and a poll, and put its email and password in
   App Review Information > Sign-in required. Without this the app will be rejected.
8. **Upload a build:** in Xcode choose Any iOS Device, Product > Archive, then Distribute App >
   App Store Connect. Raise `CFBundleVersion` in `iosApp/Info.plist` for every upload, and keep
   `CFBundleShortVersionString` in step with Android's `versionName`.
9. **TestFlight first:** install the build through TestFlight on a couple of iPhones, then submit
   for review.

## Later

- **Critical alerts**, so the SOS sounds even on silent: request the entitlement from Apple, then follow
  the note in `iosApp/iosApp.entitlements`.
