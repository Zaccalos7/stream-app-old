import Foundation

public final class SettingService {
    public static let shared = SettingService()
    private let db = DatabaseManager.shared

    private init() {}

    public func saveSettings(_ record: SettingRecord) -> Setting {
        return db.saveSetting(record)
    }

    public func changeSettings(id: Int, record: SettingRecord) -> Setting? {
        return db.updateSetting(id: id, settingRecord: record)
    }

    public func retrieveSettings(channelFilter: String? = nil) -> [Setting] {
        let all = db.getAllSettings()
        if let channelFilter = channelFilter, !channelFilter.isEmpty {
            return all.filter { $0.channelName == channelFilter }
        }
        return all
    }

    public func retrieveChannels() -> Set<String> {
        return db.getAllChannels()
    }

    public func deleteSetting(id: Int) -> Bool {
        return db.deleteSetting(id: id)
    }

    public func retrieveDirectoriesSettingsPath() -> [VideoPathRecord] {
        let all = db.getAllSettings()
        var pathRecords: [VideoPathRecord] = []
        let fm = FileManager.default

        for setting in all {
            let path = setting.videoFolder
            var isDir: ObjCBool = false
            let exists = fm.fileExists(atPath: path, isDirectory: &isDir)
            pathRecords.append(VideoPathRecord(path: path, isAFolder: exists && isDir.boolValue))
        }
        return pathRecords
    }
}
