import XCTest
import UIKit
@testable import Receipty

/// OCR vero (Vision) su foto di scontrini: lo stesso percorso dell'app, dalla foto alle voci.
final class ReceiptReaderTests: XCTestCase {

    private func photo(_ name: String) throws -> UIImage {
        let url = try XCTUnwrap(Bundle(for: Self.self).url(forResource: name, withExtension: "jpg"))
        return try XCTUnwrap(UIImage(contentsOfFile: url.path))
    }

    /// Scontrino fotografato storto (3°) su un tavolo: i prezzi devono restare sulla loro riga.
    func testTiltedReceiptPhoto() async throws {
        let image = try photo("tilted_receipt")
        if ProcessInfo.processInfo.environment["DUMP_OCR"] != nil,
           let cg = ReceiptReader.normalized(image) {
            for f in try ReceiptReader.recognize(ReceiptReader.cropToDocument(cg)) {
                print("OCR", f.text, "conf", f.confidence, "box", f.box.integral, "angle", f.angle)
            }
        }
        let parsed = try await ReceiptReader.read(image)
        XCTAssertEqual(parsed.items.map(\.name), ["Margherita", "Carbonara", "Draft beer", "Still water", "Tiramisu", "Cover charge"])
        XCTAssertEqual(parsed.items.map(\.cents), [800, 1200, 500, 200, 600, 700])
        XCTAssertEqual(parsed.items[2].quantity, 2)
        XCTAssertEqual(parsed.total, 4000)
    }
}
