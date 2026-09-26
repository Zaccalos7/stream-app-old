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
        VStack(alignment: .leading, spacing: 20) {
            HStack {
                Text("Live Streaming Control")
                    .font(.largeTitle)
                    .bold()
                Spacer()
                HStack(spacing: 8) {
                    Circle()
                        .fill(isLiveActive ? Color.green : Color.red)
                        .frame(width: 12, height: 12)
                    Text(isLiveActive ? "LIVE ONLINE" : "OFFLINE")
                        .font(.headline)
                        .foregroundColor(isLiveActive ? .green : .secondary)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.secondary.opacity(0.1))
                .cornerRadius(12)
            }

            Divider()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Group {
                        Text("Channel & Video Selection")
                            .font(.title2)
                            .bold()

                        Picker("Select Channel", selection: $selectedChannel) {
                            Text("-- Select Channel --").tag("")
                            ForEach(channels, id: \.channelName) { ch in
                                Text(ch.channelName).tag(ch.channelName)
                            }
                        }
                        .pickerStyle(.menu)
                        .onChange(of: selectedChannel) { newChannel in
                            updateChannelDetails(newChannel)
                        }

                        Picker("Select Video", selection: $selectedVideoPath) {
                            Text("-- Select Video --").tag("")
                            if let ch = channels.first(where: { $0.channelName == selectedChannel }) {
                                ForEach(ch.videoPathAndKeyList, id: \.videoPath) { item in
                                    Text(item.videoPath).tag(item.videoPath)
                                }
                            }
                        }
                        .pickerStyle(.menu)
                    }

                    Divider()

                    Group {
                        Text("Stream Endpoint")
                            .font(.title2)
                            .bold()

                        TextField("RTMP Server URL (rtmp://...)", text: $streamUrl)
                            .textFieldStyle(.roundedBorder)

                        SecureField("Stream Key", text: $streamKey)
                            .textFieldStyle(.roundedBorder)

                        TextField("Platform Name (e.g. Twitch Main)", text: $platformStreamName)
                            .textFieldStyle(.roundedBorder)
                    }

                    Divider()

                    HStack(spacing: 16) {
                        Button(action: startLive) {
                            Label("Start Live Stream", systemImage: "play.fill")
                                .font(.headline)
                                .padding(.vertical, 8)
                                .padding(.horizontal, 16)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(.green)
                        .disabled(isLiveActive || selectedVideoPath.isEmpty || streamUrl.isEmpty || streamKey.isEmpty)

                        Button(action: stopLive) {
                            Label("Stop Live Stream", systemImage: "stop.fill")
                                .font(.headline)
                                .padding(.vertical, 8)
                                .padding(.horizontal, 16)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(.red)
                        .disabled(!isLiveActive)
                    }

                    if !statusMessage.isEmpty {
                        Text(statusMessage)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .padding(.top, 8)
                    }
                }
                .padding()
            }
        }
        .padding()
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
