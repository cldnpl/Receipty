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

/// Colori campionati dal mockup (Figma "Chi paga?"), con il loro gemello per il tema scuro:
/// al posto del rosa pieno, un prugna scurissimo che in basso si accende di lampone.
enum Palette {
    static let ink = Color(light: 0x1A1A1A, dark: 0xF7EEF2)
    static let inkSoft = Color(light: 0x4C4346, dark: 0xCDB9C1)
    static let gray = Color(light: 0x58585C, dark: 0xAE9FA6)
    static let muted = Color(light: 0x867A7E, dark: 0x8F7F87)

    static let raspberry = Color(light: 0x822F4B, dark: 0xF59BBD)
    static let raspberryDeep = Color(light: 0x6F2441, dark: 0xF7A8C6)
    static let warning = Color(light: 0xA04262, dark: 0xFF8FB3)

    static let pink = Color(hex: 0xF57FA7)
    static let pinkIcon = Color(light: 0xF083A6, dark: 0xF58DB1)
    static let pinkText = Color(light: 0xE388A7, dark: 0xF58DB1)
    static let pinkLight = Color(hex: 0xFA9ABA)
    static let pinkSoft = Color(light: 0xFEE2E9, dark: 0x3A2230)
    static let pinkMist = Color(light: 0xFFF2F5, dark: 0x2E1D26)
    static let flag = Color(light: 0xFFEAEF, dark: 0x3D1F2D)
    static let hairline = Color(light: 0xF1E5E9, dark: 0x3A2A31)
    static let dashed = Color(light: 0xFDBAD0, dark: 0x6B3A50)
    static let dashedSoft = Color(light: 0xE6CED6, dark: 0x5A3A48)
    static let placeholder = Color(light: 0xD9A2B5, dark: 0x8C6B79)
    static let chipText = Color(light: 0x652B41, dark: 0xF7B6CD)
    static let chipFill = Color(light: 0xFFDFEA, dark: 0x3A2230)

    static let fieldFill = Color(light: 0xFFF6F9, dark: 0x2A1B22)
    static let fieldBorder = Color(light: 0xF4DEE5, dark: 0x4A2F3C)
    static let fieldPlaceholder = Color(light: 0xB28D9A, dark: 0x8C6F7B)

    static let disabledFill = Color(light: 0xFA93B6, dark: 0x5A2E42)
    static let disabledText = Color(light: 0xFFCDDE, dark: 0xA07488)

    static let check = Color(light: 0x2EB889, dark: 0x3CCB98)
    static let success = Color(hex: 0x22C090)
    static let successText = Color(light: 0x2B8969, dark: 0x4ED6A4)
    static let arrow = Color(light: 0xE686A7, dark: 0xF58DB1)

    /// Superfici: card piene, righe semitrasparenti, pillole chiare.
    static let card = Color(light: 0xFFFFFF, dark: 0x251A21)
    static let cardSoft = Color(light: 0xFFFFFF, dark: 0x251A21, lightOpacity: 0.8, darkOpacity: 0.85)
    static let cardGhost = Color(light: 0xFFFFFF, dark: 0x251A21, lightOpacity: 0.55, darkOpacity: 0.5)
    static let pillFill = Color(light: 0xFFF8FA, dark: 0x2C1E26)
    /// Il nero dei bottoni "Aggiungi" e "Fatto": nel tema scuro diventa chiaro.
    static let inverseFill = Color(light: 0x1A1A1A, dark: 0xF7EEF2)
    static let inverseText = Color(light: 0xFFFFFF, dark: 0x1A1A1A)

    static let sheetTop = Color(light: 0xFFFAFC, dark: 0x241920)
    static let sheetBottom = Color(light: 0xFEF5F8, dark: 0x1C141A)
    static let handle = Color(light: 0xF2C6D5, dark: 0x5A3A48)
    static let scrim = Color(hex: 0x27020E)
    static let shadow = Color(light: 0xB0325F, dark: 0x000000)

    static let previewDark = Color(light: 0x2A2227, dark: 0x120C0F)
    static let bracket = Color(hex: 0xF998BB)
    static let paper = Color(hex: 0xF7F1EA)
}

extension Color {
    /// Un colore per il tema chiaro e uno per quello scuro.
    init(light: UInt32, dark: UInt32, lightOpacity: Double = 1, darkOpacity: Double = 1) {
        self.init(uiColor: UIColor { traits in
            let isDark = traits.userInterfaceStyle == .dark
            return UIColor(rgb: isDark ? dark : light, alpha: isDark ? darkOpacity : lightOpacity)
        })
    }
}

extension UIColor {
    convenience init(rgb: UInt32, alpha: Double = 1) {
        self.init(red: CGFloat((rgb >> 16) & 0xFF) / 255,
                  green: CGFloat((rgb >> 8) & 0xFF) / 255,
                  blue: CGFloat(rgb & 0xFF) / 255,
                  alpha: alpha)
    }
}

/// Il gradiente verticale di tutte le schermate, dal rosa quasi bianco al rosa pieno
/// (nel tema scuro: dal prugna quasi nero al lampone).
enum Backdrop {
    private typealias Stop = (location: Double, hex: UInt32)
    private static let light: [Stop] = [
        (0.00, 0xFEF6F9), (0.10, 0xFEF2F6), (0.20, 0xFFEEF3), (0.30, 0xFEE9F0),
        (0.40, 0xFCE0EA), (0.50, 0xFCD3E2), (0.60, 0xFDC8DA), (0.70, 0xFFBCD3),
        (0.80, 0xFFB2CC), (0.90, 0xFEA2C1), (1.00, 0xFE93B8),
    ]
    private static let dark: [Stop] = [
        (0.00, 0x150F12), (0.20, 0x181114), (0.40, 0x1D1318), (0.55, 0x23151C),
        (0.70, 0x2D1722), (0.85, 0x3A1A2A), (1.00, 0x4A1E34),
    ]

    static var stops: [Gradient.Stop] {
        // Stesse posizioni in entrambi i temi: si campiona il gradiente scuro dove cadono quelle chiare.
        light.map { .init(color: color(at: $0.location), location: $0.location) }
    }
    static var gradient: LinearGradient { LinearGradient(stops: stops, startPoint: .top, endPoint: .bottom) }

    /// Il colore del gradiente a una frazione dell'altezza dello schermo (0 = in alto).
    static func color(at t: Double) -> Color {
        Color(uiColor: UIColor { traits in
            let stops = traits.userInterfaceStyle == .dark ? dark : light
            return UIColor.mix(stops, at: t)
        })
    }

    /// Lo stesso gradiente, ritagliato sulla fascia di schermo tra `top` e `bottom` (frazioni).
    static func slice(from top: Double, to bottom: Double) -> LinearGradient {
        guard bottom > top else { return LinearGradient(colors: [color(at: top)], startPoint: .top, endPoint: .bottom) }
        var slice: [Gradient.Stop] = [.init(color: color(at: top), location: 0)]
        for s in light where s.location > top && s.location < bottom {
            slice.append(.init(color: color(at: s.location), location: (s.location - top) / (bottom - top)))
        }
        slice.append(.init(color: color(at: bottom), location: 1))
        return LinearGradient(stops: slice, startPoint: .top, endPoint: .bottom)
    }
}

private extension UIColor {
    static func mix(_ stops: [(location: Double, hex: UInt32)], at t: Double) -> UIColor {
        let t = min(1, max(0, t))
        guard let i = stops.lastIndex(where: { $0.location <= t }), i < stops.count - 1 else {
            return UIColor(rgb: stops.last!.hex)
        }
        let a = stops[i], b = stops[i + 1]
        let u = (t - a.location) / (b.location - a.location)
        func channel(_ h: UInt32, _ shift: UInt32) -> Double { Double((h >> shift) & 0xFF) / 255 }
        func mix(_ shift: UInt32) -> CGFloat { CGFloat(channel(a.hex, shift) + (channel(b.hex, shift) - channel(a.hex, shift)) * u) }
        return UIColor(red: mix(16), green: mix(8), blue: mix(0), alpha: 1)
    }
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
    func card(radius: CGFloat = Metrics.cardRadius, fill: Color = Palette.card) -> some View {
        background(
            RoundedRectangle(cornerRadius: radius, style: .continuous)
                .fill(fill)
                .shadow(color: Palette.shadow.opacity(0.08), radius: 18, x: 0, y: 8)
        )
    }
}
