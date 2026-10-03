import SwiftUI
import Observation

/// I punti dell'interfaccia che il tutorial illumina.
enum CoachTarget: Hashable {
    case plusButton
    case scanOption
    case manualOption
}

/// Il tutorial della prima volta: lo schermo si scurisce e la patina nera si stringe
/// attorno a un solo elemento, un passo alla volta. Si può sempre saltare.
@Observable
final class Coach {
    enum Step: Int, CaseIterable {
        case plus, scan, manual

        var target: CoachTarget {
            switch self {
            case .plus: .plusButton
            case .scan: .scanOption
            case .manual: .manualOption
            }
        }

        var title: String { t("coach.\(rawValue + 1).title") }

        var message: String { t("coach.\(rawValue + 1).message") }

        /// Al primo passo si tocca davvero il +: il buco nella patina lascia passare il tocco.
        var tapThrough: Bool { self == .plus }
    }

    var step: Step?
    /// Frame dei bersagli in coordinate della finestra.
    var frames: [CoachTarget: CGRect] = [:]
    /// Quando la patina è "aperta" su tutto lo schermo (inizio e fine dell'animazione).
    var wide = true

    var isActive: Bool { step != nil }
}

private struct CoachKey: EnvironmentKey {
    static let defaultValue: Coach? = nil
}

extension EnvironmentValues {
    var coach: Coach? {
        get { self[CoachKey.self] }
        set { self[CoachKey.self] = newValue }
    }
}

private struct CoachTargetModifier: ViewModifier {
    @Environment(\.coach) private var coach
    let target: CoachTarget

    func body(content: Content) -> some View {
        content.onGeometryChange(for: CGRect.self) { $0.frame(in: .global) } action: { frame in
            coach?.frames[target] = frame
        }
    }
}

extension View {
    /// Registra la posizione di un elemento che il tutorial può illuminare.
    func coachTarget(_ target: CoachTarget) -> some View {
        modifier(CoachTargetModifier(target: target))
    }
}

// MARK: - Patina

/// Un rettangolo nero con un buco: animabile, così il buco si stringe e si sposta.
private struct Spotlight: Shape {
    var hole: CGRect
    var radius: CGFloat

    var animatableData: AnimatablePair<AnimatablePair<CGFloat, CGFloat>, AnimatablePair<AnimatablePair<CGFloat, CGFloat>, CGFloat>> {
        get { .init(.init(hole.minX, hole.minY), .init(.init(hole.width, hole.height), radius)) }
        set {
            hole = CGRect(x: newValue.first.first, y: newValue.first.second,
                          width: newValue.second.first.first, height: newValue.second.first.second)
            radius = newValue.second.second
        }
    }

    func path(in rect: CGRect) -> Path {
        var p = Path(rect)
        p.addRoundedRect(in: hole, cornerSize: CGSize(width: radius, height: radius), style: .continuous)
        return p
    }
}

struct CoachOverlay: View {
    @Bindable var coach: Coach
    let onNext: (Coach.Step) -> Void
    let onSkip: () -> Void

    @State private var pulse = false

    var body: some View {
        GeometryReader { geo in
            let screen = CGRect(origin: .zero, size: geo.size)
            let step = coach.step ?? .plus
            let target = coach.frames[step.target] ?? CGRect(x: screen.midX, y: screen.midY, width: 0, height: 0)
            let isCircle = step == .plus
            let pad: CGFloat = isCircle ? 9 : 7
            let focused = target.insetBy(dx: -pad, dy: -pad)
            // All'inizio il "buco" è più grande dello schermo: la patina non si vede. Poi si stringe.
            let hole = coach.wide ? screen.insetBy(dx: -geo.size.height, dy: -geo.size.height) : focused
            let radius = coach.wide ? geo.size.height : (isCircle ? focused.height / 2 : Metrics.cardRadius + pad)
            let spotlight = Spotlight(hole: hole, radius: radius)

            ZStack(alignment: .topLeading) {
                spotlight
                    .fill(Color.black.opacity(0.74), style: FillStyle(eoFill: true))
                    .contentShape(step.tapThrough ? AnyShape(spotlight) : AnyShape(Rectangle()), eoFill: step.tapThrough)
                    .onTapGesture {}

                if step.tapThrough && !coach.wide {
                    Circle()
                        .stroke(Palette.pink, lineWidth: 3)
                        .frame(width: focused.width, height: focused.height)
                        .scaleEffect(pulse ? 1.35 : 1)
                        .opacity(pulse ? 0 : 0.9)
                        .position(x: focused.midX, y: focused.midY)
                        .allowsHitTesting(false)
                        .onAppear {
                            withAnimation(.easeOut(duration: 1.3).repeatForever(autoreverses: false)) { pulse = true }
                        }
                }

                if !coach.wide {
                    callout(step: step)
                        .frame(width: min(geo.size.width - 40, 360))
                        .position(x: geo.size.width / 2, y: calloutY(for: focused, in: geo.size))
                        // La vecchia nuvoletta sparisce subito, la nuova arriva quando il buco è in posizione.
                        .transition(.asymmetric(
                            insertion: .opacity.combined(with: .scale(scale: 0.96))
                                .animation(.easeOut(duration: 0.25).delay(0.4)),
                            removal: .opacity.animation(.easeIn(duration: 0.1))))
                        .id(step)
                }
            }
        }
        .ignoresSafeArea()
        .animation(.spring(response: 0.75, dampingFraction: 0.86), value: coach.wide)
        .animation(.spring(response: 0.6, dampingFraction: 0.85), value: coach.step)
        .accessibilityElement(children: .contain)
        .accessibilityAddTraits(.isModal)
    }

    /// La nuvoletta sta sotto il bersaglio se è in alto, sopra se è in basso.
    private func calloutY(for target: CGRect, in size: CGSize) -> CGFloat {
        let calloutHeight: CGFloat = 190
        if target.midY < size.height / 2 {
            return min(target.maxY + 24 + calloutHeight / 2, size.height - calloutHeight / 2 - 40)
        }
        return max(target.minY - 24 - calloutHeight / 2, calloutHeight / 2 + 60)
    }

    private func callout(step: Coach.Step) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text(t("coach.step", step.rawValue + 1, Coach.Step.allCases.count))
                    .textStyle(TextStyle(face: .semibold, size: 13))
                    .foregroundStyle(Palette.raspberry)
                Spacer()
                Button(t("coach.skip"), action: onSkip)
                    .textStyle(TextStyle(face: .semibold, size: 14))
                    .foregroundStyle(Palette.gray)
            }
            Text(step.title)
                .textStyle(TextStyle(face: .extraBold, size: 24, tracking: -0.02))
                .foregroundStyle(Palette.ink)
                .padding(.top, 10)
            Text(step.message)
                .textStyle(TextStyle(face: .regular, size: 16, lineHeight: 22.5))
                .foregroundStyle(Palette.inkSoft)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 6)
            HStack {
                Spacer()
                Button {
                    onNext(step)
                } label: {
                    Text(t(step == .manual ? "coach.gotIt" : step.tapThrough ? "coach.showMe" : "common.next"))
                        .textStyle(TextStyle(face: .bold, size: 16))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 22)
                        .frame(height: 44)
                        .background(Capsule().fill(Palette.pink))
                }
                .buttonStyle(PressableStyle(scale: 0.95))
            }
            .padding(.top, 14)
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 26, style: .continuous)
                .fill(Palette.card)
                .shadow(color: .black.opacity(0.25), radius: 24, y: 10)
        )
    }
}
