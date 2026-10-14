import FirebaseCore
import Shared
import SwiftUI
import UIKit

/** The Compose app from :shared, which draws edge to edge and handles insets itself, as on Android. */
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        if FirebaseApp.app() != nil {
            ComposeView().ignoresSafeArea()
        } else {
            VStack(spacing: 12) {
                Text("Firebase isn't set up").font(.headline)
                Text("Add this app to the LocalHive Firebase project as an iOS app (bundle ID com.eleazar.localhive), then put its GoogleService-Info.plist in iosApp/iosApp and build again.")
                    .font(.subheadline)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
            }
            .padding(32)
        }
    }
}
