# Receipty for Android

The same app as the iOS one, in Kotlin and Jetpack Compose: same screens, same flow, same math.
Open this `android` folder in Android Studio and run the `app` configuration.

- **Same logic.** `SettlementEngine`, `Money`, `Currency` and `ReceiptParser` are line-by-line ports
  of the Swift code; the unit tests in `app/src/test` are the iOS tests, with the same cases.
- **Same strings.** `app/src/main/assets/i18n/<language>.strings` are copies of the iOS
  `Localizable.strings`, read at runtime through `t("key")` so the language changes on the spot.
  `assets/i18n/android/` overrides the few lines that say "iPhone". When a translation changes in
  `Resources/`, copy the file here too (`LocalizationTests` checks keys and placeholders).
- **OCR.** ML Kit text recognition with the model bundled in the app: no network, like Apple Vision.
  `app/src/androidTest/ReceiptReaderTests` runs the iOS fixture photo through it on a device.
- **Debug screens.** As with `-screen` on iOS, a debug build opens a screen with mockup data:
  `adb shell am start -n com.cldnpl.whopays/com.cldnpl.receipty.MainActivity --es screen review`
  (`sheet`, `scan`, `review`, `people`, `assign`, `payments`, `manual`, `result`, `change`,
  `changeresult`, `settings`, `currency`, `language`, `tutorial`, `onboarding`, `empty`).

Application id `com.cldnpl.whopays`, version 1.3, Android 8.0+.

When the project sits in an iCloud-synced Desktop or Documents folder, Gradle writes its build
output to `~/.gradle-builds/Receipty` instead: iCloud duplicates generated files ("file 2.xml")
and breaks the build, the same reason `build.sh` keeps DerivedData off the Desktop.
