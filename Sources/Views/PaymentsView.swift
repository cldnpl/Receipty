import SwiftUI

/// "Chi ha pagato?": quanto ha messo ciascuno. Con lo scontrino il totale è già noto,
/// nella divisione in parti uguali si scrive qui in alto.
struct PaymentsView: View {
    @Environment(\.currency) private var currency
    @Environment(AppModel.self) private var app
    @Bindable var draft: BillDraft
    @FocusState private var focus: Focus?

    enum Focus: Hashable {
        case total
        case person(UUID)
    }

    var body: some View {
        VStack(spacing: 0) {
            FlowHeader(title: "WHO PAID?")

            ScrollView {
                VStack(spacing: 14.5) {
                    totalCard
                    VStack(spacing: 10) {
                        ForEach(draft.people) { person in
                            PayerRow(person: person,
                                     consumed: draft.source.isItemized ? draft.owed[person.id] : nil,
                                     text: binding(for: person),
                                     focus: $focus) {
                                Haptics.tap()
                                focus = nil
                                withAnimation(.snappy) { draft.paidEverything(person) }
                            }
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 13)
                .padding(.bottom, 12)
            }
            .scrollIndicators(.hidden)
            .plainScrollEdges()
            .scrollDismissesKeyboard(.interactively)
            .topFade()
            .padding(.top, 4)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            // Mentre si scrive la barra sparisce (il resoconto va sopra la tastiera): altrimenti
            // tra tastiera, pill e bottone resta visibile una riga sola.
            if focus == nil {
                bottomBar.transition(.opacity)
            }
        }
        .animation(.easeOut(duration: 0.2), value: focus == nil)
        .background(BackdropView())
        .toolbar(.hidden, for: .navigationBar)
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Text(status.text)
                    .font(.custom(Inter.semibold.rawValue, size: 14.5))
                    .foregroundStyle(status.ok ? Palette.successText : Palette.raspberry)
                    .contentTransition(.numericText())
                    .animation(.snappy, value: status.text)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                Spacer()
                Button("Done") { focus = nil }
                    .font(.custom(Inter.semibold.rawValue, size: 16))
            }
        }
        .onAppear {
            if !draft.source.isItemized && draft.total == 0 { focus = .total }
        }
        .onChange(of: focus) { old, _ in
            // "52" diventa "52.00" quando si esce dal campo del totale.
            if old == .total, let cents = Money.parse(draft.equalTotalText), cents > 0 {
                draft.equalTotalText = currency.minorDigits == 0
                    ? String(cents / 100)
                    : String(format: "%d.%02d", cents / 100, cents % 100)
            }
        }
    }

    private var bottomBar: some View {
            BottomBar {
                StatusPill(text: status.text, positive: status.ok)
                    .padding(.bottom, 13)
                Button {
                    focus = nil
                    app.path.append(.result)
                } label: {
                    HStack(spacing: 13) {
                        CalculatorIcon(color: draft.canSettle ? .white : Palette.disabledText)
                        Text("Calculate")
                    }
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(!draft.canSettle)
            }
    }

    private func binding(for person: Person) -> Binding<String> {
        Binding(get: { draft.paidText[person.id] ?? "" },
                set: { draft.paidText[person.id] = $0 })
    }

    private var status: (text: String, ok: Bool) {
        let total = draft.total
        let paid = draft.paidTotal
        if total == 0 { return ("Enter the bill total", false) }
        if paid == total { return ("It all adds up", true) }
        if paid < total { return ("\(Money.format(total - paid, currency)) still missing", false) }
        let change = paid - total
        // Più di €100 di resto e più del conto stesso: quasi sempre un importo scritto male.
        if change > 10_000 && change > total {
            return ("\(Money.format(change, currency)) in change? Check the amounts", false)
        }
        return ("\(Money.format(change, currency)) comes back in change", true)
    }

    @ViewBuilder
    private var totalCard: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Bill total")
                .textStyle(TextStyle(face: .medium, size: 14))
                .foregroundStyle(Palette.raspberry)

            if draft.source.isItemized {
                Text(Money.format(draft.total, currency))
                    .textStyle(.bigAmount)
                    .foregroundStyle(Palette.ink)
                    .padding(.top, 5)
                Text("\(draft.source == .scan ? "From the receipt" : "Typed in") · \(draft.items.count) \(draft.items.count == 1 ? "item" : "items")")
                    .textStyle(TextStyle(face: .regular, size: 14))
                    .foregroundStyle(Palette.gray)
                    .padding(.top, 6)
            } else {
                HStack(alignment: .firstTextBaseline, spacing: 6.5) {
                    Text(currency.symbol)
                        .textStyle(TextStyle(face: .extraBold, size: 44, tracking: -0.02))
                        .foregroundStyle(Palette.pink)
                    TextField(text: $draft.equalTotalText, prompt: Text("0.00").foregroundStyle(Palette.fieldPlaceholder.opacity(0.6))) {
                        Text("Bill total")
                    }
                    .textStyle(TextStyle(face: .extraBold, size: 45.5, tracking: -0.02))
                    .foregroundStyle(Palette.ink)
                    .keyboardType(.decimalPad)
                    .focused($focus, equals: .total)
                    .onChange(of: draft.equalTotalText) { _, new in
                        let clean = Money.sanitizeInput(new, decimals: currency.minorDigits > 0)
                        if clean != new { draft.equalTotalText = clean }
                    }
                }
                .padding(.top, 4)
                Text(splitLine)
                    .textStyle(TextStyle(face: .regular, size: 14))
                    .foregroundStyle(Palette.gray)
                    .contentTransition(.numericText())
                    .padding(.top, 4)
            }
        }
        .padding(.horizontal, 21)
        .padding(.top, 19.5)
        .padding(.bottom, 19.5)
        .frame(maxWidth: .infinity, alignment: .leading)
        .card()
        .contentShape(Rectangle())
        .onTapGesture { if !draft.source.isItemized { focus = .total } }
    }

    private var splitLine: String {
        let n = draft.people.count
        guard draft.total > 0, n > 0 else { return "Split \(n) ways: enter the total" }
        let each = Double(draft.total) / Double(n)
        let rounded = Int(each.rounded())
        return "Split \(n) ways: \(Money.format(rounded, currency)) each"
    }
}

private struct PayerRow: View {
    @Environment(\.currency) private var currency
    let person: Person
    let consumed: Int?
    @Binding var text: String
    var focus: FocusState<PaymentsView.Focus?>.Binding
    let onEverything: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            Avatar(person: person, size: 38)
            VStack(alignment: .leading, spacing: 1) {
                Text(person.name)
                    .textStyle(.personNameStrong)
                    .foregroundStyle(Palette.ink)
                    .lineLimit(1)
                if let consumed {
                    Text("Ordered\n\(Money.format(consumed, currency))")
                        .textStyle(TextStyle(face: .medium, size: 12, lineHeight: 14.3))
                        .foregroundStyle(Palette.gray)
                }
            }
            .padding(.leading, 9.5)
            Spacer(minLength: 6)
            Button("All", action: onEverything)
                .textStyle(TextStyle(face: .semibold, size: 14))
                .foregroundStyle(Palette.raspberry)
                .padding(.horizontal, 11.5)
                .frame(height: 35)
                .background(Capsule().fill(Palette.pinkSoft))
                .buttonStyle(PressableStyle(scale: 0.92))
                .accessibilityLabel("\(person.name) paid everything")
            AmountField(text: $text)
                .focused(focus, equals: .person(person.id))
                .padding(.leading, 10.5)
        }
        .padding(.leading, 12)
        .padding(.trailing, 8)
        .frame(height: consumed == nil ? 62 : 64)
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(Palette.cardSoft))
    }
}
