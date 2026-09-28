import SwiftUI

/// "Chi ha ordinato cosa?": per ogni voce si toccano i nomi. Più nomi = prezzo diviso.
struct AssignView: View {
    @Environment(AppModel.self) private var app
    @Bindable var draft: BillDraft

    var body: some View {
        VStack(spacing: 0) {
            FlowHeader(title: "WHO ORDERED\nWHAT?", subtitle: "Tap the names. Pick more than one\nand the price is split between them.")

            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(draft.items) { item in
                        AssignCard(item: item, draft: draft)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
                .padding(.bottom, 12)
            }
            .scrollIndicators(.hidden)
            .plainScrollEdges()
            .topFade()
            .padding(.top, 4)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            BottomBar {
                RunningTotals(draft: draft)
                    .padding(.bottom, 14)
                Button("Who paid?") { app.path.append(.payments) }
                    .buttonStyle(PrimaryButtonStyle())
                    .disabled(draft.unassignedCount > 0)
                Caption(text: caption)
                    .padding(.top, 13)
                    .padding(.bottom, -2)
            }
        }
        .background(BackdropView())
        .toolbar(.hidden, for: .navigationBar)
    }

    private var caption: String {
        switch draft.unassignedCount {
        case 0: return "Every item has an owner"
        case 1: return "1 item with nobody on it"
        case let n: return "\(n) items with nobody on them"
        }
    }
}

private struct AssignCard: View {
    @Environment(\.currency) private var currency
    let item: BillItem
    @Bindable var draft: BillDraft

    private var assigned: [Person] { draft.people.filter { item.assignees.contains($0.id) } }
    private var everyone: Bool { !draft.people.isEmpty && assigned.count == draft.people.count }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .firstTextBaseline, spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.name)
                        .textStyle(.itemTitle)
                        .foregroundStyle(Palette.ink)
                    if item.quantity > 1 {
                        HStack(spacing: 6) {
                            Text("\(item.quantity) × \(Money.format(item.cents / item.quantity, currency))")
                                .foregroundStyle(Palette.gray)
                            Button("Split") { split() }
                                .foregroundStyle(Palette.pinkText)
                        }
                        .textStyle(.perHead)
                    }
                }
                Spacer(minLength: 8)
                VStack(alignment: .trailing, spacing: 3) {
                    Text(Money.format(item.cents, currency))
                        .textStyle(.itemTitle)
                        .foregroundStyle(Palette.ink)
                    if assigned.count > 1 {
                        Text("\(Money.format(Int((Double(item.cents) / Double(assigned.count)).rounded()), currency)) each")
                            .textStyle(.perHead)
                            .foregroundStyle(Palette.pinkText)
                            .contentTransition(.numericText())
                            .transition(.opacity.combined(with: .move(edge: .top)))
                    }
                }
            }

            FlowLayout(spacing: 8, lineSpacing: 9) {
                ForEach(draft.people) { person in
                    let on = item.assignees.contains(person.id)
                    Button {
                        Haptics.select()
                        withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                            draft.toggle(person, on: item.id)
                        }
                    } label: {
                        PersonChip(person: person, selected: on)
                    }
                    .buttonStyle(PressableStyle(scale: 0.93))
                    .accessibilityAddTraits(on ? .isSelected : [])
                }
                Button {
                    Haptics.select()
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                        draft.toggleEveryone(on: item.id)
                    }
                } label: {
                    Text("All")
                        .textStyle(.chip)
                        .foregroundStyle(everyone ? Color.white : Palette.chipText)
                        .padding(.horizontal, 14)
                        .frame(height: 33.5)
                        .background(Capsule().fill(everyone ? Palette.pink : Color.clear))
                        .overlay { if !everyone { DashedCapsule(color: Palette.dashedSoft) } }
                }
                .buttonStyle(PressableStyle(scale: 0.93))
                .accessibilityAddTraits(everyone ? .isSelected : [])
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 16)
        .padding(.bottom, 17)
        .frame(maxWidth: .infinity, alignment: .leading)
        .card()
    }

    /// "2 × Birra" diventa due birre separate, da dare a persone diverse.
    private func split() {
        guard let i = draft.items.firstIndex(where: { $0.id == item.id }), item.quantity > 1 else { return }
        let n = item.quantity
        var pieces: [BillItem] = []
        for k in 0..<n {
            let cents = item.cents / n + (k < item.cents % n ? 1 : 0)
            pieces.append(BillItem(name: item.name, quantity: 1, cents: cents, assignees: item.assignees))
        }
        withAnimation(.snappy) {
            draft.items.replaceSubrange(i...i, with: pieces)
        }
    }
}

private struct PersonChip: View {
    let person: Person
    let selected: Bool

    var body: some View {
        HStack(spacing: 8) {
            Avatar(person: person, size: 22, selected: selected)
            Text(person.name)
                .textStyle(.chip)
                .foregroundStyle(selected ? Color.white : Palette.ink)
                .lineLimit(1)
        }
        .padding(.leading, 6)
        .padding(.trailing, 13)
        .frame(height: 33.5)
        .background(Capsule().fill(selected ? Palette.pink : Palette.pinkMist))
    }
}

/// Quanto sta spendendo ciascuno, mentre si assegnano le voci.
private struct RunningTotals: View {
    @Environment(\.currency) private var currency
    let draft: BillDraft

    var body: some View {
        let owed = draft.owed
        ScrollView(.horizontal) {
            HStack(spacing: 20) {
                ForEach(draft.people) { person in
                    HStack(spacing: 8) {
                        Avatar(person: person, size: 22)
                        Text(Money.formatCompact(owed[person.id] ?? 0, currency))
                            .textStyle(TextStyle(face: .semibold, size: 15))
                            .foregroundStyle(Palette.ink)
                            .contentTransition(.numericText())
                    }
                    .frame(height: 32)
                }
            }
            .padding(.horizontal, 24)
            .animation(.snappy, value: owed)
        }
        .scrollIndicators(.hidden)
        .plainScrollEdges()
        .padding(.horizontal, -24)
        .accessibilityElement(children: .combine)
    }
}
