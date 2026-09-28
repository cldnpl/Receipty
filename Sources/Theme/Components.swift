import SwiftUI

// MARK: - Bottoni

struct PrimaryButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .textStyle(.button)
            .foregroundStyle(isEnabled ? Color.white : Palette.disabledText)
            .frame(maxWidth: .infinity)
            .frame(height: Metrics.buttonHeight)
            .background(Capsule().fill(isEnabled ? Palette.pink : Palette.disabledFill))
            .contentShape(Capsule())
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(.spring(response: 0.25, dampingFraction: 0.7), value: configuration.isPressed)
            .animation(.easeOut(duration: 0.2), value: isEnabled)
    }
}

/// Scala leggermente al tocco: per card e pulsanti secondari.
struct PressableStyle: ButtonStyle {
    var scale: CGFloat = 0.97

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? scale : 1)
            .animation(.spring(response: 0.25, dampingFraction: 0.7), value: configuration.isPressed)
    }
}

struct BackButton: View {
    @Environment(\.dismiss) private var dismiss
    var action: (() -> Void)?

    var body: some View {
        Button {
            if let action { action() } else { dismiss() }
        } label: {
            Image(systemName: "arrow.left")
                .font(.system(size: 17, weight: .medium))
                .foregroundStyle(Palette.ink)
                .frame(width: Metrics.backSize, height: Metrics.backSize)
                .background(Circle().fill(Palette.card).shadow(color: Palette.shadow.opacity(0.10), radius: 12, y: 5))
        }
        .buttonStyle(PressableStyle(scale: 0.92))
        .accessibilityLabel("Back")
    }
}

struct CircleIconButton: View {
    let systemName: String
    var size: CGFloat = 54
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 21, weight: .regular))
                .foregroundStyle(Palette.pink)
                .frame(width: size, height: size)
                .background(Circle().fill(Palette.card).shadow(color: Palette.shadow.opacity(0.12), radius: 16, y: 7))
        }
        .buttonStyle(PressableStyle(scale: 0.92))
    }
}

// MARK: - Persone

struct Avatar: View {
    let person: Person
    var size: CGFloat = 38
    var selected = false

    var body: some View {
        Text(person.initial)
            .font(.custom(Inter.semibold.rawValue, size: size * 0.47))
            .foregroundStyle(selected ? Color.white : Palette.raspberry)
            .frame(width: size, height: size)
            .background(Circle().fill(selected ? Palette.pinkLight : Palette.pinkSoft))
            .accessibilityHidden(true)
    }
}

/// Icona rosa dentro un cerchio rosa chiaro (card dei conti, opzioni del nuovo conto).
struct IconBadge: View {
    let systemName: String
    var size: CGFloat = 54
    var inverted = false

    var body: some View {
        Image(systemName: systemName)
            .font(.system(size: size * 0.36, weight: .regular))
            .foregroundStyle(inverted ? Color.white : Palette.pinkIcon)
            .frame(width: size, height: size)
            .background(Circle().fill(inverted ? Palette.pinkLight : Palette.pinkSoft))
            .accessibilityHidden(true)
    }
}

// MARK: - Layout

/// Dispone i figli in righe che vanno a capo (chip dei nomi).
struct FlowLayout: Layout {
    var spacing: CGFloat = 8
    var lineSpacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, lineHeight: CGFloat = 0, width: CGFloat = 0
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x > 0 && x + size.width > maxWidth {
                x = 0
                y += lineHeight + lineSpacing
                lineHeight = 0
            }
            x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
            width = max(width, x - spacing)
        }
        return CGSize(width: proposal.width ?? width, height: y + lineHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, lineHeight: CGFloat = 0
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x > bounds.minX && x + size.width > bounds.maxX {
                x = bounds.minX
                y += lineHeight + lineSpacing
                lineHeight = 0
            }
            view.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
        }
    }
}

private struct ScreenHeightKey: EnvironmentKey {
    static let defaultValue: CGFloat = 852
}

extension EnvironmentValues {
    var screenHeight: CGFloat {
        get { self[ScreenHeightKey.self] }
        set { self[ScreenHeightKey.self] = newValue }
    }
}

/// La fascia in basso con l'azione principale. Il contenuto che scorre sotto sfuma
/// nel gradiente di sfondo, con gli stessi colori del gradiente in quel punto.
///
/// Un gradiente a tutto schermo spostato con offset qui non funziona: SwiftUI lo ridisegna
/// comunque sull'altezza della fascia. Quindi si calcola la fetta di gradiente che sta
/// dietro la fascia e si disegna solo quella.
struct BottomBar<Content: View>: View {
    @Environment(\.screenHeight) private var screenHeight
    @State private var frame: CGRect = .zero
    var bottomPadding: CGFloat = 0
    @ViewBuilder var content: Content

    var body: some View {
        VStack(spacing: 0) {
            content
        }
        .padding(.horizontal, 24)
        .padding(.top, 26)
        .padding(.bottom, bottomPadding)
        .frame(maxWidth: .infinity)
        .background {
            Backdrop.slice(from: frame.minY / screenHeight, to: frame.maxY / screenHeight)
                .mask(
                    LinearGradient(stops: [.init(color: .clear, location: 0),
                                           .init(color: .black, location: 0.3)],
                                   startPoint: .top, endPoint: .bottom)
                )
                .ignoresSafeArea(edges: .bottom)
                .onGeometryChange(for: CGRect.self) { $0.frame(in: .global) } action: { frame = $0 }
                .allowsHitTesting(false)
        }
    }
}

extension View {
    /// Da iOS 26 gli ScrollView schiariscono da soli il bordo sotto le barre: qui la sfumatura
    /// la disegna già BottomBar con i colori del gradiente, e le due insieme fanno una banda chiara.
    @ViewBuilder
    func plainScrollEdges() -> some View {
        if #available(iOS 26.0, *) {
            scrollEdgeEffectHidden(true, for: .all)
        } else {
            self
        }
    }

    /// Sfuma il bordo alto di uno ScrollView sotto un'intestazione fissa.
    func topFade(_ height: CGFloat = 18) -> some View {
        mask(
            VStack(spacing: 0) {
                LinearGradient(colors: [.clear, .black], startPoint: .top, endPoint: .bottom).frame(height: height)
                Color.black
            }
        )
    }
}

// MARK: - Intestazione delle schermate del percorso

struct FlowHeader: View {
    let title: String
    var subtitle: String?
    var backAction: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            BackButton(action: backAction)
                .padding(.top, 4)
            TightTitle(text: title, style: .flowTitle)
                .padding(.top, 11)
                .padding(.leading, 1)
            if let subtitle {
                Text(subtitle)
                    .textStyle(.lead16)
                    .foregroundStyle(Palette.inkSoft)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 13.5)
                    .padding(.leading, 1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.leading, 24)
        .padding(.trailing, 20)
    }
}

// MARK: - Stati

/// "I conti tornano" / "Mancano €5,00".
struct StatusPill: View {
    let text: String
    var positive: Bool

    var body: some View {
        Text(text)
            .textStyle(TextStyle(face: .semibold, size: 14))
            .foregroundStyle(positive ? Palette.successText : Palette.raspberry)
            .contentTransition(.numericText())
            .padding(.horizontal, 16)
            .frame(height: 33.5)
            .background(Capsule().fill(Palette.pillFill))
            .animation(.snappy, value: text)
    }
}

struct Caption: View {
    let text: String

    var body: some View {
        Text(text)
            .textStyle(.caption)
            .foregroundStyle(Palette.raspberry)
            .multilineTextAlignment(.center)
            .contentTransition(.numericText())
            .animation(.snappy, value: text)
    }
}

// MARK: - Campi

/// Campo importo "€ 30" delle righe "Chi ha pagato".
struct AmountField: View {
    @Environment(\.currency) private var currency
    @Binding var text: String
    var width: CGFloat = 104

    var body: some View {
        HStack(spacing: 4) {
            Text(currency.symbol)
                .font(.custom(Inter.semibold.rawValue, size: 17))
                .lineLimit(1)
                .fixedSize()
                .foregroundStyle(Palette.raspberry)
            TextField(text: $text, prompt: Text("0").foregroundStyle(Palette.fieldPlaceholder)) {
                Text("Amount")
            }
            .textStyle(.fieldValue)
            .foregroundStyle(Palette.ink)
            .multilineTextAlignment(.trailing)
            .keyboardType(.decimalPad)
            .onChange(of: text) { _, new in
                let clean = Money.sanitizeInput(new, decimals: currency.minorDigits > 0)
                if clean != new { text = clean }
            }
        }
        .padding(.horizontal, 14)
        .frame(width: width, height: 45.5)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(Palette.fieldFill))
        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(Palette.fieldBorder, lineWidth: 1.5))
    }
}

/// Bordo tratteggiato delle chip "+ Anna" e "Tutti".
struct DashedCapsule: View {
    var color: Color = Palette.dashed

    var body: some View {
        Capsule().strokeBorder(color, style: StrokeStyle(lineWidth: 1.3, dash: [3.5, 3]))
    }
}

// MARK: - Icone disegnate

/// La calcolatrice del pulsante "Calcola" (non c'è un SF Symbol equivalente).
struct CalculatorIcon: View {
    var color: Color = .white

    var body: some View {
        Canvas { ctx, size in
            let w = size.width, h = size.height
            let body = CGRect(x: 0.75, y: 0.75, width: w - 1.5, height: h - 1.5)
            ctx.stroke(Path(roundedRect: body, cornerRadius: 2.5), with: .color(color), lineWidth: 1.5)
            let screen = CGRect(x: body.minX + w * 0.2, y: body.minY + h * 0.14, width: body.width - w * 0.4, height: h * 0.14)
            ctx.fill(Path(roundedRect: screen, cornerRadius: 0.8), with: .color(color))
            let dot = w * 0.13
            for row in 0..<3 {
                for col in 0..<3 {
                    let x = body.minX + w * 0.2 + CGFloat(col) * (body.width - w * 0.4 - dot) / 2
                    let y = body.minY + h * 0.43 + CGFloat(row) * h * 0.16
                    ctx.fill(Path(roundedRect: CGRect(x: x, y: y, width: dot, height: dot), cornerRadius: 0.8), with: .color(color))
                }
            }
        }
        .frame(width: 15, height: 18)
        .accessibilityHidden(true)
    }
}

// MARK: - Aptico

enum Haptics {
    static func tap() { UIImpactFeedbackGenerator(style: .light).impactOccurred() }
    static func select() { UISelectionFeedbackGenerator().selectionChanged() }
    static func success() { UINotificationFeedbackGenerator().notificationOccurred(.success) }
    static func warning() { UINotificationFeedbackGenerator().notificationOccurred(.warning) }
}
