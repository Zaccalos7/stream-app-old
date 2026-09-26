import SwiftUI

public enum NavigationTab: String, CaseIterable, Identifiable {
    case dashboard = "Live Streaming"
    case settings = "Configurations"
    case videoSettings = "Video & Audio Settings"
    case taskManager = "Task Manager"
    case liveHistory = "Live History"

    public var id: String { rawValue }

    public var iconName: String {
        switch self {
        case .dashboard: return "play.tv.fill"
        case .settings: return "gearshape.2.fill"
        case .videoSettings: return "sliders.3"
        case .taskManager: return "cpu"
        case .liveHistory: return "clock.arrow.circlepath"
        }
    }
}

public struct MainView: View {
    @State private var selectedTab: NavigationTab = .dashboard

    public init() {}

    public var body: some View {
        NavigationSplitView {
            List(NavigationTab.allCases, selection: $selectedTab) { tab in
                NavigationLink(value: tab) {
                    Label(tab.rawValue, systemImage: tab.iconName)
                        .font(.body)
                }
            }
            .listStyle(.sidebar)
            .navigationTitle("Orbis Stream")
            .frame(minWidth: 200)
        } detail: {
            switch selectedTab {
            case .dashboard:
                DashboardView()
            case .settings:
                SettingView()
            case .videoSettings:
                VideoSettingView()
            case .taskManager:
                TaskManagerView()
            case .liveHistory:
                LiveHistoryView()
            }
        }
        .frame(minWidth: 900, minHeight: 600)
    }
}
