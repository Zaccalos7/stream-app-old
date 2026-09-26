import Foundation

public struct Setting: Identifiable, Codable, Equatable, Hashable {
    public var id: Int?
    public var streamUrl: String
    public var streamKey: String
    public var platformStreamName: String?
    public var description: String?
    public var videoFolder: String
    public var isActive: Bool?
    public var channelName: String

    public init(id: Int? = nil, streamUrl: String, streamKey: String, platformStreamName: String? = nil, description: String? = nil, videoFolder: String = "/", isActive: Bool? = false, channelName: String = "Zingy") {
        self.id = id
        self.streamUrl = streamUrl
        self.streamKey = streamKey
        self.platformStreamName = platformStreamName
        self.description = description
        self.videoFolder = videoFolder
        self.isActive = isActive
        self.channelName = channelName
    }
}

public struct AudioSetting: Identifiable, Codable, Equatable, Hashable {
    public var id: Int?
    public var audioCodec: Int?
    public var audioBitrate: Int?

    public init(id: Int? = nil, audioCodec: Int? = nil, audioBitrate: Int? = nil) {
        self.id = id
        self.audioCodec = audioCodec
        self.audioBitrate = audioBitrate
    }
}

public struct VideoSettingsOption: Codable, Equatable, Hashable {
    public var key: String?
    public var value: String?

    public init(key: String? = nil, value: String? = nil) {
        self.key = key
        self.value = value
    }
}

public struct VideoSetting: Identifiable, Codable, Equatable, Hashable {
    public var id: Int?
    public var title: String?
    public var videoCodec: Int?
    public var videoCodecName: String?
    public var pixelFormat: Int?
    public var videoBitrate: Int?
    public var videoFormat: String?
    public var lastModified: Date?
    public var isDefaultConfiguration: Bool?
    public var defaultPlatformConfiguration: String?
    public var gopSize: Int
    public var isVideoAndAudioSettingActive: Bool
    public var videoSettingsOptions: [VideoSettingsOption]?
    public var audioSetting: AudioSetting?

    public init(id: Int? = nil, title: String? = "Title", videoCodec: Int? = nil, videoCodecName: String? = nil, pixelFormat: Int? = nil, videoBitrate: Int? = nil, videoFormat: String? = nil, lastModified: Date? = nil, isDefaultConfiguration: Bool? = false, defaultPlatformConfiguration: String? = "Custom", gopSize: Int = 2, isVideoAndAudioSettingActive: Bool = false, videoSettingsOptions: [VideoSettingsOption]? = [], audioSetting: AudioSetting? = nil) {
        self.id = id
        self.title = title
        self.videoCodec = videoCodec
        self.videoCodecName = videoCodecName
        self.pixelFormat = pixelFormat
        self.videoBitrate = videoBitrate
        self.videoFormat = videoFormat
        self.lastModified = lastModified
        self.isDefaultConfiguration = isDefaultConfiguration
        self.defaultPlatformConfiguration = defaultPlatformConfiguration
        self.gopSize = gopSize
        self.isVideoAndAudioSettingActive = isVideoAndAudioSettingActive
        self.videoSettingsOptions = videoSettingsOptions
        self.audioSetting = audioSetting
    }
}

public struct VideoLiveHistory: Identifiable, Codable, Equatable, Hashable {
    public var pkid: Int64?
    public var folderOfVideoToStream: String
    public var localDateTimeStartLive: Date
    public var streamUrl: String
    public var streamKey: String
    public var platformStreamName: String
    public var userName: String?

    public init(pkid: Int64? = nil, folderOfVideoToStream: String, localDateTimeStartLive: Date = Date(), streamUrl: String = "N/A", streamKey: String = "N/A", platformStreamName: String = "N/A", userName: String? = nil) {
        self.pkid = pkid
        self.folderOfVideoToStream = folderOfVideoToStream
        self.localDateTimeStartLive = localDateTimeStartLive
        self.streamUrl = streamUrl
        self.streamKey = streamKey
        self.platformStreamName = platformStreamName
        self.userName = userName
    }
}

public struct Video: Identifiable, Codable, Equatable, Hashable {
    public var pkid: Int?
    public var id: Int? { pkid }
    public var name: String
    public var videoPath: String
    public var extensionName: String
    public var liveStatus: LiveStatusEnum
    public var lastTimeStampBeforeStop: Int64?
    public var message: String?
    public var shouldBeStop: Bool?
    public var startDateLive: Date?
    public var channelName: String
    public var videoLiveHistory: VideoLiveHistory?
    public var videoSetting: VideoSetting?

    public init(pkid: Int? = nil, name: String, videoPath: String, extensionName: String, liveStatus: LiveStatusEnum = .offline, lastTimeStampBeforeStop: Int64? = 0, message: String? = nil, shouldBeStop: Bool? = false, startDateLive: Date? = nil, channelName: String = "Zingy", videoLiveHistory: VideoLiveHistory? = nil, videoSetting: VideoSetting? = nil) {
        self.pkid = pkid
        self.name = name
        self.videoPath = videoPath
        self.extensionName = extensionName
        self.liveStatus = liveStatus
        self.lastTimeStampBeforeStop = lastTimeStampBeforeStop
        self.message = message
        self.shouldBeStop = shouldBeStop
        self.startDateLive = startDateLive
        self.channelName = channelName
        self.videoLiveHistory = videoLiveHistory
        self.videoSetting = videoSetting
    }
}
