import Foundation

public final class ImageService {
    public static let shared = ImageService()

    private let imagesDirectory: URL

    private init() {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        imagesDirectory = docs.appendingPathComponent("OrbisStreamImages")
        try? FileManager.default.createDirectory(at: imagesDirectory, withIntermediateDirectories: true)
    }

    public func saveImage(data: Data, filename: String) throws -> URL {
        let fileURL = imagesDirectory.appendingPathComponent(filename)
        try data.write(to: fileURL)
        return fileURL
    }

    public func loadImage(filename: String) -> Data? {
        let fileURL = imagesDirectory.appendingPathComponent(filename)
        return try? Data(contentsOf: fileURL)
    }
}
