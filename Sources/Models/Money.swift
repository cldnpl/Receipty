import Foundation

/// Tutti gli importi dell'app sono centesimi interi: niente errori di arrotondamento
/// tra quello che si vede e quello che si somma.
enum Money {
    /// Per i campi di testo: 1200 → "12", 1250 → "12.50".
    static func editable(_ cents: Int) -> String {
        let value = abs(cents)
        let whole = (cents < 0 ? "-" : "") + String(value / 100)
        return value % 100 == 0 ? whole : whole + "." + String(format: "%02d", value % 100)
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
    /// punto, qualunque sia la tastiera), al massimo due decimali. Niente decimali per yen & co.
    static func sanitizeInput(_ text: String, decimals allowDecimals: Bool = true) -> String {
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
                out.append(out.isEmpty ? "0." : ".")
            }
        }
        return out
    }
}

enum DateText {
    private static let formatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "d MMM yyyy"
        return f
    }()

    /// "24 Sep 2026"
    static func short(_ date: Date) -> String {
        formatter.string(from: date)
    }
}
