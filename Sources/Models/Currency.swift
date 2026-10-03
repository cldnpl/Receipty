import SwiftUI

/// La valuta di un conto. Ogni conto si ricorda la sua: cambiare valuta nelle impostazioni
/// vale per i conti nuovi, non riscrive quelli di un viaggio passato.
struct Currency: Hashable, Identifiable {
    let code: String
    var id: String { code }

    init(code: String) {
        self.code = code.uppercased()
    }

    /// "Euro", "Japanese Yen", "Franco svizzero"…: il nome nella lingua dell'app.
    /// L'iniziale maiuscola la mettiamo noi: in italiano e francese iOS li dà minuscoli,
    /// e in un elenco di righe stonano.
    var name: String {
        guard let name = I18n.shared.locale.localizedString(forCurrencyCode: code) else { return code }
        return name.prefix(1).uppercased() + name.dropFirst()
    }

    /// Il simbolo non segue la lingua: serve corto, come glifo nei cerchietti e davanti ai campi
    /// ("$", non "USD").
    var symbol: String { Self.formatter(code, Self.symbolLocale).currencySymbol ?? code }

    /// 2 per euro e dollaro, 0 per yen, won, fiorino islandese...
    var minorDigits: Int { Self.formatter(code, Self.symbolLocale).maximumFractionDigits }

    /// Il passo di arrotondamento in centesimi: 1 centesimo, oppure 1 unità intera (100).
    var unit: Int { minorDigits == 0 ? 100 : 1 }

    /// Per scrivere un importo intero: "€22.30" in inglese, "22,30 €" in italiano.
    fileprivate var localizedFormatter: NumberFormatter {
        Self.formatter(code, I18n.shared.locale)
    }

    private static let symbolLocale = Locale(identifier: "en_US")

    private static func formatter(_ code: String, _ locale: Locale) -> NumberFormatter {
        let key = "\(code)|\(locale.identifier)"
        if let cached = cache[key] { return cached }
        let f = NumberFormatter()
        f.numberStyle = .currency
        f.locale = locale
        f.currencyCode = code
        cache[key] = f
        return f
    }

    private static var cache: [String: NumberFormatter] = [:]

    // MARK: Elenco

    /// Le valute proposte: le più usate in viaggio e nel mondo, con 0 o 2 decimali
    /// (dinaro kuwaitiano & co. a tre decimali restano fuori: i conti sono in centesimi).
    private static let codes = [
        "EUR", "USD", "GBP", "CHF", "JPY", "CNY", "CAD", "AUD", "NZD", "HKD", "SGD", "INR",
        "KRW", "SEK", "NOK", "DKK", "ISK", "PLN", "CZK", "HUF", "RON", "BGN", "RSD", "ALL",
        "MKD", "BAM", "MDL", "UAH", "GEL", "AMD", "AZN", "TRY", "ILS", "AED", "SAR", "QAR",
        "EGP", "MAD", "ZAR", "NGN", "KES", "GHS", "TZS", "UGX", "BRL", "MXN", "ARS", "CLP",
        "COP", "PEN", "UYU", "CRC", "DOP", "THB", "VND", "IDR", "MYR", "PHP", "TWD", "PKR",
        "BDT", "LKR", "NPR", "KZT", "UZS", "MNT", "KHR", "LAK",
    ]

    /// In ordine alfabetico per nome, quindi un ordine diverso in ogni lingua.
    static var all: [Currency] {
        let locale = I18n.shared.locale
        if let sorted, sorted.locale == locale.identifier { return sorted.list }
        let list = codes.map(Currency.init(code:)).sorted {
            $0.name.compare($1.name, options: [.caseInsensitive, .diacriticInsensitive],
                            range: nil, locale: locale) == .orderedAscending
        }
        sorted = (locale.identifier, list)
        return list
    }

    private static var sorted: (locale: String, list: [Currency])?

    static let defaultsKey = "currency"

    /// La valuta scelta nelle impostazioni, o quella del telefono, o l'euro.
    static var current: Currency {
        if let saved = UserDefaults.standard.string(forKey: defaultsKey) { return Currency(code: saved) }
        return deviceDefault
    }

    static var deviceDefault: Currency {
        let code = Locale.current.currency?.identifier ?? "EUR"
        return all.contains { $0.code == code } ? Currency(code: code) : Currency(code: "EUR")
    }
}

extension Currency: Codable {
    init(from decoder: Decoder) throws {
        self.init(code: try decoder.singleValueContainer().decode(String.self))
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.singleValueContainer()
        try c.encode(code)
    }
}

extension Money {
    /// 2230 → "€22.30", "22,30 €", "¥2,230": nella lingua dell'app
    /// (per lo yen i centesimi sono già unità intere × 100).
    static func format(_ cents: Int, _ currency: Currency) -> String {
        let value = NSDecimalNumber(value: cents).dividing(by: 100)
        let text = currency.localizedFormatter.string(from: value) ?? currency.symbol + editable(cents)
        return text.replacingOccurrences(of: "-", with: "−")
    }

    /// Zero scritto corto, come nel mockup: "€0".
    static func formatCompact(_ cents: Int, _ currency: Currency) -> String {
        cents == 0 ? currency.symbol + "0" : format(cents, currency)
    }
}

private struct CurrencyKey: EnvironmentKey {
    static var defaultValue: Currency { .current }
}

extension EnvironmentValues {
    /// La valuta del conto mostrato: la imposta RootView per il percorso in corso o per il conto salvato.
    var currency: Currency {
        get { self[CurrencyKey.self] }
        set { self[CurrencyKey.self] = newValue }
    }
}
