import Foundation

public enum LiveStatusEnum: String, Codable, CaseIterable {
    case live = "LIVE"
    case offline = "OFFLINE"
    case ended = "ENDED"
    case error = "ERROR"
    case stopped = "STOPPED"

    public var value: String {
        return self.rawValue
    }
}

public enum SystemInfoEnum: String, Codable, CaseIterable {
    case cpu = "CPU"
    case ram = "RAM"
    case swap = "SWAP"
    case cpuTemperature = "TEMPERATURA CPU"

    public var infoName: String {
        return self.rawValue
    }
}

public enum VideoExtensionEnum: String, Codable, CaseIterable {
    case mp4 = "mp4"
    case flv = "flv"
    case mov = "mov"
    case webm = "webm"
    case vp9 = "vp9"
    case mkv = "mkv"

    public var videoExtension: String {
        return self.rawValue
    }

    public static func isVideoExtensionPresent(_ extensionStr: String?) -> Bool {
        guard let ext = extensionStr?.lowercased() else { return false }
        return VideoExtensionEnum.allCases.contains { $0.rawValue.lowercased() == ext }
    }
}
