import Foundation

public final class VideoSettingService {
    public static let shared = VideoSettingService()
    private let db = DatabaseManager.shared

    private init() {}

    public func getAllVideoSettings() -> [VideoSetting] {
        return db.getAllVideoSettings()
    }

    public func saveVideoSetting(_ record: VideoSettingsRecord) -> VideoSetting {
        return db.saveVideoSetting(record)
    }

    public func deleteVideoSetting(id: Int) -> Bool {
        return db.deleteVideoSetting(id: id)
    }
}
