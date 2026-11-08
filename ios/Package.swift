// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "SnakeEscape",
    defaultLocalization: "en",
    platforms: [
        .iOS(.v16),
        .macOS(.v13)
    ],
    products: [
        .library(name: "SnakeEscape", targets: ["SnakeEscape"])
    ],
    targets: [
        .target(
            name: "SnakeEscape",
            path: "Sources/SnakeEscape"
        ),
        .testTarget(
            name: "SnakeEscapeTests",
            dependencies: ["SnakeEscape"],
            path: "Tests/SnakeEscapeTests"
        )
    ]
)
