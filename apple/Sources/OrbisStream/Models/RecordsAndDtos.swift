import Foundation

public struct AudioSettingsRecord: Codable, Equatable, Hashable {
    public var audioCodec: Int?
    public var audioBitrate: Int?

    public init(audioCodec: Int? = nil, audioBitrate: Int? = nil) {
        self.audioCodec = audioCodec
        self.audioBitrate = audioBitrate
    }
}

public struct RegisterRecord: Codable {
    public var nickName: String
    public var email: String
    public var password: String

    public init(nickName: String, email: String, password: String) {
        self.nickName = nickName
        self.email = email
        self.password = password
    }
}

public struct SettingRecord: Codable, Equatable, Hashable {
    public var streamUrl: String
    public var streamKey: String
    public var platformStreamName: String
    public var description: String
    public var videoFolder: String
    public var isActive: Bool?
    public var channelName: String

    public init(streamUrl: String, streamKey: String, platformStreamName: String, description: String, videoFolder: String, isActive: Bool? = false, channelName: String) {
        self.streamUrl = streamUrl
        self.streamKey = streamKey
        self.platformStreamName = platformStreamName
        self.description = description
        self.videoFolder = videoFolder
        self.isActive = isActive
        self.channelName = channelName
    }
}

public struct VideoOptionRecord: Codable, Equatable, Hashable {
    public var key: String?
    public var value: String?

    public init(key: String? = nil, value: String? = nil) {
        self.key = key
        self.value = value
    }
}

public struct VideoSettingsRecord: Codable, Equatable, Hashable {
    public var id: Int?
    public var title: String?
    public var videoCodec: Int?
    public var videoCodecName: String?
    public var pixelFormat: Int?
    public var videoBitrate: Int?
    public var lastModified: Date?
    public var isVideoAndAudioSettingActive: Bool?
    public var gopSize: Int?
    public var videoOptions: [VideoOptionRecord]?
    public var videoFormat: String?
    public var audioSettingRecord: AudioSettingsRecord?
    public var isDefaultConfiguration: Bool?
    public var defaultPlatformConfiguration: String?

    public init(id: Int? = nil, title: String? = nil, videoCodec: Int? = nil, videoCodecName: String? = nil, pixelFormat: Int? = nil, videoBitrate: Int? = nil, lastModified: Date? = nil, isVideoAndAudioSettingActive: Bool? = nil, gopSize: Int? = nil, videoOptions: [VideoOptionRecord]? = nil, videoFormat: String? = nil, audioSettingRecord: AudioSettingsRecord? = nil, isDefaultConfiguration: Bool? = nil, defaultPlatformConfiguration: String? = nil) {
        self.id = id
        self.title = title
        self.videoCodec = videoCodec
        self.videoCodecName = videoCodecName
        self.pixelFormat = pixelFormat
        self.videoBitrate = videoBitrate
        self.lastModified = lastModified
        self.isVideoAndAudioSettingActive = isVideoAndAudioSettingActive
        self.gopSize = gopSize
        self.videoOptions = videoOptions
        self.videoFormat = videoFormat
        self.audioSettingRecord = audioSettingRecord
        self.isDefaultConfiguration = isDefaultConfiguration
        self.defaultPlatformConfiguration = defaultPlatformConfiguration
    }
}

public struct VideoLiveHistoryRecord: Codable, Equatable, Hashable {
    public var pkid: Int64?
    public var folderOfVideoToStream: String?
    public var localDateTimeStartLive: Date?
    public var streamUrl: String?
    public var streamKey: String?
    public var platformStreamName: String?
    public var userName: String?

    public init(pkid: Int64? = nil, folderOfVideoToStream: String? = nil, localDateTimeStartLive: Date? = nil, streamUrl: String? = nil, streamKey: String? = nil, platformStreamName: String? = nil, userName: String? = nil) {
        self.pkid = pkid
        self.folderOfVideoToStream = folderOfVideoToStream
        self.localDateTimeStartLive = localDateTimeStartLive
        self.streamUrl = streamUrl
        self.streamKey = streamKey
        self.platformStreamName = platformStreamName
        self.userName = userName
    }
}

public struct StartLiveRecord: Codable {
    public var streamUrl: String
    public var streamKey: String
    public var videoPath: String
    public var platformStreamName: String
    public var channelName: String
    public var videoSettingsRecord: VideoSettingsRecord?

    public init(streamUrl: String, streamKey: String, videoPath: String, platformStreamName: String, channelName: String, videoSettingsRecord: VideoSettingsRecord? = nil) {
        self.streamUrl = streamUrl
        self.streamKey = streamKey
        self.videoPath = videoPath
        self.platformStreamName = platformStreamName
        self.channelName = channelName
        self.videoSettingsRecord = videoSettingsRecord
    }
}

public struct VideoRecord: Codable {
    public var pkid: Int?
    public var name: String
    public var videoPath: String
    public var extensionName: String
    public var liveStatus: LiveStatusEnum
    public var lastTimeStampBeforeStop: Int64?
    public var message: String?
    public var videoLiveHistory: VideoLiveHistoryRecord?
    public var videoSetting: VideoSettingsRecord?
    public var shouldBeStop: Bool?
    public var startDateLive: Date?
    public var channelName: String

    public init(pkid: Int? = nil, name: String, videoPath: String, extensionName: String, liveStatus: LiveStatusEnum = .offline, lastTimeStampBeforeStop: Int64? = 0, message: String? = nil, videoLiveHistory: VideoLiveHistoryRecord? = nil, videoSetting: VideoSettingsRecord? = nil, shouldBeStop: Bool? = false, startDateLive: Date? = nil, channelName: String) {
        self.pkid = pkid
        self.name = name
        self.videoPath = videoPath
        self.extensionName = extensionName
        self.liveStatus = liveStatus
        self.lastTimeStampBeforeStop = lastTimeStampBeforeStop
        self.message = message
        self.videoLiveHistory = videoLiveHistory
        self.videoSetting = videoSetting
        self.shouldBeStop = shouldBeStop
        self.startDateLive = startDateLive
        self.channelName = channelName
    }
}

public struct VideoPathAndKey: Codable, Equatable, Hashable {
    public var videoPath: String
    public var videoKey: Int

    public init(videoPath: String, videoKey: Int) {
        self.videoPath = videoPath
        self.videoKey = videoKey
    }
}

public struct ChannelRecord: Codable, Equatable, Hashable {
    public var channelName: String
    public var videoPathAndKeyList: [VideoPathAndKey]

    public init(channelName: String, videoPathAndKeyList: [VideoPathAndKey]) {
        self.channelName = channelName
        self.videoPathAndKeyList = videoPathAndKeyList
    }
}

public struct VideoPathRecord: Codable, Equatable, Hashable {
    public var path: String
    public var isAFolder: Bool

    public init(path: String, isAFolder: Bool) {
        self.path = path
        self.isAFolder = isAFolder
    }
}

public struct SystemInfoDto: Codable {
    public var infoName: String
    public var percentageUsed: Double
    public var available: String
    public var total: String
    public var used: String
    public var usageHistory: [Double]?

    public init(infoName: String, percentageUsed: Double, available: String, total: String, used: String, usageHistory: [Double]? = nil) {
        self.infoName = infoName
        self.percentageUsed = percentageUsed
        self.available = available
        self.total = total
        self.used = used
        self.usageHistory = usageHistory
    }
}
