import Foundation

/// Tutti gli importi dell'app sono centesimi interi: niente errori di arrotondamento
/// tra quello che si vede e quello che si somma.
enum Money {
    /// 2230 → "€22,30", 123450 → "€1.234,50".
    static func format(_ cents: Int, symbol: Bool = true) -> String {
        let sign = cents < 0 ? "−" : ""
        let value = abs(cents)
        return sign + (symbol ? "€" : "") + grouped(value / 100) + "," + String(format: "%02d", value % 100)
    }

    /// Per i campi di testo: 1200 → "12", 1250 → "12,50".
    static func editable(_ cents: Int) -> String {
        let value = abs(cents)
        let whole = (cents < 0 ? "-" : "") + String(value / 100)
        return value % 100 == 0 ? whole : whole + "," + String(format: "%02d", value % 100)
    }

    /// Legge quello che una persona scrive: "12", "12,5", "12.50", "€ 1.234,50".
    static func parse(_ text: String) -> Int? {
        var t = text.replacingOccurrences(of: "€", with: "")
            .replacingOccurrences(of: " ", with: "")
            .replacingOccurrences(of: "\u{00A0}", with: "")
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
            if tail.count == 3, !head.isEmpty, head.filter(\.isNumber).count <= 3 || head.contains(where: { $0 == "," || $0 == "." }) {
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

    /// Filtra quello che si digita in un campo importo: cifre e un solo separatore, al massimo due decimali.
    static func sanitizeInput(_ text: String) -> String {
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
            } else if (ch == "," || ch == "."), !seenSeparator {
                seenSeparator = true
                out.append(out.isEmpty ? "0," : ",")
            }
        }
        return out
    }

    private static func grouped(_ n: Int) -> String {
        let digits = Array(String(n))
        var out = ""
        for (i, ch) in digits.enumerated() {
            if i > 0 && (digits.count - i) % 3 == 0 { out.append(".") }
            out.append(ch)
        }
        return out
    }
}

enum DateText {
    private static let formatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.setLocalizedDateFormatFromTemplate("d MMM yyyy")
        return f
    }()

    /// "24 set 2026"
    static func short(_ date: Date) -> String {
        formatter.string(from: date).replacingOccurrences(of: ".", with: "")
    }
}
