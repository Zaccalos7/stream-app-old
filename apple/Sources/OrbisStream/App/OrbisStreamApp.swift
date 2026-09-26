import SwiftUI

@main
struct OrbisStreamApp: App {
    var body: some Scene {
        WindowGroup {
            MainView()
        }
        .windowStyle(.titleBar)
        .defaultSize(width: 1000, height: 700)
    }
}
