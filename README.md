# Receipty

A small iOS app that answers one question at the end of a dinner: **who pays whom, and how much?**

- **Scan the receipt.** Receipty reads the items and prices on the phone (Apple Vision, no network), flags anything it is unsure about, and lets you tap who had what. Shared items are split automatically.
- **Or type the total.** Enter what the bill came to and who paid; it is split equally.
- **Change handled.** If more money went on the table than the bill, Receipty works out who keeps the change and what is still owed after it is handed out.
- **Fewest transfers.** Settlements use the minimum number of payments between people.

- **In your language.** English, Italian, Spanish, French, German, Portuguese, Dutch, Turkish, Russian, Japanese, Korean and Simplified Chinese. The language follows the iPhone, or you pick one in Settings and it changes on the spot.

No account, no ads, no tracking: bills stay on the device.

## Legal

- [Privacy Policy](PRIVACY_POLICY.md)
- [End User License Agreement](EULA.md)

## Building

On the App Store as **Receipty: Split the Bill**. The bundle id keeps the original working name: `com.cldnpl.whopays`.


The Xcode project is generated with [XcodeGen](https://github.com/yonaskolb/XcodeGen):

```bash
xcodegen generate
open Receipty.xcodeproj
```

`./build.sh [simulator UDID]` generates, builds and launches the app on a simulator. Tests: `xcodebuild -scheme Receipty test -destination 'platform=iOS Simulator,name=iPhone 16'`.

SwiftUI, iOS 17+. The Inter typeface is bundled under the SIL Open Font License (`Resources/Fonts/Inter-LICENSE.txt`).

## Translations

One `Resources/<language>.lproj/Localizable.strings` per language, plus `InfoPlist.strings` for the
camera permission. Strings are read through `t("key")` (`Sources/Localization/Localization.swift`),
which goes to the bundle of the chosen language rather than to `Bundle.main`: that is what lets the
app switch language without a restart. Amounts, dates and currency names follow the same choice.

To add a language: copy `en.lproj`, translate it, and add a case to `AppLanguage`. `LocalizationTests`
fails if a key or a `%@` goes missing along the way.
