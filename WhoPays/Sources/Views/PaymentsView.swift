import SwiftUI

/// "Chi ha pagato?": quanto ha messo ciascuno. Con lo scontrino il totale è già noto,
/// nella divisione in parti uguali si scrive qui in alto.
struct PaymentsView: View {
    @Environment(AppModel.self) private var app
    @Bindable var draft: BillDraft
    @FocusState private var focus: Focus?

    enum Focus: Hashable {
        case total
        case person(UUID)
    }

    var body: some View {
        VStack(spacing: 0) {
            FlowHeader(title: "CHI HA PAGATO?")

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
            BottomBar {
                StatusPill(text: status.text, positive: status.ok)
                    .padding(.bottom, 13)
                Button {
                    focus = nil
                    app.path.append(.result)
                } label: {
                    HStack(spacing: 13) {
                        CalculatorIcon(color: draft.isBalanced ? .white : Palette.disabledText)
                        Text("Calcola")
                    }
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(!draft.isBalanced)
            }
        }
        .background(BackdropView())
        .toolbar(.hidden, for: .navigationBar)
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Fine") { focus = nil }
                    .font(.custom(Inter.semibold.rawValue, size: 16))
            }
        }
        .onAppear {
            if !draft.source.isItemized && draft.total == 0 { focus = .total }
        }
    }

    private func binding(for person: Person) -> Binding<String> {
        Binding(get: { draft.paidText[person.id] ?? "" },
                set: { draft.paidText[person.id] = $0 })
    }

    private var status: (text: String, ok: Bool) {
        let total = draft.total
        let paid = draft.paidTotal
        if total == 0 { return ("Scrivi il totale del conto", false) }
        if paid == total { return ("I conti tornano", true) }
        if paid < total { return ("Mancano \(Money.format(total - paid))", false) }
        return ("\(Money.format(paid - total)) in più del totale", false)
    }

    @ViewBuilder
    private var totalCard: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Totale del conto")
                .textStyle(TextStyle(face: .medium, size: 14))
                .foregroundStyle(Palette.raspberry)

            if draft.source.isItemized {
                Text(Money.format(draft.total))
                    .textStyle(.bigAmount)
                    .foregroundStyle(Palette.ink)
                    .padding(.top, 5)
                Text("\(draft.source == .scan ? "Dallo scontrino" : "Scritto a mano") · \(draft.items.count) \(draft.items.count == 1 ? "voce" : "voci")")
                    .textStyle(TextStyle(face: .regular, size: 14))
                    .foregroundStyle(Palette.gray)
                    .padding(.top, 6)
            } else {
                HStack(alignment: .firstTextBaseline, spacing: 6.5) {
                    Text("€")
                        .textStyle(TextStyle(face: .extraBold, size: 44, tracking: -0.02))
                        .foregroundStyle(Palette.pink)
                    TextField(text: $draft.equalTotalText, prompt: Text("0,00").foregroundStyle(Palette.fieldPlaceholder.opacity(0.6))) {
                        Text("Totale del conto")
                    }
                    .textStyle(TextStyle(face: .extraBold, size: 45.5, tracking: -0.02))
                    .foregroundStyle(Palette.ink)
                    .keyboardType(.decimalPad)
                    .focused($focus, equals: .total)
                    .onChange(of: draft.equalTotalText) { _, new in
                        let clean = Money.sanitizeInput(new)
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
        guard draft.total > 0, n > 0 else { return "Diviso in \(n): scrivi quanto è venuto" }
        let each = Double(draft.total) / Double(n)
        let rounded = Int(each.rounded())
        return "Diviso in \(n): \(Money.format(rounded)) a testa"
    }
}

private struct PayerRow: View {
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
                    Text("Ha consumato\n\(Money.format(consumed))")
                        .textStyle(TextStyle(face: .medium, size: 12, lineHeight: 14.3))
                        .foregroundStyle(Palette.gray)
                }
            }
            .padding(.leading, 9.5)
            Spacer(minLength: 6)
            Button("Tutto", action: onEverything)
                .textStyle(TextStyle(face: .semibold, size: 14))
                .foregroundStyle(Palette.raspberry)
                .padding(.horizontal, 11.5)
                .frame(height: 35)
                .background(Capsule().fill(Palette.pinkSoft))
                .buttonStyle(PressableStyle(scale: 0.92))
                .accessibilityLabel("\(person.name) ha pagato tutto")
            AmountField(text: $text)
                .focused(focus, equals: .person(person.id))
                .padding(.leading, 10.5)
        }
        .padding(.leading, 12)
        .padding(.trailing, 8)
        .frame(height: consumed == nil ? 62 : 64)
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(Color.white.opacity(0.78)))
    }
}
