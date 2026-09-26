// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "OrbisStream",
    defaultLocalization: "en",
    platforms: [
        .macOS(.v13)
    ],
    products: [
        .executable(name: "OrbisStream", targets: ["OrbisStream"])
    ],
    dependencies: [],
    targets: [
        .executableTarget(
            name: "OrbisStream",
            dependencies: [],
            path: "Sources/OrbisStream",
            resources: [
                .process("Resources")
            ]
        ),
        .testTarget(
            name: "OrbisStreamTests",
            dependencies: ["OrbisStream"],
            path: "Tests/OrbisStreamTests"
        )
    ]
)
