import SwiftUI

struct HomeView: View {
    @Environment(AppModel.self) private var app

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .top) {
                    Text("Receipty")
                        .textStyle(.hero)
                        .foregroundStyle(Palette.ink)
                        .accessibilityAddTraits(.isHeader)
                    Spacer(minLength: 12)
                    CircleIconButton(systemName: "plus") { app.showingNewBill = true }
                        .coachTarget(.plusButton)
                        .padding(.top, 8)
                        .accessibilityLabel(t("newBill.title"))
                }
                .padding(.leading, 6)
                .padding(.trailing, 5)

                Text(t("home.subtitle"))
                    .textStyle(.lead)
                    .foregroundStyle(Palette.inkSoft)
                    .frame(maxWidth: 290, alignment: .leading)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 3.5)
                    .padding(.leading, 6)

                if app.store.bills.isEmpty {
                    EmptyRecents()
                        .padding(.top, 44)
                } else {
                    Text(t("home.recent"))
                        .textStyle(.section)
                        .foregroundStyle(Palette.raspberry)
                        .padding(.top, 41.5)
                        .padding(.leading, 8)
                        .accessibilityAddTraits(.isHeader)

                    LazyVStack(spacing: 16) {
                        ForEach(app.store.bills) { bill in
                            SwipeToDelete(height: 99) {
                                delete(bill)
                            } content: {
                                Button {
                                    app.path.append(.saved(bill.id))
                                } label: {
                                    BillCard(bill: bill)
                                }
                                .buttonStyle(PressableStyle(scale: 0.98))
                                .contextMenu {
                                    Button(t("common.delete"), systemImage: "trash", role: .destructive) { delete(bill) }
                                }
                                .accessibilityAction(named: Text(t("common.delete"))) { delete(bill) }
                            }
                            .transition(.asymmetric(insertion: .opacity,
                                                    removal: .opacity.combined(with: .move(edge: .leading))))
                        }
                    }
                    .padding(.top, 14)
                }
            }
            .padding(.horizontal, 22)
            .padding(.top, 8)
            .padding(.bottom, 24)
        }
        .scrollIndicators(.hidden)
        .plainScrollEdges()
        .topFade(10)
        .background(BackdropView())
        .toolbar(.hidden, for: .navigationBar)
    }

    private func delete(_ bill: SavedBill) {
        Haptics.tap()
        withAnimation(.snappy) { app.store.delete(bill.id) }
    }
}

private struct BillCard: View {
    let bill: SavedBill

    var body: some View {
        HStack(spacing: 18) {
            IconBadge(systemName: bill.source == .equal ? "divide" : "receipt")
            VStack(alignment: .leading, spacing: 4.5) {
                Text(DateText.short(bill.date))
                    .textStyle(.cardTitle)
                    .foregroundStyle(Palette.ink)
                Text(bill.names)
                    .textStyle(.cardSub)
                    .foregroundStyle(Palette.gray)
                    .lineLimit(1)
            }
            Spacer(minLength: 8)
            VStack(alignment: .trailing, spacing: 8) {
                Image(systemName: "checkmark.circle")
                    .font(.system(size: 21, weight: .medium))
                    .foregroundStyle(Palette.check)
                Text(Money.format(bill.settlement.total, bill.settlement.currency))
                    .textStyle(.cardMeta)
                    .foregroundStyle(Palette.gray)
            }
        }
        .padding(.leading, 22)
        .padding(.trailing, 22)
        .frame(height: 99)
        .card()
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(DateText.short(bill.date)), \(bill.names), \(Money.format(bill.settlement.total, bill.settlement.currency)), \(t("common.settled"))")
    }
}

private struct EmptyRecents: View {
    var body: some View {
        VStack(spacing: 14) {
            IconBadge(systemName: "receipt", size: 54)
            VStack(spacing: 5) {
                Text(t("home.empty.title"))
                    .textStyle(.cardTitle)
                    .foregroundStyle(Palette.ink)
                Text(t("home.empty.text"))
                    .textStyle(.cardSub)
                    .foregroundStyle(Palette.gray)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 30)
        .background(
            RoundedRectangle(cornerRadius: Metrics.cardRadius, style: .continuous)
                .fill(Palette.cardGhost)
        )
        .overlay(
            RoundedRectangle(cornerRadius: Metrics.cardRadius, style: .continuous)
                .strokeBorder(Palette.dashed, style: StrokeStyle(lineWidth: 1.3, dash: [4, 4]))
        )
    }
}
