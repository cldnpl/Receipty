import SwiftUI

/// Il foglio "Nuovo conto": le due sole strade dell'app.
struct NewBillSheet: View {
    @Environment(AppModel.self) private var app
    @GestureState private var drag: CGFloat = 0

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            VStack(alignment: .leading, spacing: 0) {
                Capsule()
                    .fill(Palette.handle)
                    .frame(width: 40, height: 4)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 14.5)

                Text("Nuovo conto")
                    .textStyle(.sheetTitle)
                    .foregroundStyle(Palette.ink)
                    .padding(.top, 24)
                    .padding(.leading, 6)
                    .accessibilityAddTraits(.isHeader)

                VStack(spacing: 14) {
                    Button { app.startScan() } label: {
                        OptionCard(icon: "camera",
                                   title: "Scansiona scontrino",
                                   subtitle: "Ognuno paga quello che ha ordinato",
                                   prominent: true)
                    }
                    Button { app.startEqualSplit() } label: {
                        OptionCard(icon: "divide",
                                   title: "Inserisci a mano",
                                   subtitle: "Totale diviso in parti uguali",
                                   prominent: false)
                    }
                }
                .buttonStyle(PressableStyle())
                .padding(.top, 18)
            }
            .padding(.horizontal, 22)
            .padding(.bottom, 5)
            .background(alignment: .top) {
                UnevenRoundedRectangle(topLeadingRadius: 38, topTrailingRadius: 38, style: .continuous)
                    .fill(LinearGradient(colors: [Palette.sheetTop, Palette.sheetBottom], startPoint: .top, endPoint: .bottom))
                    .ignoresSafeArea(edges: .bottom)
            }
            .offset(y: max(0, drag))
            .gesture(
                DragGesture()
                    .updating($drag) { value, state, _ in state = value.translation.height }
                    .onEnded { value in
                        if value.translation.height > 90 || value.predictedEndTranslation.height > 220 {
                            app.showingNewBill = false
                        }
                    }
            )
        }
    }
}

private struct OptionCard: View {
    let icon: String
    let title: String
    let subtitle: String
    let prominent: Bool

    var body: some View {
        HStack(spacing: 17) {
            IconBadge(systemName: icon, size: 52, inverted: prominent)
            VStack(alignment: .leading, spacing: 4.5) {
                Text(title)
                    .textStyle(.optionTitle)
                    .foregroundStyle(prominent ? Color.white : Palette.ink)
                Text(subtitle)
                    .textStyle(.optionSub)
                    .foregroundStyle(prominent ? Color(hex: 0xFFEAF6) : Palette.gray)
            }
            Spacer(minLength: 0)
        }
        .padding(.leading, 18.5)
        .frame(height: 90.5)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: Metrics.cardRadius, style: .continuous)
                .fill(prominent ? Palette.pink : .white)
                .shadow(color: (prominent ? Palette.pink : Palette.shadow).opacity(prominent ? 0.35 : 0.07),
                        radius: prominent ? 18 : 16, y: 8)
        )
        .contentShape(RoundedRectangle(cornerRadius: Metrics.cardRadius, style: .continuous))
    }
}
