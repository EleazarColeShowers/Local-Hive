import FirebaseCore
import FirebaseMessaging
import Shared
import SwiftUI
import UserNotifications

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                // localhive:// links, the same ones Android opens, e.g. an estate invite.
                .onOpenURL { url in LocalHiveIos.shared.openLink(url: url.absoluteString) }
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Without the Firebase config there's nothing to sign in to; ContentView says what's missing.
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else { return true }
        FirebaseApp.configure()
        LocalHiveIos.shared.start()

        // ENABLE_PUSH in Config.xcconfig: off for free Apple IDs, which can't sign for push.
        guard Bundle.main.object(forInfoDictionaryKey: "LocalHivePushEnabled") as? String == "YES" else { return true }
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        // Once Apple approves critical alerts (see iosApp.entitlements), use
        // [.alert, .badge, .sound, .criticalAlert] so the SOS siren can sound on silent.
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in }
        // Firebase picks up the APNs token itself and answers with an FCM token below.
        application.registerForRemoteNotifications()
        return true
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        LocalHiveIos.shared.onPushToken(token: fcmToken)
    }

    /** A push that arrives while the app is open still shows, unless pushes are switched off in Settings. */
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler(LocalHiveIos.shared.pushNotificationsEnabled() ? [.banner, .list, .sound] : [])
    }

    /** Tapping a push opens what it's about: the Worker puts a localhive:// link in its data. */
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if let link = response.notification.request.content.userInfo["deepLink"] as? String {
            LocalHiveIos.shared.openLink(url: link)
        }
        completionHandler()
    }
}
