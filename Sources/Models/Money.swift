import Foundation

/// Tutti gli importi dell'app sono centesimi interi: niente errori di arrotondamento
/// tra quello che si vede e quello che si somma.
enum Money {
    /// Il separatore decimale della lingua dell'app: "." in inglese, "," in italiano.
    static var separator: String { I18n.shared.decimalSeparator }

    /// Per i campi di testo: 1200 → "12", 1250 → "12.50" (o "12,50").
    static func editable(_ cents: Int) -> String {
        let value = abs(cents)
        let whole = (cents < 0 ? "-" : "") + String(value / 100)
        return value % 100 == 0 ? whole : whole + separator + String(format: "%02d", value % 100)
    }

    /// 1250 → "12.50" / "12,50": quello che resta nel campo quando si smette di scrivere.
    static func editableFixed(_ cents: Int, decimals: Bool) -> String {
        decimals ? String(cents / 100) + separator + String(format: "%02d", cents % 100)
                 : String(cents / 100)
    }

    /// Legge quello che una persona scrive, con la virgola o col punto: "12", "12,5", "12.50", "€ 1,234.50".
    static func parse(_ text: String) -> Int? {
        var t = String(text.unicodeScalars.filter { !CharacterSet.whitespaces.contains($0) && $0.properties.generalCategory != .currencySymbol })
        guard !t.isEmpty else { return nil }
        var negative = false
        if t.hasPrefix("-") || t.hasPrefix("−") {
            negative = true
            t.removeFirst()
        }
        guard !t.isEmpty, t.allSatisfy({ $0.isASCII && ($0.isNumber || $0 == "," || $0 == ".") }) else { return nil }

        var euros = t
        var fraction = ""
        if let sep = t.lastIndex(where: { $0 == "," || $0 == "." }) {
            let tail = String(t[t.index(after: sep)...])
            let head = String(t[..<sep])
            if tail.count == 3, !head.isEmpty {
                // "1.234" o "1,234": separatore delle migliaia, non decimali.
                euros = head + tail
            } else if tail.count <= 2 {
                euros = head
                fraction = tail
            } else {
                return nil
            }
        }
        euros = euros.filter(\.isNumber)
        if euros.isEmpty && fraction.isEmpty { return nil }
        guard let e = Int(euros.isEmpty ? "0" : euros), e < 1_000_000 else { return nil }
        let f = Int(fraction.padding(toLength: 2, withPad: "0", startingAt: 0)) ?? 0
        let cents = e * 100 + f
        return negative ? -cents : cents
    }

    /// Filtra quello che si digita in un campo importo: cifre e un solo separatore (mostrato come
    /// quello della lingua in uso, qualunque sia la tastiera), al massimo due decimali.
    /// Niente decimali per yen & co.
    static func sanitizeInput(_ text: String, decimals allowDecimals: Bool = true) -> String {
        let separator = Self.separator
        var out = ""
        var seenSeparator = false
        var decimals = 0
        for ch in text {
            if ch.isASCII && ch.isNumber {
                if seenSeparator {
                    guard decimals < 2 else { continue }
                    decimals += 1
                }
                if out.count >= 7 && !seenSeparator { continue }
                out.append(ch)
            } else if allowDecimals, ch == "," || ch == ".", !seenSeparator {
                seenSeparator = true
                out += out.isEmpty ? "0" + separator : separator
            }
        }
        return out
    }
}

enum DateText {
    private static var cached: (locale: String, formatter: DateFormatter)?

    /// "24 Sep 2026", "24 set 2026", "2026年9月24日": giorno, mese e anno come si scrivono
    /// nella lingua dell'app.
    static func short(_ date: Date) -> String {
        let locale = I18n.shared.locale
        if cached?.locale != locale.identifier {
            let f = DateFormatter()
            f.locale = locale
            f.setLocalizedDateFormatFromTemplate("d MMM y")
            cached = (locale.identifier, f)
        }
        return cached!.formatter.string(from: date)
    }
}
