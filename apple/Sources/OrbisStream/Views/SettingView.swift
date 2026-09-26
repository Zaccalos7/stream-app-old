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
            // Left Panel: Settings Form
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(selectedSettingId == nil ? "New Configuration" : "Edit Configuration")
                            .font(.system(size: 28, weight: .bold, design: .rounded))
                            .foregroundStyle(.primary)
                        Text("Set up your channel and stream details")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.bottom, 8)

                    VStack(alignment: .leading, spacing: 20) {

                        VStack(alignment: .leading, spacing: 8) {
                            Text("Channel Name")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            TextField("Enter channel name", text: $channelName)
                                .textFieldStyle(.roundedBorder)
                                .controlSize(.large)
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            Text("Stream URL (RTMP)")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            TextField("rtmp://...", text: $streamUrl)
                                .textFieldStyle(.roundedBorder)
                                .controlSize(.large)
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            Text("Stream Key")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            SecureField("Enter secret key", text: $streamKey)
                                .textFieldStyle(.roundedBorder)
                                .controlSize(.large)
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            Text("Platform Name")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            TextField("e.g. Twitch Main", text: $platformStreamName)
                                .textFieldStyle(.roundedBorder)
                                .controlSize(.large)
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            Text("Video Folder Path")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            TextField("/path/to/videos", text: $videoFolder)
                                .textFieldStyle(.roundedBorder)
                                .controlSize(.large)
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            Text("Description")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            TextField("Optional description", text: $descriptionText)
                                .textFieldStyle(.roundedBorder)
                                .controlSize(.large)
                        }
                    }
                    .padding(24)
                    .background(.regularMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .shadow(color: .black.opacity(0.06), radius: 10, x: 0, y: 4)

                    HStack(spacing: 16) {
                        Spacer()
                        if selectedSettingId != nil {
                            Button(action: resetForm) {
                                Text("Cancel")
                                    .font(.system(size: 15, weight: .medium, design: .rounded))
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 8)
                            }
                            .buttonStyle(.borderless)
                        }

                        Button(action: saveConfiguration) {
                            Text(selectedSettingId == nil ? "Save Configuration" : "Update Configuration")
                                .font(.system(size: 15, weight: .semibold, design: .rounded))
                                .padding(.horizontal, 24)
                                .padding(.vertical, 10)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(.blue)
                        .clipShape(Capsule())
                    }
                }
                .padding(32)
            }
            .frame(minWidth: 450, maxWidth: 500)
            .background(Color(NSColor.windowBackgroundColor))

            Divider()

            // Right Panel: Saved Configurations List
            VStack(alignment: .leading, spacing: 0) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Saved Configurations")
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                        .foregroundStyle(.primary)
                }
                .padding(.horizontal, 24)
                .padding(.top, 32)
                .padding(.bottom, 16)

                List(settingsList, selection: $selectedSettingId) { setting in
                    HStack {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(setting.channelName)
                                .font(.system(size: 16, weight: .semibold, design: .rounded))
                                .foregroundStyle(.primary)

                            Text(setting.streamUrl)
                                .font(.system(size: 13, design: .monospaced))
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                                .truncationMode(.tail)

                            HStack(spacing: 4) {
                                Image(systemName: "folder.fill")
                                    .foregroundStyle(.tertiary)
                                Text(setting.videoFolder)
                            }
                            .font(.system(size: 12))
                            .foregroundStyle(.secondary)
                        }
                        Spacer()

                        VStack(spacing: 12) {
                            HStack(spacing: 16) {
                                Button(action: { editConfiguration(setting) }) {
                                    Image(systemName: "pencil")
                                        .foregroundStyle(.blue)
                                }
                                .buttonStyle(.plain)

                                Button(action: { scanFolder(setting) }) {
                                    Image(systemName: "arrow.triangle.2.circlepath")
                                        .foregroundStyle(.orange)
                                }
                                .buttonStyle(.plain)

                                Button(action: { deleteConfiguration(setting) }) {
                                    Image(systemName: "trash")
                                        .foregroundStyle(.red)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .padding(.vertical, 12)
                    .padding(.horizontal, 8)
                    .tag(setting.id)
                }
                .listStyle(.inset)
                .scrollContentBackground(.hidden)
                .background(Color(NSColor.controlBackgroundColor))
            }
            .frame(maxWidth: .infinity)
            .background(Color(NSColor.controlBackgroundColor))
        }
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
