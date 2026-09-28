#!/bin/zsh
# Genera il progetto, compila e lancia sul simulatore.
# Uso: ./build.sh [UDID]   (default: primo iPhone avviato, altrimenti iPhone 16, 393×852 come il mockup)
set -e
cd "$(dirname "$0")"
UDID=${1:-$(xcrun simctl list devices booted | grep -oE '[0-9A-F-]{36}' | head -1)}
UDID=${UDID:-B27B1E2E-258E-46BC-8613-2E1E6CF30217}
# DerivedData fuori dal Desktop: iCloud aggiunge attributi che fanno fallire la firma.
DD=~/Library/Developer/Xcode/DerivedData/WhoPays
xcodegen generate -q
xcodebuild -project WhoPays.xcodeproj -scheme WhoPays -sdk iphonesimulator \
  -destination "id=$UDID" -derivedDataPath $DD build -quiet
xcrun simctl boot $UDID 2>/dev/null || true
xcrun simctl install $UDID $DD/Build/Products/Debug-iphonesimulator/WhoPays.app
xcrun simctl launch $UDID com.cldnpl.whopays
