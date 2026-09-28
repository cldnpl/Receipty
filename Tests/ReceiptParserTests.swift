import XCTest
@testable import WhoPays

final class ReceiptParserTests: XCTestCase {

    /// Righe già separate: nome a sinistra e prezzo a destra, come le restituisce Vision.
    private func lines(_ rows: [(String, String?)], confidence: Float = 1) -> [OCRFragment] {
        rows.enumerated().flatMap { i, row -> [OCRFragment] in
            let y = CGFloat(i) * 40
            var out = [OCRFragment(text: row.0, confidence: confidence, box: CGRect(x: 20, y: y, width: 200, height: 24))]
            if let price = row.1 {
                out.append(OCRFragment(text: price, confidence: confidence, box: CGRect(x: 400, y: y + 2, width: 70, height: 24)))
            }
            return out
        }
    }

    func testMockupReceipt() {
        let parsed = ReceiptParser.parse(lines([
            ("TRATTORIA DA NINO", nil),
            ("Via Roma 12 - Tel. 06 1234567", nil),
            ("MARGHERITA", "8,00"),
            ("CARBONARA", "12,00"),
            ("BIRRA MEDIA", "5,00"),
            ("ACQUA NAT.", "2,00"),
            ("TIRAMIS?", "6,00"),
            ("COPERTO", "7,O0"),
            ("TOTALE", "40,00"),
            ("CONTANTI", "50,00"),
            ("RESTO", "10,00"),
        ]))
        XCTAssertEqual(parsed.items.map(\.name), ["Margherita", "Carbonara", "Birra media", "Acqua nat.", "Tiramis?", "Coperto"])
        XCTAssertEqual(parsed.items.map(\.cents), [800, 1200, 500, 200, 600, 700])
        XCTAssertEqual(parsed.items.map(\.issue), [nil, nil, nil, nil, .name, .price])
        XCTAssertEqual(parsed.total, 4000)
    }

    func testSkewedReceiptStillGroupsLines() {
        // Scontrino inclinato di ~4°: il prezzo sta più in basso del nome della stessa riga.
        let angle: CGFloat = 0.07
        var frags: [OCRFragment] = []
        for (i, row) in [("PASTA", "9,50"), ("VINO", "14,00"), ("CAFFE", "1,50")].enumerated() {
            let y = CGFloat(i) * 36
            frags.append(OCRFragment(text: row.0, confidence: 1, box: CGRect(x: 20, y: y + 20 * tan(angle), width: 120, height: 22), angle: angle))
            frags.append(OCRFragment(text: row.1, confidence: 1, box: CGRect(x: 500, y: y + 520 * tan(angle), width: 60, height: 22), angle: angle))
        }
        let parsed = ReceiptParser.parse(frags)
        XCTAssertEqual(parsed.items.map(\.name), ["Pasta", "Vino", "Caffe"])
        XCTAssertEqual(parsed.items.map(\.cents), [950, 1400, 150])
    }

    func testQuantities() {
        let parsed = ReceiptParser.parse(lines([
            ("2 x 2,50", nil),
            ("BIRRA", "5,00"),
            ("2x Caffè", "3,00"),
            ("ACQUA 3 x 1,50", "4,50"),
            ("4 FORMAGGI", "9,00"),
            ("3 x 2,00", nil),
            ("SPRITZ", "7,00"),
        ]))
        XCTAssertEqual(parsed.items.map(\.name), ["Birra", "Caffè", "Acqua", "4 formaggi", "Spritz"])
        XCTAssertEqual(parsed.items.map(\.quantity), [2, 2, 3, 1, 3])
        // 3 × 2,00 non fa 7,00: il prezzo va confermato.
        XCTAssertEqual(parsed.items.last?.issue, .price)
        XCTAssertNil(parsed.items.first?.issue)
    }

    func testIgnoresVatDatesAndPayments() {
        let parsed = ReceiptParser.parse(lines([
            ("DOCUMENTO COMMERCIALE", nil),
            ("28/09/2026 20:31", nil),
            ("PIZZA DIAVOLA 10%", "9,00 B"),
            ("Fiore di latte", "3,00"),
            ("Cassata", "5,50"),
            ("IVA 10%", "1,59"),
            ("TOTALE COMPLESSIVO", "17,50"),
            ("PAGAMENTO ELETTRONICO", "17,50"),
        ]))
        XCTAssertEqual(parsed.items.map(\.name), ["Pizza diavola", "Fiore di latte", "Cassata"])
        XCTAssertEqual(parsed.total, 1750)
    }

    func testWrappedNameAndSplitPrice() {
        let parsed = ReceiptParser.parse(lines([
            ("TAGLIATA DI MANZO CON", nil),
            ("RUCOLA E GRANA", "18,00"),
            ("Tagliere misto", "€ 14,00"),
            ("Dolce", "5 ,00"),
        ]))
        XCTAssertEqual(parsed.items.map(\.cents), [1800, 1400, 500])
        XCTAssertEqual(parsed.items[1].name, "Tagliere misto")
    }

    func testEnglishReceipt() {
        let parsed = ReceiptParser.parse(lines([
            ("THE RED LION", nil),
            ("Table 12   Guests 4", nil),
            ("Fish & Chips", "14.50"),
            ("2 x Pint Lager", "11.00"),
            ("Sticky toffee pudding", "6.75"),
            ("Service charge 12.5%", "4.03"),
            ("SUBTOTAL", "36.28"),
            ("VAT 20%", "6.05"),
            ("TOTAL", "36.28"),
            ("CASH", "40.00"),
            ("CHANGE", "3.72"),
        ]))
        XCTAssertEqual(parsed.items.map(\.name), ["Fish & Chips", "Pint Lager", "Sticky toffee pudding", "Service charge"])
        XCTAssertEqual(parsed.items.map(\.cents), [1450, 1100, 675, 403])
        XCTAssertEqual(parsed.items[1].quantity, 2)
        XCTAssertEqual(parsed.total, 3628)
    }

    func testOtherCurrencies() {
        let parsed = ReceiptParser.parse(lines([
            ("Burger", "$12.50"),
            ("Fries", "4.00 USD"),
            ("Fondue", "CHF38.00"),
            ("Laksa", "S$9.80"),
            ("Pint", "£6.20"),
        ]))
        XCTAssertEqual(parsed.items.map(\.cents), [1250, 400, 3800, 980, 620])
        XCTAssertTrue(parsed.items.allSatisfy { $0.issue == nil })
    }

    func testYenReceiptWithoutDecimals() {
        let parsed = ReceiptParser.$minorDigits.withValue(0) {
            ReceiptParser.parse(lines([
                ("Table 12", nil),
                ("Ramen", "¥1,200"),
                ("Gyoza", "¥480"),
                ("Beer", "650"),
                ("TOTAL", "¥2,330"),
            ]))
        }
        XCTAssertEqual(parsed.items.map(\.name), ["Ramen", "Gyoza", "Beer"])
        XCTAssertEqual(parsed.items.map(\.cents), [120_000, 48_000, 65_000])
        XCTAssertEqual(parsed.total, 233_000)
    }

    func testLowConfidenceFlagsItems() {
        let parsed = ReceiptParser.parse(lines([("CARBONARA", "12,00")], confidence: 0.3))
        XCTAssertEqual(parsed.items.first?.issue, .price)
    }

    func testAmountParsing() {
        XCTAssertEqual(ReceiptParser.parseAmount("8,00")?.cents, 800)
        XCTAssertEqual(ReceiptParser.parseAmount("€12.50")?.cents, 1250)
        XCTAssertEqual(ReceiptParser.parseAmount("1.234,00")?.cents, 123400)
        XCTAssertEqual(ReceiptParser.parseAmount("-2,00")?.cents, -200)
        XCTAssertEqual(ReceiptParser.parseAmount("7,O0")?.corrected, true)
        XCTAssertNil(ReceiptParser.parseAmount("12"))
        XCTAssertNil(ReceiptParser.parseAmount("SO"))
        XCTAssertNil(ReceiptParser.parseAmount("Pizza"))
    }
}
