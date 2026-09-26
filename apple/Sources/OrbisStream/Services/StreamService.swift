import Foundation

public final class StreamService {
    public static let shared = StreamService()
    private let db = DatabaseManager.shared

    // Track running ffmpeg processes by video pkid or stream key
    private var activeProcesses: [Int: Process] = [:]
    private let lock = NSLock()

    private init() {}

    public func startLive(record: StartLiveRecord) -> (success: Bool, message: String, liveHistoryPkid: Int64?) {
        lock.lock()
        defer { lock.unlock() }

        let history = VideoLiveHistory(
            folderOfVideoToStream: record.videoPath,
            localDateTimeStartLive: Date(),
            streamUrl: record.streamUrl,
            streamKey: record.streamKey,
            platformStreamName: record.platformStreamName,
            userName: "DefaultUser"
        )
        let savedHistory = db.saveLiveHistory(history)

        // Spawn FFmpeg streaming process in background
        let process = Process()
        process.executableURL = URL(fileURLWithPath: "/usr/bin/env")

        // Construct ffmpeg command args
        // ffmpeg -re -i <videoPath> -c:v libx264 -preset veryfast -maxrate 3000k -bufsize 6000k -pix_fmt yuv420p -g 50 -c:a aac -b:a 128k -ar 44100 -f flv <streamUrl/streamKey>
        let rtmpUrl = record.streamUrl.hasSuffix("/") ? "\(record.streamUrl)\(record.streamKey)" : "\(record.streamUrl)/\(record.streamKey)"

        var arguments = ["ffmpeg", "-re", "-i", record.videoPath]

        if let vSettings = record.videoSettingsRecord {
            if let codecName = vSettings.videoCodecName, !codecName.isEmpty {
                arguments.append(contentsOf: ["-c:v", codecName])
            } else {
                arguments.append(contentsOf: ["-c:v", "libx264"])
            }

            if let options = vSettings.videoOptions {
                for opt in options {
                    if let k = opt.key, let v = opt.value {
                        arguments.append(contentsOf: ["-\(k)", v])
                    }
                }
            }

            if let gop = vSettings.gopSize {
                arguments.append(contentsOf: ["-g", "\(gop * 25)"])
            }

            if let aSetting = vSettings.audioSettingRecord {
                arguments.append(contentsOf: ["-c:a", "aac"])
                if let bitrate = aSetting.audioBitrate {
                    arguments.append(contentsOf: ["-b:a", "\(bitrate)"])
                }
            } else {
                arguments.append(contentsOf: ["-c:a", "aac", "-b:a", "128k"])
            }
        } else {
            arguments.append(contentsOf: [
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-maxrate", "3000k",
                "-bufsize", "6000k",
                "-pix_fmt", "yuv420p",
                "-g", "50",
                "-c:a", "aac",
                "-b:a", "128k",
                "-ar", "44100"
            ])
        }

        arguments.append(contentsOf: ["-f", "flv", rtmpUrl])
        process.arguments = arguments

        do {
            try process.run()
            if let pkid = savedHistory.pkid {
                activeProcesses[Int(pkid)] = process
            }
            return (true, "live.started", savedHistory.pkid)
        } catch {
            return (false, "streaming.error: \(error.localizedDescription)", savedHistory.pkid)
        }
    }

    public func stopVideoStreamingByPkid(_ videoLivePkid: Int) -> Bool {
        lock.lock()
        defer { lock.unlock() }

        if let process = activeProcesses[videoLivePkid] {
            process.terminate()
            activeProcesses.removeValue(forKey: videoLivePkid)
            return true
        }
        return false
    }

    public func startVideo(_ videoRecord: VideoRecord) -> (success: Bool, message: String) {
        let startRecord = StartLiveRecord(
            streamUrl: videoRecord.videoLiveHistory?.streamUrl ?? "",
            streamKey: videoRecord.videoLiveHistory?.streamKey ?? "",
            videoPath: videoRecord.videoPath,
            platformStreamName: videoRecord.videoLiveHistory?.platformStreamName ?? "Stream",
            channelName: videoRecord.channelName,
            videoSettingsRecord: videoRecord.videoSetting
        )
        let res = startLive(record: startRecord)
        return (res.success, res.message)
    }
}
