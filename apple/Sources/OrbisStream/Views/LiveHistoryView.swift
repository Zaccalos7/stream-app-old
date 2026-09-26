import SwiftUI

public struct LiveHistoryView: View {
    @State private var histories: [VideoLiveHistory] = []

    public init() {}

    public var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("Streaming History")
                .font(.largeTitle)
                .bold()

            Divider()

            if histories.isEmpty {
                Text("No live history recorded yet.")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .padding()
            } else {
                List(histories) { item in
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text(item.platformStreamName)
                                .font(.headline)
                            Spacer()
                            Text(item.localDateTimeStartLive, style: .date)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Text("Stream URL: \(item.streamUrl)")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        Text("Source: \(item.folderOfVideoToStream)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .padding()
        .onAppear {
            loadHistory()
        }
    }

    private func loadHistory() {
        histories = DatabaseManager.shared.getAllLiveHistory()
    }
}
