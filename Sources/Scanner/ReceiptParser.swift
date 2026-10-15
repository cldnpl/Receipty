import Foundation
import CoreGraphics

/// Un pezzo di testo riconosciuto dall'OCR, in coordinate pixel dell'immagine (origine in alto a sinistra).
struct OCRFragment: Hashable {
    var text: String
    var confidence: Float
    /// Le altre letture possibili, dalla più probabile.
    var alternatives: [String] = []
    var box: CGRect
    /// Inclinazione della riga di testo in radianti (0 = orizzontale).
    var angle: CGFloat = 0
}

struct ParsedReceipt {
    var items: [BillItem]
    /// Il TOTALE stampato, se c'è.
    var total: Int?
}

/// Trasforma il testo di uno scontrino in voci con prezzo.
///
/// Regola di fondo: nel dubbio non si indovina. Una voce di cui l'OCR non è sicuro
/// viene segnalata, e chi usa l'app la conferma prima di andare avanti.
enum ReceiptParser {
    /// Cifre decimali della valuta del conto: 2 di solito, 0 per yen, won & co.
    /// (TaskLocal: il parser gira in un task a parte, così non serve passarlo ovunque).
    @TaskLocal static var minorDigits = 2

    private static let isoCodes = Set(Locale.commonISOCurrencyCodes.map { $0.uppercased() })
    private static let localMarkers: Set<String> = ["KR", "ZŁ", "KČ", "FT", "LEI", "ЛВ", "DIN", "RS", "TL", "DH", "RM", "RP", "DKK", "SFR"]

    struct Line {
        var fragments: [OCRFragment]
        var text: String { fragments.map(\.text).joined(separator: " ") }
    }

    static func parse(_ fragments: [OCRFragment]) -> ParsedReceipt {
        parse(lines: groupIntoLines(fragments))
    }

    // MARK: Righe

    /// Raggruppa i frammenti che stanno sulla stessa riga, compensando lo scontrino storto.
    static func groupIntoLines(_ fragments: [OCRFragment]) -> [Line] {
        guard !fragments.isEmpty else { return [] }
        return group(fragments, slope: estimateSlope(fragments))
    }

    /// L'inclinazione delle righe. Gli angoli che dà Vision valgono solo per le scritte lunghe
    /// (per "8.00" dice sempre 0), quindi si provano le pendenze fino a ±8° e si tiene quella
    /// che mette sulla stessa riga più coppie "nome … prezzo". A parità, la più vicina
    /// all'angolo misurato sulle scritte lunghe.
    static func estimateSlope(_ fragments: [OCRFragment]) -> CGFloat {
        let long = fragments.filter { $0.box.width > $0.box.height * 5 }.map(\.angle).sorted()
        let hint = long.isEmpty ? 0 : tan(long[long.count / 2])
        var best: (score: Int, slope: CGFloat) = (-1, hint)
        for step in -28...28 {
            let slope = CGFloat(step) * 0.005
            let score = group(fragments, slope: slope).filter(isPricedItemLine).count
            if score > best.score || (score == best.score && abs(slope - hint) < abs(best.slope - hint)) {
                best = (score, slope)
            }
        }
        return best.slope
    }

    private static func isPricedItemLine(_ line: Line) -> Bool {
        guard line.fragments.count >= 2, let amount = trailingAmount(in: line) else { return false }
        return amount.rest.contains(where: \.isLetter)
    }

    static func group(_ fragments: [OCRFragment], slope: CGFloat) -> [Line] {
        func level(_ f: OCRFragment) -> CGFloat { f.box.midY - slope * f.box.midX }

        var rows: [(y: CGFloat, height: CGFloat, items: [OCRFragment])] = []
        for f in fragments.sorted(by: { level($0) < level($1) }) {
            let y = level(f)
            if let i = rows.indices.last(where: { abs(rows[$0].y - y) < min(rows[$0].height, f.box.height) * 0.6 }) {
                rows[i].items.append(f)
                rows[i].y += (y - rows[i].y) / CGFloat(rows[i].items.count)
                rows[i].height = max(rows[i].height, f.box.height)
            } else {
                rows.append((y, f.box.height, [f]))
            }
        }
        return rows
            .sorted { $0.y < $1.y }
            .map { Line(fragments: $0.items.sorted { $0.box.minX < $1.box.minX }) }
    }

    // MARK: Voci

    static func parse(lines: [Line]) -> ParsedReceipt {
        var items: [BillItem] = []
        var total: Int?
        var subtotal: Int?
        var itemsEnded = false
        var pendingName: (text: String, confidence: Float)?
        var pendingQuantity: (qty: Int, unit: Int)?
        let layout = Layout(lines)
        let firstLine = lines.firstIndex { $0.text.contains(where: \.isLetter) }

        for (index, line) in lines.enumerated() {
            let raw = line.text.trimmingCharacters(in: .whitespaces)
            let folded = fold(raw)
            let price = trailingAmount(in: line)

            if isTotalLine(folded) {
                if let p = price {
                    if folded.contains("sub") { subtotal = subtotal ?? p.cents } else { total = total ?? p.cents }
                }
                if !items.isEmpty { itemsEnded = true }
                continue
            }
            if itemsEnded { continue }
            if isNoise(folded) {
                pendingName = nil
                continue
            }
            if let q = quantityOnlyLine(raw) {
                pendingQuantity = q
                continue
            }
            // Sopra la prima voce c'è l'intestazione: insegna, ragione sociale, indirizzo. Non sono prodotti.
            if items.isEmpty, isShopHeader(line, folded: folded, priced: price != nil, layout: layout) {
                pendingName = nil
                continue
            }
            guard var p = price else {
                // Un nome senza prezzo: può essere una voce lunga andata a capo.
                // La prima riga dello scontrino no: lì c'è il nome del locale.
                if looksLikeName(raw), index != firstLine { pendingName = (raw, minConfidence(line.fragments)) } else { pendingName = nil }
                continue
            }

            var name = p.rest
            var nameConfidence = p.restConfidence
            if cleanName(name).isEmpty, let pending = pendingName {
                name = pending.text
                nameConfidence = pending.confidence
            }
            pendingName = nil

            var quantity = 1
            var unitPrice: Int?
            if let inline = inlineQuantity(in: name) {
                quantity = inline.qty
                unitPrice = inline.unit
                name = inline.rest
            } else if let leading = leadingQuantity(in: name) {
                quantity = leading.qty
                name = leading.rest
            }
            if let q = pendingQuantity {
                quantity = q.qty
                unitPrice = q.unit
                pendingQuantity = nil
            }
            if let unit = unitPrice, unit * quantity != p.cents {
                p.uncertain = true
            }

            let cleaned = cleanName(name)
            guard !cleaned.isEmpty else { continue }

            var issue: BillItem.Issue?
            if p.uncertain || p.cents <= 0 || p.cents >= 100_000 {
                issue = .price
            } else if nameConfidence < 0.45 || nameLooksGarbled(cleaned) {
                issue = .name
            }
            items.append(BillItem(name: cleaned, quantity: quantity, cents: p.cents, issue: issue))
        }

        return ParsedReceipt(items: items, total: total ?? subtotal)
    }

    // MARK: Intestazione

    /// Dove stanno le voci: margini e altezza del testo delle righe "nome … prezzo".
    /// L'insegna del locale di solito è centrata e scritta più grande.
    struct Layout {
        var left: CGFloat
        var right: CGFloat
        var textHeight: CGFloat

        init?(_ lines: [Line]) {
            let priced = lines.filter { line in
                guard let amount = trailingAmount(in: line), amount.rest.contains(where: \.isLetter) else { return false }
                let folded = fold(line.text)
                return !isTotalLine(folded) && !isNoise(folded)
            }
            guard priced.count >= 2 else { return nil }
            func median(_ values: [CGFloat]) -> CGFloat { values.sorted()[values.count / 2] }
            left = median(priced.map { $0.fragments[0].box.minX })
            right = median(priced.map { $0.fragments.map(\.box.maxX).max()! })
            textHeight = median(priced.map { $0.fragments[0].box.height })
            guard right - left > 0, textHeight > 0 else { return nil }
        }

        /// Contano solo le parole: un prezzo finito sulla stessa riga non dice com'è scritta.
        private func words(_ line: Line) -> [CGRect] {
            line.fragments.filter { $0.text.filter(\.isLetter).count >= 2 }.map(\.box)
        }

        /// Scritta più grande delle voci: le insegne sono a doppia altezza o in un carattere da titolo.
        func isLarge(_ line: Line) -> Bool {
            (words(line).map(\.height).max() ?? 0) > textHeight * 1.6
        }

        /// Centrata: non parte dal margine sinistro delle voci e non arriva alla colonna dei prezzi.
        func isCentered(_ line: Line) -> Bool {
            let boxes = words(line)
            guard let minX = boxes.map(\.minX).min(), let maxX = boxes.map(\.maxX).max() else { return false }
            let span = right - left
            return minX > left + span * 0.08 && maxX < right - span * 0.08
                && abs((minX + maxX) / 2 - (left + right) / 2) < span * 0.08
        }
    }

    /// Che tipo di posto è: nell'intestazione basta questo a dire che la riga è l'insegna.
    private static let shopWords: Set<String> = [
        "ristorante", "trattoria", "pizzeria", "osteria", "taverna", "locanda", "enoteca", "bar", "caffetteria",
        "pasticceria", "gelateria", "panificio", "macelleria", "salumeria", "alimentari", "supermercato",
        "ipermercato", "market", "supermarket", "minimarket", "bistrot", "bistro", "restaurant", "pub", "tavern",
        "diner", "cafe", "brasserie", "steakhouse", "bakery", "deli", "store", "shop", "restaurante", "cafeteria",
        "panaderia", "boulangerie", "gasthaus", "gasthof", "baeckerei", "backerei", "eetcafe", "lokanta",
    ]
    /// Indirizzo e dati del documento: in testa allo scontrino non sono mai una voce, prezzo o no.
    private static let headerWords: Set<String> = [
        "via", "viale", "piazza", "piazzale", "corso", "largo", "vicolo", "strada", "contrada", "localita",
        "street", "road", "avenue", "boulevard", "blvd", "calle", "avenida", "rue", "strasse", "straat",
        "tavolo", "table", "cameriere", "server", "scontrino", "ricevuta", "fattura", "receipt", "invoice",
        "benvenuti", "welcome", "cliente", "customer",
    ]
    /// Forma societaria: "S.r.l.", "SNC", "S.p.A.", "Ltd", "GmbH"...
    private static let legalForm = try! Regex(#"(^|[^a-z])(s\.?\s?r\.?\s?l|s\.?\s?n\.?\s?c|s\.?\s?a\.?\s?s|s\.?\s?p\.?\s?a|ltd|llc|inc|gmbh|sarl|plc)\.?([^a-z]|$)"#)

    /// Una riga dell'intestazione che è l'insegna, la ragione sociale o l'indirizzo del locale.
    /// Su una riga con prezzo servono segni forti (scritta grande o centrata, forma societaria, indirizzo):
    /// "Bar" da solo non basta, una "Protein bar" è un prodotto.
    static func isShopHeader(_ line: Line, folded: String, priced: Bool, layout: Layout?) -> Bool {
        let words = folded.split(whereSeparator: { !$0.isLetter }).map(String.init)
        if let layout, layout.isLarge(line) || layout.isCentered(line) { return true }
        if folded.firstMatch(of: legalForm) != nil || words.contains(where: headerWords.contains) { return true }
        return !priced && words.contains(where: shopWords.contains)
    }

    // MARK: Importi

    struct Amount {
        var cents: Int
        var uncertain: Bool
        /// Il testo della riga prima del prezzo.
        var rest: String
        var restConfidence: Float
    }

    private struct Token {
        var text: String
        var fragment: Int
    }

    /// Il prezzo in fondo alla riga, se c'è. Tollera le sviste tipiche dell'OCR (O al posto di 0,
    /// S al posto di 5...) ma in quel caso segna il prezzo come da confermare.
    static func trailingAmount(in line: Line) -> Amount? {
        var tokens: [Token] = []
        for (i, f) in line.fragments.enumerated() {
            for piece in f.text.split(whereSeparator: \.isWhitespace) {
                tokens.append(Token(text: String(piece), fragment: i))
            }
        }
        // Via simboli di valuta e codici IVA in coda ("5,00 €", "5,00 B").
        while let last = tokens.last, isTrailingMarker(last.text) { tokens.removeLast() }
        guard let last = tokens.last else { return nil }

        var candidates: [(text: String, used: Int)] = [(last.text, 1)]
        if tokens.count >= 2 {
            let prev = tokens[tokens.count - 2].text
            // "40, 00" / "40 ,00" / "€ 40,00" spezzati dall'OCR.
            if prev.hasSuffix(",") || prev.hasSuffix(".") || last.text.hasPrefix(",") || last.text.hasPrefix(".") {
                candidates.insert((prev + last.text, 2), at: 0)
            }
        }

        for candidate in candidates {
            guard let parsed = parseAmount(candidate.text) else { continue }
            let priceTokens = tokens.suffix(candidate.used)
            let restTokens = tokens.dropLast(candidate.used)
            let priceFragments = Set(priceTokens.map(\.fragment))
            let restFragments = Set(restTokens.map(\.fragment))

            var uncertain = parsed.corrected
            for i in priceFragments {
                let f = line.fragments[i]
                if f.confidence < 0.5 { uncertain = true }
                // Se una lettura alternativa dà un prezzo diverso, l'OCR stava tirando a indovinare.
                for alt in f.alternatives {
                    if let a = lastAmount(in: alt), a != parsed.cents { uncertain = true }
                }
            }
            let rest = restTokens.map(\.text).joined(separator: " ")
            let confidence = restFragments.map { line.fragments[$0].confidence }.min() ?? 1
            return Amount(cents: parsed.cents, uncertain: uncertain, rest: rest, restConfidence: confidence)
        }
        return nil
    }

    private static func lastAmount(in text: String) -> Int? {
        var tokens = text.split(whereSeparator: \.isWhitespace).map(String.init)
        while let last = tokens.last, isTrailingMarker(last) { tokens.removeLast() }
        guard let last = tokens.last else { return nil }
        return parseAmount(last)?.cents
    }

    private static func isTrailingMarker(_ t: String) -> Bool {
        let u = t.uppercased().trimmingCharacters(in: CharacterSet(charactersIn: "."))
        if ["EURO", "*", "-", "B", "C", "D", "E", "A", "S", "N", "V", "IVA", "TAX"].contains(u) { return true }
        if isoCodes.contains(u) || localMarkers.contains(u) { return true }
        if !u.isEmpty, u.unicodeScalars.allSatisfy({ $0.properties.generalCategory == .currencySymbol }) { return true }
        if u.hasSuffix("%"), u.dropLast().allSatisfy({ $0.isNumber || $0 == "," || $0 == "." }) { return true }
        return false
    }

    /// "8,00" → 800. "€12.00", "$12.00", "CHF12.00", "S$12.00" → 1200. "7,O0" → 700 (corretto).
    /// "12" → nil (manca la parte decimale), a meno che la valuta non abbia decimali: "¥1,200" → 120000.
    static func parseAmount(_ token: String) -> (cents: Int, corrected: Bool)? {
        var t = token.trimmingCharacters(in: CharacterSet(charactersIn: "*:;"))
        // Prefissi tipo "US$", "S$", "HK$": le lettere vanno via solo se attaccate a un simbolo.
        if let m = t.firstMatch(of: /^[A-Za-z]{1,3}(?=\p{Sc})/) { t.removeSubrange(m.range) }
        t = String(t.unicodeScalars.filter { $0.properties.generalCategory != .currencySymbol })
        // Codici ISO attaccati al numero: "CHF12.50", "12.50EUR".
        if t.count > 3 {
            let head = String(t.prefix(3)).uppercased(), tail = String(t.suffix(3)).uppercased()
            if isoCodes.contains(head) { t.removeFirst(3) } else if isoCodes.contains(tail) { t.removeLast(3) }
        }
        guard !t.isEmpty else { return nil }
        if minorDigits == 0, let whole = wholeUnits(t) { return (whole * 100, false) }

        let fixes: [Character: Character] = ["O": "0", "o": "0", "D": "0", "Q": "0", "l": "1", "I": "1",
                                             "|": "1", "i": "1", "S": "5", "s": "5", "B": "8", "Z": "2", "z": "2"]
        var out = ""
        var corrected = false
        var digits = 0
        for ch in t {
            if ch.isASCII && ch.isNumber {
                out.append(ch)
                digits += 1
            } else if ch == "," || ch == "." || ch == "-" {
                out.append(ch)
            } else if let fix = fixes[ch] {
                out.append(fix)
                corrected = true
            } else {
                return nil
            }
        }
        guard digits >= 2 else { return nil }

        var negative = false
        if out.hasPrefix("-") { negative = true; out.removeFirst() }
        if out.hasSuffix("-") { negative = true; out.removeLast() }

        // Ultimo separatore = decimali, e devono essere esattamente due cifre.
        guard let sep = out.lastIndex(where: { $0 == "," || $0 == "." }) else { return nil }
        let fraction = out[out.index(after: sep)...]
        let whole = out[..<sep].filter(\.isNumber)
        guard fraction.count == 2, fraction.allSatisfy(\.isNumber), !whole.isEmpty, whole.count <= 5,
              out[..<sep].allSatisfy({ $0.isNumber || $0 == "." || $0 == "," }),
              let w = Int(whole), let f = Int(fraction) else { return nil }
        let cents = w * 100 + f
        return (negative ? -cents : cents, corrected)
    }

    /// Importi senza decimali per yen, won & co.: "1,200", "1.200", "850". Sotto 50 no:
    /// su uno scontrino in yen un "12" è un tavolo, non un prezzo.
    private static func wholeUnits(_ t: String) -> Int? {
        guard t.firstMatch(of: /^\d{1,3}([.,]\d{3})+$|^\d{2,8}$/) != nil,
              let value = Int(t.filter(\.isNumber)), value >= 50 else { return nil }
        return value
    }

    // MARK: Quantità

    /// Riga "2 x 2,50" sopra la voce, tipica dei documenti commerciali italiani.
    static func quantityOnlyLine(_ text: String) -> (qty: Int, unit: Int)? {
        let pattern = #"^\s*(\d{1,2})\s*[xX×*]\s*(?:€\s*)?(\d{1,4}[.,]\d{2})\s*$"#
        guard let m = text.firstMatch(of: try! Regex(pattern)),
              let qty = Int(m.output[1].substring ?? ""), qty > 0,
              let unit = parseAmount(String(m.output[2].substring ?? ""))?.cents else { return nil }
        return (qty, unit)
    }

    /// "Birra 2 x 2,50" → quantità 2 da 2,50, nome "Birra".
    static func inlineQuantity(in text: String) -> (qty: Int, unit: Int, rest: String)? {
        let pattern = #"^(.*?)\s*(\d{1,2})\s*[xX×*]\s*(?:€\s*)?(\d{1,4}[.,]\d{2})\s*$"#
        guard let m = text.firstMatch(of: try! Regex(pattern)),
              let qty = Int(m.output[2].substring ?? ""), qty > 0,
              let unit = parseAmount(String(m.output[3].substring ?? ""))?.cents else { return nil }
        return (qty, unit, String(m.output[1].substring ?? ""))
    }

    /// "2 x Birra" / "2x Birra". Un numero da solo non basta: "4 formaggi" è una pizza.
    static func leadingQuantity(in text: String) -> (qty: Int, rest: String)? {
        let pattern = #"^\s*(\d{1,2})\s*[xX×*]\s+(.+)$"#
        guard let m = text.firstMatch(of: try! Regex(pattern)),
              let qty = Int(m.output[1].substring ?? ""), qty > 0 else { return nil }
        return (qty, String(m.output[2].substring ?? ""))
    }

    // MARK: Classificazione

    static func fold(_ s: String) -> String {
        s.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "it_IT"))
    }

    static func isTotalLine(_ folded: String) -> Bool {
        let words = folded.split(whereSeparator: { !$0.isLetter }).map(String.init)
        guard let first = words.first else { return false }
        if ["totale", "total", "tot", "subtotale", "subtotal", "sub", "importo", "amount", "balance", "grand"].contains(first) { return true }
        return ["da pagare", "totale complessivo", "amount due", "balance due", "grand total"].contains(where: folded.contains)
    }

    /// Righe con un numero che sembra un prezzo ma non sono voci: IVA, pagamento, resto, intestazione.
    /// Parole intere, non prefissi: "cassata" è un dolce, "fiore di latte" non è un orario.
    /// Scontrini italiani e inglesi.
    static func isNoise(_ folded: String) -> Bool {
        let words: Set<String> = ["tax", "vat", "cash", "change", "tendered", "card", "debit", "approved", "auth",
                                  "thank", "thanks", "phone", "date", "time", "iva", "imponibile", "partita", "tel", "telefono", "fax", "resto", "contanti",
                                  "contante", "pagamento", "pagato", "elettronico", "carta", "bancomat", "visa",
                                  "mastercard", "credito", "ticket", "buono", "matricola", "documento", "cassa",
                                  "cassiere", "operatore", "ore", "data", "grazie", "arrivederci", "pezzi",
                                  "articoli", "descrizione", "prezzo", "rt", "cf"]
        let phrases = ["p.iva", "p. iva", "c.f.", "cod. fisc", "scontrino n", "doc.", "www", "http",
                       "cap. soc", "cap.soc", "capitale soc", "reg. imp", "r.e.a", "c.c.i.a.a"]
        let lineWords = folded.split(whereSeparator: { !$0.isLetter }).map(String.init)
        if lineWords.contains(where: words.contains) { return true }
        if phrases.contains(where: folded.contains) { return true }
        // Date e orari ("28/09/2026", "20:31") hanno l'aspetto di importi.
        if folded.firstMatch(of: try! Regex(#"\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4}"#)) != nil { return true }
        if folded.firstMatch(of: try! Regex(#"\b\d{1,2}:\d{2}\b"#)) != nil { return true }
        return false
    }

    private static func looksLikeName(_ text: String) -> Bool {
        let letters = text.filter(\.isLetter).count
        return letters >= 3 && Double(letters) / Double(max(1, text.count)) > 0.6
    }

    private static func minConfidence(_ fragments: [OCRFragment]) -> Float {
        fragments.map(\.confidence).min() ?? 1
    }

    /// Nome pulito, con la maiuscola solo all'inizio se lo scontrino era tutto in maiuscolo.
    static func cleanName(_ text: String) -> String {
        var s = text
        // Aliquote IVA e simboli rimasti in mezzo.
        s = s.replacing(try! Regex(#"\d{1,2}([.,]\d+)?\s?%"#), with: "")
        s = s.replacingOccurrences(of: "€", with: "")
        s = s.trimmingCharacters(in: CharacterSet.whitespaces.union(CharacterSet(charactersIn: "*-:;,_=#")))
        // Il punto di un'abbreviazione resta ("Acqua nat."), i puntini di riempimento no ("Pasta .....").
        s = s.replacing(try! Regex(#"(\s+\.+|\.{2,})$"#), with: "")
        s = s.replacing(try! Regex(#"^\.+"#), with: "")
        s = s.split(whereSeparator: \.isWhitespace).joined(separator: " ")
        guard s.contains(where: \.isLetter) else { return "" }
        if s == s.uppercased() {
            let lower = s.lowercased()
            s = lower.prefix(1).uppercased() + lower.dropFirst()
        }
        return s
    }

    /// Caratteri che su un menu non ci sono: segno che l'OCR ha letto male.
    static func nameLooksGarbled(_ name: String) -> Bool {
        if name.count < 3 { return true }
        let odd = CharacterSet(charactersIn: "?|_~#@^<>[]{}\\▒░▓■□")
        if name.unicodeScalars.contains(where: { odd.contains($0) }) { return true }
        let letters = name.filter(\.isLetter).count
        return Double(letters) / Double(name.filter { !$0.isWhitespace }.count) < 0.6
    }
}
