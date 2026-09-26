import SwiftUI

@main
struct OrbisStreamApp: App {
    var body: some Scene {
        WindowGroup {
            MainView()
                .background(Color(NSColor.windowBackgroundColor))
        }
        .windowStyle(.hiddenTitleBar)
        .defaultSize(width: 1000, height: 700)
    }
}
