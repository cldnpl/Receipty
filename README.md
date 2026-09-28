# WhoPays

A small iOS app that answers one question at the end of a dinner: **who pays whom, and how much?**

- **Scan the receipt.** WhoPays reads the items and prices on the phone (Apple Vision, no network), flags anything it is unsure about, and lets you tap who had what. Shared items are split automatically.
- **Or type the total.** Enter what the bill came to and who paid; it is split equally.
- **Change handled.** If more money went on the table than the bill, WhoPays works out who keeps the change and what is still owed after it is handed out.
- **Fewest transfers.** Settlements use the minimum number of payments between people.

No account, no ads, no tracking: bills stay on the device.

## Legal

- [Privacy Policy](PRIVACY_POLICY.md)
- [End User License Agreement](EULA.md)

## Building

The Xcode project is generated with [XcodeGen](https://github.com/yonaskolb/XcodeGen):

```bash
xcodegen generate
open WhoPays.xcodeproj
```

`./build.sh [simulator UDID]` generates, builds and launches the app on a simulator. Tests: `xcodebuild -scheme WhoPays test -destination 'platform=iOS Simulator,name=iPhone 16'`.

SwiftUI, iOS 17+. The Inter typeface is bundled under the SIL Open Font License (`Resources/Fonts/Inter-LICENSE.txt`).
