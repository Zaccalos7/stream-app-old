import SwiftUI

public struct SettingView: View {
    @State private var settingsList: [Setting] = []
    @State private var channelName: String = ""
    @State private var streamUrl: String = ""
    @State private var streamKey: String = ""
    @State private var platformStreamName: String = ""
    @State private var descriptionText: String = ""
    @State private var videoFolder: String = "/"
    @State private var selectedSettingId: Int? = nil

    public init() {}

    public var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("Channel & Stream Configurations")
                .font(.largeTitle)
                .bold()

            Divider()

            HStack(alignment: .top, spacing: 20) {
                // Settings Form
                VStack(alignment: .leading, spacing: 12) {
                    Text(selectedSettingId == nil ? "New Configuration" : "Edit Configuration")
                        .font(.title2)
                        .bold()

                    TextField("Channel Name", text: $channelName)
                        .textFieldStyle(.roundedBorder)

                    TextField("Stream URL (RTMP)", text: $streamUrl)
                        .textFieldStyle(.roundedBorder)

                    SecureField("Stream Key", text: $streamKey)
                        .textFieldStyle(.roundedBorder)

                    TextField("Platform Name (e.g. Twitch Main)", text: $platformStreamName)
                        .textFieldStyle(.roundedBorder)

                    TextField("Video Folder Path", text: $videoFolder)
                        .textFieldStyle(.roundedBorder)

                    TextField("Description", text: $descriptionText)
                        .textFieldStyle(.roundedBorder)

                    HStack {
                        Button(action: saveConfiguration) {
                            Text(selectedSettingId == nil ? "Save Configuration" : "Update Configuration")
                        }
                        .buttonStyle(.borderedProminent)

                        if selectedSettingId != nil {
                            Button("Cancel") {
                                resetForm()
                            }
                            .buttonStyle(.bordered)
                        }
                    }
                }
                .frame(width: 320)
                .padding()
                .background(Color.secondary.opacity(0.1))
                .cornerRadius(12)

                // Settings List
                VStack(alignment: .leading) {
                    Text("Saved Configurations")
                        .font(.title2)
                        .bold()

                    List(settingsList) { setting in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(setting.channelName)
                                    .font(.headline)
                                Text(setting.streamUrl)
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                                Text("Folder: \(setting.videoFolder)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            Button("Edit") {
                                editConfiguration(setting)
                            }
                            .buttonStyle(.bordered)

                            Button("Scan") {
                                scanFolder(setting)
                            }
                            .buttonStyle(.bordered)

                            Button("Delete", role: .destructive) {
                                deleteConfiguration(setting)
                            }
                            .buttonStyle(.bordered)
                        }
                        .padding(.vertical, 4)
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
        settingsList = SettingService.shared.retrieveSettings()
    }

    private func saveConfiguration() {
        let record = SettingRecord(
            streamUrl: streamUrl,
            streamKey: streamKey,
            platformStreamName: platformStreamName,
            description: descriptionText,
            videoFolder: videoFolder,
            isActive: true,
            channelName: channelName
        )
        if let id = selectedSettingId {
            _ = SettingService.shared.changeSettings(id: id, record: record)
        } else {
            _ = SettingService.shared.saveSettings(record)
        }
        resetForm()
        loadSettings()
    }

    private func editConfiguration(_ setting: Setting) {
        selectedSettingId = setting.id
        channelName = setting.channelName
        streamUrl = setting.streamUrl
        streamKey = setting.streamKey
        platformStreamName = setting.platformStreamName ?? ""
        descriptionText = setting.description ?? ""
        videoFolder = setting.videoFolder
    }

    private func deleteConfiguration(_ setting: Setting) {
        if let id = setting.id {
            _ = SettingService.shared.deleteSetting(id: id)
            loadSettings()
        }
    }

    private func scanFolder(_ setting: Setting) {
        _ = VideoService.shared.scanFolderForVideos(folderPath: setting.videoFolder, channelName: setting.channelName)
    }

    private func resetForm() {
        selectedSettingId = nil
        channelName = ""
        streamUrl = ""
        streamKey = ""
        platformStreamName = ""
        descriptionText = ""
        videoFolder = "/"
    }
}
