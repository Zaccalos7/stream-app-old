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
        HStack(spacing: 0) {
            // Settings Form - macOS Native Style
            Form {
                Section(header: Text(selectedSettingId == nil ? "New Configuration" : "Edit Configuration").font(.headline)) {
                    TextField("Channel Name", text: $channelName)
                    TextField("Stream URL (RTMP)", text: $streamUrl)
                    SecureField("Stream Key", text: $streamKey)
                    TextField("Platform Name (e.g. Twitch Main)", text: $platformStreamName)
                    TextField("Video Folder Path", text: $videoFolder)
                    TextField("Description", text: $descriptionText)

                    HStack {
                        Spacer()
                        if selectedSettingId != nil {
                            Button("Cancel") {
                                resetForm()
                            }
                        }
                        Button(action: saveConfiguration) {
                            Text(selectedSettingId == nil ? "Save Configuration" : "Update Configuration")
                        }
                        .buttonStyle(.borderedProminent)
                        .controlSize(.large)
                    }
                    .padding(.top, 10)
                }
            }
            .formStyle(.grouped)
            .frame(width: 400)

            Divider()

            // Settings List - macOS Table Style
            VStack(alignment: .leading, spacing: 0) {
                Text("Saved Configurations")
                    .font(.headline)
                    .padding()

                List(settingsList, selection: $selectedSettingId) { setting in
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

                        Button(action: {
                            editConfiguration(setting)
                        }) {
                            Label("Edit", systemImage: "pencil")
                        }
                        .buttonStyle(.borderless)

                        Button(action: {
                            scanFolder(setting)
                        }) {
                            Label("Scan", systemImage: "arrow.triangle.2.circlepath")
                        }
                        .buttonStyle(.borderless)

                        Button(action: {
                            deleteConfiguration(setting)
                        }) {
                            Label("Delete", systemImage: "trash")
                                .foregroundColor(.red)
                        }
                        .buttonStyle(.borderless)
                    }
                    .padding(.vertical, 4)
                    .tag(setting.id)
                }
                .listStyle(.inset(alternatesRowBackgrounds: true))
            }
        }
        .navigationTitle("Channel & Stream Configurations")
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
            if selectedSettingId == id {
                resetForm()
            }
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
