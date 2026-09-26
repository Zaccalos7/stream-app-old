import SwiftUI

public struct DashboardView: View {
    @State private var channels: [ChannelRecord] = []
    @State private var selectedChannel: String = ""
    @State private var selectedVideoPath: String = ""
    @State private var streamUrl: String = ""
    @State private var streamKey: String = ""
    @State private var platformStreamName: String = "Twitch / YouTube"
    @State private var isLiveActive: Bool = false
    @State private var currentLivePkid: Int64? = nil
    @State private var statusMessage: String = "Ready"

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Header Area
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Live Streaming Control")
                            .font(.system(size: 32, weight: .bold, design: .rounded))
                            .foregroundStyle(.primary)
                        Text("Manage and monitor your broadcasts")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()

                    // Live Status Indicator Pill
                    HStack(spacing: 8) {
                        Circle()
                            .fill(isLiveActive ? Color.green : Color.red)
                            .frame(width: 10, height: 10)
                            .shadow(color: isLiveActive ? .green.opacity(0.5) : .red.opacity(0.5), radius: 4)
                        Text(isLiveActive ? "LIVE ONLINE" : "OFFLINE")
                            .font(.system(size: 14, weight: .bold, design: .rounded))
                            .foregroundColor(isLiveActive ? .green : .secondary)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(.regularMaterial)
                    .clipShape(Capsule())
                    .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 4)
                }
                .padding(.top, 16)

                // Cards
                VStack(spacing: 20) {

                    // Selection Card
                    VStack(alignment: .leading, spacing: 16) {
                        Label("Channel & Video", systemImage: "play.tv.fill")
                            .font(.headline)
                            .foregroundStyle(.primary)

                        Divider()

                        VStack(alignment: .leading, spacing: 12) {
                            Text("Channel")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Picker("", selection: $selectedChannel) {
                                Text("-- Select Channel --").tag("")
                                ForEach(channels, id: \.channelName) { ch in
                                    Text(ch.channelName).tag(ch.channelName)
                                }
                            }
                            .labelsHidden()
                            .pickerStyle(.menu)
                            .onChange(of: selectedChannel) { newChannel in
                                updateChannelDetails(newChannel)
                            }

                            Text("Video Source")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .padding(.top, 4)
                            Picker("", selection: $selectedVideoPath) {
                                Text("-- Select Video --").tag("")
                                if let ch = channels.first(where: { $0.channelName == selectedChannel }) {
                                    ForEach(ch.videoPathAndKeyList, id: \.videoPath) { item in
                                        Text(item.videoPath).tag(item.videoPath)
                                    }
                                }
                            }
                            .labelsHidden()
                            .pickerStyle(.menu)
                        }
                    }
                    .padding(20)
                    .background(.regularMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .shadow(color: .black.opacity(0.08), radius: 12, x: 0, y: 4)

                    // Endpoint Card
                    VStack(alignment: .leading, spacing: 16) {
                        Label("Stream Endpoint", systemImage: "network")
                            .font(.headline)
                            .foregroundStyle(.primary)

                        Divider()

                        VStack(alignment: .leading, spacing: 12) {
                            Text("RTMP Server URL")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            TextField("rtmp://...", text: $streamUrl)
                                .textFieldStyle(.roundedBorder)
                                .controlSize(.large)

                            Text("Stream Key")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .padding(.top, 4)
                            SecureField("Secret Key", text: $streamKey)
                                .textFieldStyle(.roundedBorder)
                                .controlSize(.large)

                            Text("Platform Name")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .padding(.top, 4)
                            TextField("e.g. Twitch Main", text: $platformStreamName)
                                .textFieldStyle(.roundedBorder)
                                .controlSize(.large)
                        }
                    }
                    .padding(20)
                    .background(.regularMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .shadow(color: .black.opacity(0.08), radius: 12, x: 0, y: 4)

                }

                // Action Buttons
                VStack(spacing: 12) {
                    HStack(spacing: 16) {
                        Button(action: startLive) {
                            HStack {
                                Spacer()
                                Image(systemName: "play.fill")
                                Text("Start Broadcast")
                                Spacer()
                            }
                            .font(.system(size: 16, weight: .semibold, design: .rounded))
                            .padding(.vertical, 10)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(Color.blue)
                        .clipShape(Capsule())
                        .disabled(isLiveActive || selectedVideoPath.isEmpty || streamUrl.isEmpty || streamKey.isEmpty)

                        Button(action: stopLive) {
                            HStack {
                                Spacer()
                                Image(systemName: "stop.fill")
                                Text("End Broadcast")
                                Spacer()
                            }
                            .font(.system(size: 16, weight: .semibold, design: .rounded))
                            .padding(.vertical, 10)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(Color.red)
                        .clipShape(Capsule())
                        .disabled(!isLiveActive)
                    }

                    if !statusMessage.isEmpty {
                        Text(statusMessage)
                            .font(.system(.callout, design: .rounded))
                            .foregroundColor(.secondary)
                            .padding(.top, 8)
                            .animation(.easeInOut, value: statusMessage)
                    }
                }
                .padding(.top, 10)

            }
            .padding(32)
        }
        .background(Color(NSColor.windowBackgroundColor))
        .onAppear {
            loadChannels()
        }
    }

    private func loadChannels() {
        channels = VideoService.shared.getAllChannelsWithVideoLive()
        if let first = channels.first {
            selectedChannel = first.channelName
            updateChannelDetails(first.channelName)
        }
    }

    private func updateChannelDetails(_ channelName: String) {
        let settings = SettingService.shared.retrieveSettings(channelFilter: channelName)
        if let firstSetting = settings.first {
            streamUrl = firstSetting.streamUrl
            streamKey = firstSetting.streamKey
            if let platform = firstSetting.platformStreamName {
                platformStreamName = platform
            }
        }
    }

    private func startLive() {
        let record = StartLiveRecord(
            streamUrl: streamUrl,
            streamKey: streamKey,
            videoPath: selectedVideoPath,
            platformStreamName: platformStreamName,
            channelName: selectedChannel
        )
        let res = StreamService.shared.startLive(record: record)
        if res.success {
            isLiveActive = true
            currentLivePkid = res.liveHistoryPkid
            statusMessage = "Live streaming started successfully!"
        } else {
            statusMessage = "Failed to start live: \(res.message)"
        }
    }

    private func stopLive() {
        if let pkid = currentLivePkid {
            _ = StreamService.shared.stopVideoStreamingByPkid(Int(pkid))
        }
        isLiveActive = false
        currentLivePkid = nil
        statusMessage = "Live streaming stopped."
    }
}
