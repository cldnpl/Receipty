import UIKit
import Vision
import CoreImage
import CoreImage.CIFilterBuiltins

/// Dalla foto alle voci: raddrizza il foglio, legge il testo sul telefono (Vision, niente rete)
/// e passa tutto al parser.
enum ReceiptReader {
    enum ReadError: Error { case unreadableImage }

    static func read(_ image: UIImage) async throws -> ParsedReceipt {
        try await Task.detached(priority: .userInitiated) {
            guard let upright = normalized(image) else { throw ReadError.unreadableImage }
            let cropped = cropToDocument(upright)
            var parsed = ReceiptParser.parse(try recognize(cropped))
            // Se il ritaglio ha tagliato male, meglio rileggere la foto intera.
            if parsed.items.isEmpty, cropped !== upright {
                parsed = ReceiptParser.parse(try recognize(upright))
            }
            return parsed
        }.value
    }

    /// Foto dritta (niente orientamento EXIF) e non più grande del necessario.
    static func normalized(_ image: UIImage, maxDimension: CGFloat = 2800) -> CGImage? {
        let longest = max(image.size.width, image.size.height)
        guard longest > 0 else { return nil }
        let scale = min(1, maxDimension / longest)
        let size = CGSize(width: (image.size.width * scale).rounded(), height: (image.size.height * scale).rounded())
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        format.opaque = true
        return UIGraphicsImageRenderer(size: size, format: format).image { _ in
            image.draw(in: CGRect(origin: .zero, size: size))
        }.cgImage
    }

    /// Trova il foglio nella foto e lo raddrizza in prospettiva.
    static func cropToDocument(_ image: CGImage) -> CGImage {
        let request = VNDetectDocumentSegmentationRequest()
        let handler = VNImageRequestHandler(cgImage: image, orientation: .up)
        guard (try? handler.perform([request])) != nil,
              let doc = request.results?.first, doc.confidence > 0.6 else { return image }

        let w = CGFloat(image.width), h = CGFloat(image.height)
        func point(_ p: CGPoint) -> CGPoint { CGPoint(x: p.x * w, y: p.y * h) }
        let quad = [doc.topLeft, doc.topRight, doc.bottomRight, doc.bottomLeft].map(point)
        // Area del quadrilatero (formula di Gauss): se è piccola il rilevamento non è affidabile.
        var area: CGFloat = 0
        for i in 0..<4 {
            let a = quad[i], b = quad[(i + 1) % 4]
            area += a.x * b.y - b.x * a.y
        }
        guard abs(area) / 2 > w * h * 0.2 else { return image }

        let filter = CIFilter.perspectiveCorrection()
        filter.inputImage = CIImage(cgImage: image)
        filter.topLeft = point(doc.topLeft)
        filter.topRight = point(doc.topRight)
        filter.bottomRight = point(doc.bottomRight)
        filter.bottomLeft = point(doc.bottomLeft)
        guard let output = filter.outputImage,
              let result = CIContext().createCGImage(output, from: output.extent) else { return image }
        return result
    }

    static func recognize(_ image: CGImage) throws -> [OCRFragment] {
        let request = VNRecognizeTextRequest()
        request.recognitionLevel = .accurate
        request.recognitionLanguages = ["it-IT", "en-US"]
        request.usesLanguageCorrection = true
        try VNImageRequestHandler(cgImage: image, orientation: .up).perform([request])

        let w = CGFloat(image.width), h = CGFloat(image.height)
        return (request.results ?? []).compactMap { observation in
            let candidates = observation.topCandidates(3)
            guard let best = candidates.first else { return nil }
            let bb = observation.boundingBox
            let box = CGRect(x: bb.minX * w, y: (1 - bb.maxY) * h, width: bb.width * w, height: bb.height * h)
            // Vision ha l'origine in basso: la y va ribaltata anche per l'angolo.
            let dx = (observation.topRight.x - observation.topLeft.x) * w
            let dy = -(observation.topRight.y - observation.topLeft.y) * h
            return OCRFragment(text: best.string,
                               confidence: best.confidence,
                               alternatives: candidates.dropFirst().map(\.string),
                               box: box,
                               angle: atan2(dy, dx))
        }
    }
}
