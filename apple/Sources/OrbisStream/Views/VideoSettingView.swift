import SwiftUI

public struct VideoSettingView: View {
    @State private var videoSettings: [VideoSetting] = []
    @State private var title: String = ""
    @State private var videoCodecName: String = "libx264"
    @State private var videoBitrate: String = "2500000"
    @State private var gopSize: String = "2"
    @State private var audioBitrate: String = "128000"

    public init() {}

    public var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("Video & Audio Transcoding Settings")
                .font(.largeTitle)
                .bold()

            Divider()

            HStack(alignment: .top, spacing: 20) {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Add Preset")
                        .font(.title2)
                        .bold()

                    TextField("Preset Title", text: $title)
                        .textFieldStyle(.roundedBorder)

                    TextField("Video Codec (e.g. libx264, h264_nvenc)", text: $videoCodecName)
                        .textFieldStyle(.roundedBorder)

                    TextField("Video Bitrate (bps)", text: $videoBitrate)
                        .textFieldStyle(.roundedBorder)

                    TextField("GOP Size (seconds)", text: $gopSize)
                        .textFieldStyle(.roundedBorder)

                    TextField("Audio Bitrate (bps)", text: $audioBitrate)
                        .textFieldStyle(.roundedBorder)

                    Button("Save Preset") {
                        savePreset()
                    }
                    .buttonStyle(.borderedProminent)
                }
                .frame(width: 300)
                .padding()
                .background(Color.secondary.opacity(0.1))
                .cornerRadius(12)

                VStack(alignment: .leading) {
                    Text("Preset List")
                        .font(.title2)
                        .bold()

                    List(videoSettings) { item in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.title ?? "Preset")
                                    .font(.headline)
                                Text("Codec: \(item.videoCodecName ?? "libx264") | Bitrate: \(item.videoBitrate ?? 2500000) bps")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            Button("Delete", role: .destructive) {
                                if let id = item.id {
                                    _ = VideoSettingService.shared.deleteVideoSetting(id: id)
                                    loadSettings()
                                }
                            }
                            .buttonStyle(.bordered)
                        }
                    }
                }
            }
        }
        .padding()
        .onAppear {
            loadSettings()
        }
    }

    private func loadSettings() {
        videoSettings = VideoSettingService.shared.getAllVideoSettings()
    }

    private func savePreset() {
        let aRec = AudioSettingsRecord(audioCodec: 0, audioBitrate: Int(audioBitrate) ?? 128000)
        let record = VideoSettingsRecord(
            title: title.isEmpty ? "Custom Preset" : title,
            videoCodec: 0,
            videoCodecName: videoCodecName,
            pixelFormat: 0,
            videoBitrate: Int(videoBitrate) ?? 2500000,
            isVideoAndAudioSettingActive: true,
            gopSize: Int(gopSize) ?? 2,
            videoFormat: "flv",
            audioSettingRecord: aRec,
            isDefaultConfiguration: false,
            defaultPlatformConfiguration: "Custom"
        )
        _ = VideoSettingService.shared.saveVideoSetting(record)
        loadSettings()
        title = ""
    }
}
