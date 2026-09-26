#!/bin/bash

# Exit on any error
set -e

echo "Building OrbisStream for Mac Silicon (arm64)..."

# Ensure we are in the project root containing the 'apple' folder
if [ ! -d "apple" ]; then
    echo "Error: Run this script from the root of the project where 'apple' directory exists."
    exit 1
fi

APP_NAME="OrbisStream"
APP_DIR="build_mac/${APP_NAME}.app"
CONTENTS_DIR="${APP_DIR}/Contents"
MACOS_DIR="${CONTENTS_DIR}/MacOS"
RESOURCES_DIR="${CONTENTS_DIR}/Resources"
DMG_NAME="${APP_NAME}_Mac_Silicon.dmg"
ICON_SOURCE="apple/Sources/OrbisStream/Resources/lazy-icon.png"

# Clean previous build
rm -rf build_mac
mkdir -p build_mac

echo "Compiling Swift Package..."
cd apple
swift build -c release --arch arm64
cd ..

# Get the path of the built executable
EXECUTABLE_PATH="apple/.build/arm64-apple-macosx/release/${APP_NAME}"

if [ ! -f "${EXECUTABLE_PATH}" ]; then
    echo "Error: Executable not found at ${EXECUTABLE_PATH}. Build failed?"
    exit 1
fi

echo "Creating Application Bundle..."
mkdir -p "${MACOS_DIR}"
mkdir -p "${RESOURCES_DIR}"

# Copy the executable
cp "${EXECUTABLE_PATH}" "${MACOS_DIR}/"

# Generate Icon (.icns) using macOS native tools
echo "Generating AppIcon.icns..."
ICONSET_DIR="build_mac/AppIcon.iconset"
mkdir -p "${ICONSET_DIR}"

if command -v sips &> /dev/null && command -v iconutil &> /dev/null; then
    sips -z 16 16     "${ICON_SOURCE}" --out "${ICONSET_DIR}/icon_16x16.png" > /dev/null
    sips -z 32 32     "${ICON_SOURCE}" --out "${ICONSET_DIR}/icon_16x16@2x.png" > /dev/null
    sips -z 32 32     "${ICON_SOURCE}" --out "${ICONSET_DIR}/icon_32x32.png" > /dev/null
    sips -z 64 64     "${ICON_SOURCE}" --out "${ICONSET_DIR}/icon_32x32@2x.png" > /dev/null
    sips -z 128 128   "${ICON_SOURCE}" --out "${ICONSET_DIR}/icon_128x128.png" > /dev/null
    sips -z 256 256   "${ICON_SOURCE}" --out "${ICONSET_DIR}/icon_128x128@2x.png" > /dev/null
    sips -z 256 256   "${ICON_SOURCE}" --out "${ICONSET_DIR}/icon_256x256.png" > /dev/null
    sips -z 512 512   "${ICON_SOURCE}" --out "${ICONSET_DIR}/icon_256x256@2x.png" > /dev/null
    sips -z 512 512   "${ICON_SOURCE}" --out "${ICONSET_DIR}/icon_512x512.png" > /dev/null
    sips -z 1024 1024 "${ICON_SOURCE}" --out "${ICONSET_DIR}/icon_512x512@2x.png" > /dev/null

    iconutil -c icns "${ICONSET_DIR}" -o "${RESOURCES_DIR}/AppIcon.icns"
else
    echo "Warning: 'sips' or 'iconutil' not found. Skipping .icns generation."
    echo "Make sure to run this on macOS to generate the proper icon."
fi

# Copy any resources if available (Localizable files, etc)
cp -R apple/Sources/OrbisStream/Resources/* "${RESOURCES_DIR}/" || true

# Remove Swift source files that might have been copied
find "${RESOURCES_DIR}" -name "*.swift" -type f -delete || true

# Create Info.plist
echo "Creating Info.plist..."
cat <<EOF > "${CONTENTS_DIR}/Info.plist"
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>en</string>
    <key>CFBundleExecutable</key>
    <string>${APP_NAME}</string>
    <key>CFBundleIconFile</key>
    <string>AppIcon</string>
    <key>CFBundleIdentifier</key>
    <string>com.orbis.stream</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>${APP_NAME}</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>LSMinimumSystemVersion</key>
    <string>13.0</string>
    <key>NSHighResolutionCapable</key>
    <true/>
</dict>
</plist>
EOF

# Create DMG
if command -v hdiutil &> /dev/null; then
    echo "Creating DMG..."
    rm -f "${DMG_NAME}"
    hdiutil create -volname "${APP_NAME}" -srcfolder "build_mac" -ov -format UDZO "${DMG_NAME}"
    echo "Done! Installer created at ${DMG_NAME}"
else
    echo "Warning: 'hdiutil' not found. Skipping DMG creation."
    echo "Application Bundle created at ${APP_DIR}. Use a Mac to create the final DMG."
fi
