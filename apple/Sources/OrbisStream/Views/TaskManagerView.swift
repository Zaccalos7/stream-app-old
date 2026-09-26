import SwiftUI

public struct TaskManagerView: View {
    @State private var metrics: [SystemInfoDto] = []
    @State private var timer: Timer? = nil

    public init() {}

    public var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("System Statistics & Resource Monitor")
                .font(.largeTitle)
                .bold()

            Divider()

            LazyVGrid(columns: [GridItem(.adaptive(minimum: 220))], spacing: 20) {
                ForEach(metrics, id: \.infoName) { metric in
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Text(metric.infoName)
                                .font(.headline)
                            Spacer()
                            Text("\(Int(metric.percentageUsed))%")
                                .font(.title3)
                                .bold()
                        }

                        ProgressView(value: metric.percentageUsed, total: 100)
                            .tint(progressColor(metric.percentageUsed))

                        HStack {
                            Text("Used: \(metric.used)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Spacer()
                            Text("Total: \(metric.total)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding()
                    .background(Color.secondary.opacity(0.1))
                    .cornerRadius(12)
                }
            }

            Spacer()
        }
        .padding()
        .onAppear {
            refreshMetrics()
            timer = Timer.scheduledTimer(withTimeInterval: 2.0, repeats: true) { _ in
                refreshMetrics()
            }
        }
        .onDisappear {
            timer?.invalidate()
            timer = nil
        }
    }

    private func refreshMetrics() {
        metrics = TaskManagerService.shared.getSystemInfo()
    }

    private func progressColor(_ percent: Double) -> Color {
        if percent > 85 { return .red }
        if percent > 60 { return .orange }
        return .blue
    }
}
