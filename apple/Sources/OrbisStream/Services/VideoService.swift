import Foundation

public final class VideoService {
    public static let shared = VideoService()
    private let db = DatabaseManager.shared

    private init() {}

    public func getAllVideos() -> [Video] {
        return db.getAllVideos()
    }

    public func scanFolderForVideos(folderPath: String, channelName: String) -> [Video] {
        let fm = FileManager.default
        guard let files = try? fm.contentsOfDirectory(atPath: folderPath) else { return [] }

        var addedVideos: [Video] = []
        for file in files {
            let path = (folderPath as NSString).appendingPathComponent(file)
            let ext = (file as NSString).pathExtension.lowercased()
            if VideoExtensionEnum.isVideoExtensionPresent(ext) {
                let videoName = (file as NSString).deletingPathExtension
                let v = Video(
                    name: videoName,
                    videoPath: path,
                    extensionName: ext,
                    liveStatus: .offline,
                    channelName: channelName
                )
                let saved = db.saveVideo(v)
                addedVideos.append(saved)
            }
        }
        return addedVideos
    }

    public func getAllChannelsWithVideoLive() -> [ChannelRecord] {
        let settings = db.getAllSettings()
        let videos = db.getAllVideos()

        var channelsMap: [String: [VideoPathAndKey]] = [:]
        for setting in settings {
            channelsMap[setting.channelName] = []
        }

        for video in videos {
            if let pkid = video.pkid {
                let item = VideoPathAndKey(videoPath: video.videoPath, videoKey: pkid)
                channelsMap[video.channelName, default: []].append(item)
            }
        }

        return channelsMap.map { ChannelRecord(channelName: $0.key, videoPathAndKeyList: $0.value) }
    }
}
