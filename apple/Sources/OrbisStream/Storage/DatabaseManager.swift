import Foundation

public final class DatabaseManager {
    public static let shared = DatabaseManager()

    private var settings: [Setting] = []
    private var videoSettings: [VideoSetting] = []
    private var videos: [Video] = []
    private var liveHistories: [VideoLiveHistory] = []

    private var nextSettingId = 1
    private var nextVideoSettingId = 1
    private var nextVideoPkid = 1
    private var nextHistoryPkid: Int64 = 1

    private let lock = NSLock()

    private init() {
        // Seed default VideoSetting if needed
        let defaultSetting = VideoSetting(
            id: 1,
            title: "Default Config",
            videoCodec: 0,
            videoCodecName: "libx264",
            pixelFormat: 0,
            videoBitrate: 2500000,
            videoFormat: "flv",
            lastModified: Date(),
            isDefaultConfiguration: true,
            defaultPlatformConfiguration: "Twitch / YouTube",
            gopSize: 2,
            isVideoAndAudioSettingActive: true,
            videoSettingsOptions: [
                VideoSettingsOption(key: "preset", value: "veryfast"),
                VideoSettingsOption(key: "tune", value: "zerolatency")
            ],
            audioSetting: AudioSetting(id: 1, audioCodec: 0, audioBitrate: 128000)
        )
        videoSettings.append(defaultSetting)
        nextVideoSettingId = 2
    }

    // MARK: - Settings Repository Operations

    public func saveSetting(_ settingRecord: SettingRecord) -> Setting {
        lock.lock()
        defer { lock.unlock() }

        let setting = Setting(
            id: nextSettingId,
            streamUrl: settingRecord.streamUrl,
            streamKey: settingRecord.streamKey,
            platformStreamName: settingRecord.platformStreamName,
            description: settingRecord.description,
            videoFolder: settingRecord.videoFolder,
            isActive: settingRecord.isActive ?? false,
            channelName: settingRecord.channelName
        )
        nextSettingId += 1
        settings.append(setting)
        return setting
    }

    public func updateSetting(id: Int, settingRecord: SettingRecord) -> Setting? {
        lock.lock()
        defer { lock.unlock() }

        guard let index = settings.firstIndex(where: { $0.id == id }) else { return nil }
        settings[index].streamUrl = settingRecord.streamUrl
        settings[index].streamKey = settingRecord.streamKey
        settings[index].platformStreamName = settingRecord.platformStreamName
        settings[index].description = settingRecord.description
        settings[index].videoFolder = settingRecord.videoFolder
        if let active = settingRecord.isActive {
            settings[index].isActive = active
        }
        settings[index].channelName = settingRecord.channelName
        return settings[index]
    }

    public func getAllSettings() -> [Setting] {
        lock.lock()
        defer { lock.unlock() }
        return settings
    }

    public func deleteSetting(id: Int) -> Bool {
        lock.lock()
        defer { lock.unlock() }
        if let idx = settings.firstIndex(where: { $0.id == id }) {
            settings.remove(at: idx)
            return true
        }
        return false
    }

    public func getAllChannels() -> Set<String> {
        lock.lock()
        defer { lock.unlock() }
        return Set(settings.map { $0.channelName })
    }

    // MARK: - Video Settings Operations

    public func getAllVideoSettings() -> [VideoSetting] {
        lock.lock()
        defer { lock.unlock() }
        return videoSettings
    }

    public func saveVideoSetting(_ record: VideoSettingsRecord) -> VideoSetting {
        lock.lock()
        defer { lock.unlock() }

        let audio: AudioSetting?
        if let aRec = record.audioSettingRecord {
            audio = AudioSetting(id: 1, audioCodec: aRec.audioCodec, audioBitrate: aRec.audioBitrate)
        } else {
            audio = nil
        }

        let options = record.videoOptions?.map { VideoSettingsOption(key: $0.key, value: $0.value) } ?? []

        let setting = VideoSetting(
            id: nextVideoSettingId,
            title: record.title ?? "Setting \(nextVideoSettingId)",
            videoCodec: record.videoCodec,
            videoCodecName: record.videoCodecName,
            pixelFormat: record.pixelFormat,
            videoBitrate: record.videoBitrate,
            videoFormat: record.videoFormat,
            lastModified: Date(),
            isDefaultConfiguration: record.isDefaultConfiguration ?? false,
            defaultPlatformConfiguration: record.defaultPlatformConfiguration ?? "Custom",
            gopSize: record.gopSize ?? 2,
            isVideoAndAudioSettingActive: record.isVideoAndAudioSettingActive ?? false,
            videoSettingsOptions: options,
            audioSetting: audio
        )
        nextVideoSettingId += 1
        videoSettings.append(setting)
        return setting
    }

    public func deleteVideoSetting(id: Int) -> Bool {
        lock.lock()
        defer { lock.unlock() }
        if let idx = videoSettings.firstIndex(where: { $0.id == id }) {
            videoSettings.remove(at: idx)
            return true
        }
        return false
    }

    // MARK: - Video & Live History Operations

    public func getAllVideos() -> [Video] {
        lock.lock()
        defer { lock.unlock() }
        return videos
    }

    public func saveVideo(_ video: Video) -> Video {
        lock.lock()
        defer { lock.unlock() }
        var v = video
        if v.pkid == nil {
            v.pkid = nextVideoPkid
            nextVideoPkid += 1
        }
        if let idx = videos.firstIndex(where: { $0.pkid == v.pkid }) {
            videos[idx] = v
        } else {
            videos.append(v)
        }
        return v
    }

    public func saveLiveHistory(_ history: VideoLiveHistory) -> VideoLiveHistory {
        lock.lock()
        defer { lock.unlock() }
        var h = history
        if h.pkid == nil {
            h.pkid = nextHistoryPkid
            nextHistoryPkid += 1
        }
        if let idx = liveHistories.firstIndex(where: { $0.pkid == h.pkid }) {
            liveHistories[idx] = h
        } else {
            liveHistories.append(h)
        }
        return h
    }

    public func getAllLiveHistory() -> [VideoLiveHistory] {
        lock.lock()
        defer { lock.unlock() }
        return liveHistories
    }
}
