import Foundation
import Observation

/// Le lingue dell'app. `system` segue quella dell'iPhone, le altre la forzano.
///
/// Il `rawValue` è anche il nome della cartella `.lproj` dentro il bundle.
enum AppLanguage: String, CaseIterable, Identifiable {
    case system
    case english = "en"
    case italian = "it"
    case spanish = "es"
    case french = "fr"
    case german = "de"
    case portuguese = "pt-BR"
    case dutch = "nl"
    case turkish = "tr"
    case russian = "ru"
    case japanese = "ja"
    case korean = "ko"
    case chinese = "zh-Hans"

    var id: String { rawValue }

    /// Il nome della lingua scritto in quella lingua: si riconosce anche senza capire l'inglese.
    var nativeName: String {
        switch self {
        case .system: t("language.system")
        case .english: "English"
        case .italian: "Italiano"
        case .spanish: "Español"
        case .french: "Français"
        case .german: "Deutsch"
        case .portuguese: "Português (Brasil)"
        case .dutch: "Nederlands"
        case .turkish: "Türkçe"
        case .russian: "Русский"
        case .japanese: "日本語"
        case .korean: "한국어"
        case .chinese: "简体中文"
        }
    }

    /// Il nome della lingua nella lingua in uso ("Japanese" se l'app è in inglese).
    /// Per `system`, la lingua che l'iPhone ha scelto.
    var localizedName: String? {
        let code = self == .system ? Bundle.main.preferredLocalizations.first : rawValue
        guard let code else { return nil }
        let name = I18n.shared.locale.localizedString(forIdentifier: code)
            ?? Locale(identifier: code).localizedString(forIdentifier: code)
        guard let name, name.lowercased() != nativeName.lowercased() else { return nil }
        return name.prefix(1).uppercased() + name.dropFirst()
    }
}

/// La lingua dell'interfaccia. Le stringhe non passano per `Bundle.main` ma per il bundle
/// della lingua scelta: così cambiarla si vede subito, senza riavviare l'app.
///
/// È `@Observable` e `t(_:)` legge `bundle`: ogni `body` che chiama `t` si ridisegna da sé
/// quando la lingua cambia.
@Observable
final class I18n {
    static let shared = I18n()
    static let defaultsKey = "language"

    /// Il bundle da cui arrivano le stringhe (`Resources/<lingua>.lproj`).
    private(set) var bundle: Bundle
    /// La lingua con cui si formattano importi e date.
    private(set) var locale: Locale

    var language: AppLanguage {
        didSet {
            guard language != oldValue else { return }
            persist()
            (bundle, locale) = Self.resolve(language)
        }
    }

    private init() {
        let saved = UserDefaults.standard.string(forKey: Self.defaultsKey)
        let language = saved.flatMap(AppLanguage.init(rawValue:)) ?? .system
        self.language = language
        (bundle, locale) = Self.resolve(language)
    }

    private static func resolve(_ language: AppLanguage) -> (Bundle, Locale) {
        guard language != .system else { return (.main, .current) }
        let bundle = lproj(language.rawValue) ?? .main
        // "pt-BR" → "pt_BR", come vuole Locale.
        return (bundle, Locale(identifier: language.rawValue.replacingOccurrences(of: "-", with: "_")))
    }

    private static func lproj(_ code: String) -> Bundle? {
        Bundle.main.path(forResource: code, ofType: "lproj").flatMap(Bundle.init(path:))
    }

    /// L'inglese: la rete di sicurezza se una chiave manca nella lingua scelta.
    private static let fallback = lproj("en") ?? .main

    private func persist() {
        let defaults = UserDefaults.standard
        defaults.set(language.rawValue, forKey: Self.defaultsKey)
        // Tastiera, selettore foto, Safari e gli avvisi di sistema leggono questa: la seguono
        // dalla prossima apertura dell'app.
        if language == .system {
            defaults.removeObject(forKey: "AppleLanguages")
        } else {
            defaults.set([language.rawValue], forKey: "AppleLanguages")
        }
    }

    /// Una stringa tradotta; se manca nella lingua scelta si ripiega sull'inglese, poi sulla chiave.
    func string(_ key: String) -> String {
        let text = bundle.localizedString(forKey: key, value: Self.sentinel, table: nil)
        guard text == Self.sentinel else { return text }
        let english = Self.fallback.localizedString(forKey: key, value: Self.sentinel, table: nil)
        return english == Self.sentinel ? key : english
    }

    private static let sentinel = "\u{0}?"

    /// Il separatore decimale della lingua in uso: i campi importo scrivono questo.
    var decimalSeparator: String { locale.decimalSeparator ?? "." }
}

/// La stringa tradotta per `key`. Chiamata da un `body` SwiftUI, lo rende dipendente
/// dalla lingua: cambiarla ridisegna la schermata.
func t(_ key: String) -> String {
    I18n.shared.string(key)
}

/// Come `t(_:)`, con i segnaposto (`%@`, `%d`, `%1$@`…) riempiti.
func t(_ key: String, _ arguments: any CVarArg...) -> String {
    String(format: I18n.shared.string(key), locale: I18n.shared.locale, arguments: arguments)
}
