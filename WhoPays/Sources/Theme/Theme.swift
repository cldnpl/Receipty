import SwiftUI

extension Color {
    init(hex: UInt32, opacity: Double = 1) {
        self.init(.sRGB,
                  red: Double((hex >> 16) & 0xFF) / 255,
                  green: Double((hex >> 8) & 0xFF) / 255,
                  blue: Double(hex & 0xFF) / 255,
                  opacity: opacity)
    }
}

/// Colori campionati dal mockup (Figma "Chi paga?").
enum Palette {
    static let ink = Color(hex: 0x1A1A1A)
    static let inkSoft = Color(hex: 0x4C4346)
    static let gray = Color(hex: 0x58585C)
    static let muted = Color(hex: 0x867A7E)

    static let raspberry = Color(hex: 0x822F4B)
    static let raspberryDeep = Color(hex: 0x6F2441)
    static let warning = Color(hex: 0xA04262)

    static let pink = Color(hex: 0xF57FA7)
    static let pinkIcon = Color(hex: 0xF083A6)
    static let pinkLight = Color(hex: 0xFA9ABA)
    static let pinkSoft = Color(hex: 0xFEE2E9)
    static let pinkMist = Color(hex: 0xFFF2F5)
    static let flag = Color(hex: 0xFFEAEF)
    static let hairline = Color(hex: 0xF1E5E9)
    static let dashed = Color(hex: 0xFDBAD0)
    static let dashedSoft = Color(hex: 0xE6CED6)
    static let placeholder = Color(hex: 0xD9A2B5)

    static let fieldFill = Color(hex: 0xFFF6F9)
    static let fieldBorder = Color(hex: 0xF4DEE5)
    static let fieldPlaceholder = Color(hex: 0xB28D9A)

    static let disabledFill = Color(hex: 0xFA93B6)
    static let disabledText = Color(hex: 0xFFCDDE)

    static let check = Color(hex: 0x2EB889)
    static let success = Color(hex: 0x22C090)
    static let successText = Color(hex: 0x2B8969)
    static let arrow = Color(hex: 0xE686A7)

    static let sheetTop = Color(hex: 0xFFFAFC)
    static let sheetBottom = Color(hex: 0xFEF5F8)
    static let handle = Color(hex: 0xF2C6D5)
    static let scrim = Color(hex: 0x27020E)
    static let shadow = Color(hex: 0xB0325F)

    static let previewDark = Color(hex: 0x2A2227)
    static let bracket = Color(hex: 0xF998BB)
    static let paper = Color(hex: 0xF7F1EA)
}

/// Il gradiente verticale di tutte le schermate, dal rosa quasi bianco al rosa pieno.
enum Backdrop {
    static let stops: [Gradient.Stop] = [
        .init(color: Color(hex: 0xFEF6F9), location: 0.00),
        .init(color: Color(hex: 0xFEF2F6), location: 0.10),
        .init(color: Color(hex: 0xFFEEF3), location: 0.20),
        .init(color: Color(hex: 0xFEE9F0), location: 0.30),
        .init(color: Color(hex: 0xFCE0EA), location: 0.40),
        .init(color: Color(hex: 0xFCD3E2), location: 0.50),
        .init(color: Color(hex: 0xFDC8DA), location: 0.60),
        .init(color: Color(hex: 0xFFBCD3), location: 0.70),
        .init(color: Color(hex: 0xFFB2CC), location: 0.80),
        .init(color: Color(hex: 0xFEA2C1), location: 0.90),
        .init(color: Color(hex: 0xFE93B8), location: 1.00),
    ]
    static let gradient = LinearGradient(stops: stops, startPoint: .top, endPoint: .bottom)
}

struct BackdropView: View {
    var body: some View {
        Backdrop.gradient.ignoresSafeArea()
    }
}

// MARK: - Tipografia (Inter, come nel Figma)

enum Inter: String {
    case regular = "Inter-Regular"
    case medium = "Inter-Medium"
    case semibold = "Inter-SemiBold"
    case bold = "Inter-Bold"
    case extraBold = "Inter-ExtraBold"
}

struct TextStyle {
    var face: Inter
    var size: CGFloat
    /// Letter-spacing in em, come in Figma (-0.03 = -3%).
    var tracking: CGFloat = 0
    /// Distanza tra le linee di base, se diversa da quella naturale del font (1,21 em).
    var lineHeight: CGFloat? = nil
    var relativeTo: Font.TextStyle = .body

    var font: Font { .custom(face.rawValue, size: size, relativeTo: relativeTo) }
    var naturalLineHeight: CGFloat { size * 1.21 }
}

extension TextStyle {
    static let hero = TextStyle(face: .extraBold, size: 46, tracking: -0.03, relativeTo: .largeTitle)
    static let scanTitle = TextStyle(face: .extraBold, size: 39, tracking: -0.02, lineHeight: 42.5, relativeTo: .largeTitle)
    static let flowTitle = TextStyle(face: .extraBold, size: 31, tracking: -0.02, lineHeight: 34, relativeTo: .largeTitle)
    static let resultTitle = TextStyle(face: .extraBold, size: 42.5, tracking: -0.02, relativeTo: .largeTitle)

    static let lead = TextStyle(face: .regular, size: 17, lineHeight: 24.5)
    static let lead16 = TextStyle(face: .regular, size: 16, lineHeight: 23)
    static let section = TextStyle(face: .semibold, size: 14.5, tracking: -0.02, relativeTo: .subheadline)

    static let cardTitle = TextStyle(face: .semibold, size: 17.5, tracking: 0.01, relativeTo: .headline)
    static let cardSub = TextStyle(face: .medium, size: 13.5, tracking: 0.01, relativeTo: .subheadline)
    static let cardMeta = TextStyle(face: .medium, size: 13, tracking: 0.01, relativeTo: .subheadline)

    static let button = TextStyle(face: .bold, size: 18.5, tracking: -0.02, relativeTo: .headline)
    static let sheetTitle = TextStyle(face: .bold, size: 23.5, tracking: -0.01, relativeTo: .title2)
    static let optionTitle = TextStyle(face: .bold, size: 18, relativeTo: .headline)
    static let optionSub = TextStyle(face: .regular, size: 14, relativeTo: .subheadline)

    static let rowName = TextStyle(face: .medium, size: 16)
    static let rowPrice = TextStyle(face: .semibold, size: 16)
    static let rowWarning = TextStyle(face: .medium, size: 14, relativeTo: .subheadline)
    static let totalLabel = TextStyle(face: .regular, size: 14.5, relativeTo: .subheadline)
    static let totalValue = TextStyle(face: .extraBold, size: 22, tracking: -0.02, relativeTo: .title2)

    static let pillLabel = TextStyle(face: .semibold, size: 15.5, relativeTo: .subheadline)
    static let personName = TextStyle(face: .medium, size: 17)
    static let personNameStrong = TextStyle(face: .semibold, size: 17)
    static let chip = TextStyle(face: .medium, size: 15, relativeTo: .subheadline)
    static let caption = TextStyle(face: .semibold, size: 14.5, tracking: -0.01, relativeTo: .footnote)
    static let micro = TextStyle(face: .medium, size: 12, relativeTo: .caption)
    static let itemTitle = TextStyle(face: .semibold, size: 16)
    static let perHead = TextStyle(face: .semibold, size: 12, relativeTo: .caption)

    static let bigAmount = TextStyle(face: .extraBold, size: 46, tracking: -0.03, relativeTo: .largeTitle)
    static let transferName = TextStyle(face: .semibold, size: 17.5, relativeTo: .headline)
    static let transferSub = TextStyle(face: .regular, size: 13.5, relativeTo: .subheadline)
    static let transferAmount = TextStyle(face: .extraBold, size: 24.5, tracking: -0.02, relativeTo: .title2)
    static let link = TextStyle(face: .semibold, size: 15.5, tracking: -0.01, relativeTo: .subheadline)
    static let smallLink = TextStyle(face: .semibold, size: 14, relativeTo: .subheadline)
    static let fieldValue = TextStyle(face: .bold, size: 19, tracking: -0.01)
}

extension View {
    func textStyle(_ style: TextStyle) -> some View {
        font(style.font)
            .tracking(style.size * style.tracking)
            .lineSpacing(max(0, (style.lineHeight ?? style.naturalLineHeight) - style.naturalLineHeight))
    }
}

/// Titolo su più righe con interlinea più stretta di quella naturale del font:
/// SwiftUI non accetta un lineSpacing negativo, quindi le righe vanno impilate a mano.
struct TightTitle: View {
    let text: String
    let style: TextStyle
    var alignment: HorizontalAlignment = .leading

    var body: some View {
        let lines = text.components(separatedBy: "\n")
        VStack(alignment: alignment, spacing: (style.lineHeight ?? style.naturalLineHeight) - style.naturalLineHeight) {
            ForEach(Array(lines.enumerated()), id: \.offset) { _, line in
                Text(line)
                    .textStyle(style)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .foregroundStyle(Palette.ink)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(text.replacingOccurrences(of: "\n", with: " "))
        .accessibilityAddTraits(.isHeader)
    }
}

// MARK: - Geometria comune

enum Metrics {
    static let cardRadius: CGFloat = 28
    static let buttonHeight: CGFloat = 60
    static let backSize: CGFloat = 43
}

extension View {
    /// Card bianca con l'ombra rosata del mockup.
    func card(radius: CGFloat = Metrics.cardRadius, fill: Color = .white) -> some View {
        background(
            RoundedRectangle(cornerRadius: radius, style: .continuous)
                .fill(fill)
                .shadow(color: Palette.shadow.opacity(0.08), radius: 18, x: 0, y: 8)
        )
    }
}
