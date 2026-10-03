import XCTest
import Observation
@testable import Receipty

/// Le traduzioni: nessuna chiave dimenticata, nessun segnaposto perso per strada.
/// Un `%@` in meno in una lingua non si vede compilando, ma a schermo manca un importo.
final class LocalizationTests: XCTestCase {

    private func table(_ name: String, _ localization: String) throws -> [String: String] {
        let url = try XCTUnwrap(Bundle.main.url(forResource: name, withExtension: "strings",
                                                subdirectory: nil, localization: localization),
                                "manca \(name).strings in \(localization).lproj")
        return try XCTUnwrap(NSDictionary(contentsOf: url) as? [String: String])
    }

    private func placeholders(_ text: String) throws -> [String] {
        let pattern = try Regex(#"%(?:\d+\$)?[@d]"#)
        return text.matches(of: pattern).map { String(text[$0.range]) }.sorted()
    }

    func testEveryLanguageTranslatesEveryKey() throws {
        let english = try table("Localizable", "en")
        XCTAssertGreaterThan(english.count, 150)

        for language in AppLanguage.allCases where language != .system {
            let translated = try table("Localizable", language.rawValue)
            XCTAssertEqual(Set(translated.keys), Set(english.keys),
                           "\(language.rawValue): chiavi diverse dall'inglese")
            for (key, base) in english {
                let text = try XCTUnwrap(translated[key], "\(language.rawValue): manca \(key)")
                XCTAssertFalse(text.trimmingCharacters(in: .whitespaces).isEmpty,
                               "\(language.rawValue): \(key) è vuota")
                XCTAssertEqual(try placeholders(text), try placeholders(base),
                               "\(language.rawValue): segnaposto diversi in \(key)")
            }
        }
    }

    func testCameraPermissionIsTranslated() throws {
        for language in AppLanguage.allCases where language != .system {
            let info = try table("InfoPlist", language.rawValue)
            XCTAssertNotNil(info["NSCameraUsageDescription"], language.rawValue)
        }
    }

    func testSwitchingLanguageChangesTheStrings() {
        let original = I18n.shared.language
        defer { I18n.shared.language = original }

        I18n.shared.language = .italian
        XCTAssertEqual(t("common.delete"), "Elimina")
        XCTAssertEqual(t("review.caption.many", 3), "3 voci")

        I18n.shared.language = .japanese
        XCTAssertEqual(t("common.delete"), "削除")

        I18n.shared.language = .english
        XCTAssertEqual(t("common.delete"), "Delete")
    }

    /// Cambiare lingua deve ridisegnare le schermate già aperte, senza riavviare l'app.
    /// SwiftUI valuta ogni `body` dentro `withObservationTracking`: se la lettura che fa `t(_:)`
    /// registra la dipendenza qui, la registra anche là.
    func testChangingLanguageInvalidatesViewsThatCallT() {
        let original = I18n.shared.language
        defer { I18n.shared.language = original }
        I18n.shared.language = .english

        let redrawn = expectation(description: "le viste che chiamano t() si ridisegnano")
        withObservationTracking {
            _ = t("common.done")
        } onChange: {
            redrawn.fulfill()
        }

        I18n.shared.language = .italian
        wait(for: [redrawn], timeout: 1)
        XCTAssertEqual(t("common.done"), "Fatto")
    }

    /// Una chiave che non esiste si vede (resta la chiave), non diventa una stringa vuota.
    func testUnknownKeyFallsBackToItself() {
        let original = I18n.shared.language
        defer { I18n.shared.language = original }
        I18n.shared.language = .german
        XCTAssertEqual(t("chiave.inventata"), "chiave.inventata")
    }

    /// Gli importi e il separatore decimale seguono la lingua scelta.
    func testAmountsFollowTheLanguage() {
        let original = I18n.shared.language
        defer { I18n.shared.language = original }
        let eur = Currency(code: "EUR")

        I18n.shared.language = .english
        XCTAssertEqual(Money.format(2230, eur), "€22.30")
        XCTAssertEqual(Money.editable(1250), "12.50")

        I18n.shared.language = .italian
        // Lo spazio prima del simbolo è indivisibile: l'importo non va a capo a metà.
        XCTAssertEqual(Money.format(2230, eur), "22,30\u{00A0}€")
        XCTAssertEqual(Money.editable(1250), "12,50")
        XCTAssertEqual(Money.sanitizeInput("12.5"), "12,5")
        // Scritto con il punto o con la virgola, il valore letto è lo stesso.
        XCTAssertEqual(Money.parse("12,50"), 1250)
        XCTAssertEqual(Money.parse("12.50"), 1250)
    }

    /// Il simbolo resta corto in ogni lingua: finisce in cerchietti da 36 punti.
    func testCurrencySymbolDoesNotFollowTheLanguage() {
        let original = I18n.shared.language
        defer { I18n.shared.language = original }
        I18n.shared.language = .italian
        XCTAssertEqual(Currency(code: "USD").symbol, "$")
        XCTAssertEqual(Currency(code: "EUR").symbol, "€")
    }
}
