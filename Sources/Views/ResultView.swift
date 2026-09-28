import SwiftUI

/// Calcola il risultato del conto in corso, lo salva tra i recenti e lo mostra.
struct DraftResultView: View {
    @Environment(AppModel.self) private var app
    let draft: BillDraft
    @State private var settlement: Settlement?

    var body: some View {
        Group {
            if let settlement {
                ResultView(settlement: settlement, date: draft.createdAt) { app.finish() }
            } else {
                BackdropView()
            }
        }
        .onAppear {
            settlement = draft.settlement()
            app.record(draft)
        }
    }
}

/// "Siete a posto.": chi dà quanto a chi. La schermata per cui esiste l'app.
struct ResultView: View {
    let settlement: Settlement
    let date: Date
    let onDone: () -> Void

    @State private var appeared = false
    @State private var copied = false
    @State private var showingBreakdown = false

    private var currency: Currency { settlement.currency }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            BackButton()
                .padding(.top, 4)
                .padding(.leading, 24)

            ScrollView {
                content
                    .padding(.horizontal, 24)
                    .padding(.bottom, 24)
            }
            .scrollIndicators(.hidden)
            .plainScrollEdges()
            .scrollBounceBehavior(.basedOnSize)
            .topFade()
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            BottomBar {
                HStack(spacing: 11) {
                    Button {
                        UIPasteboard.general.string = summary
                        Haptics.success()
                        withAnimation(.snappy) { copied = true }
                        Task {
                            try? await Task.sleep(for: .seconds(1.8))
                            withAnimation(.snappy) { copied = false }
                        }
                    } label: {
                        Text(copied ? "Copied" : "Copy summary")
                            .textStyle(TextStyle(face: .medium, size: 16))
                            .foregroundStyle(Palette.ink)
                            .contentTransition(.opacity)
                            .frame(maxWidth: .infinity)
                            .frame(height: Metrics.buttonHeight)
                            .background(Capsule().fill(Palette.pillFill).shadow(color: Palette.shadow.opacity(0.1), radius: 14, y: 6))
                    }
                    .buttonStyle(PressableStyle())
                    .frame(width: 153)

                    Button(action: onDone) {
                        Text("Done")
                            .textStyle(TextStyle(face: .semibold, size: 17))
                            .foregroundStyle(Palette.inverseText)
                            .frame(maxWidth: .infinity)
                            .frame(height: Metrics.buttonHeight)
                            .background(Capsule().fill(Palette.inverseFill))
                    }
                    .buttonStyle(PressableStyle())
                }
            }
        }
        .background(BackdropView())
        .toolbar(.hidden, for: .navigationBar)
        .sheet(isPresented: $showingBreakdown) {
            BreakdownSheet(settlement: settlement)
        }
        .environment(\.currency, settlement.currency)
        .onAppear {
            guard !appeared else { return }
            Haptics.success()
            appeared = true
        }
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 0) {
            SuccessMark(drawn: appeared)
                .padding(.top, 19)

            Text("You're all set.")
                .textStyle(.resultTitle)
                .foregroundStyle(Palette.ink)
                .padding(.top, 5)
                .padding(.leading, 1)
                .accessibilityAddTraits(.isHeader)

            Text(subtitle)
                .textStyle(.lead)
                .foregroundStyle(Palette.inkSoft)
                .frame(maxWidth: 322, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 8)
                .padding(.leading, 1)

            if settlement.change > 0 {
                SectionLabel(text: "Change back · \(Money.format(settlement.change, currency))")
                    .padding(.top, 22)
                VStack(spacing: 10.5) {
                    ForEach(Array(settlement.changeSplit.enumerated()), id: \.element.id) { index, share in
                        ChangeCard(share: share)
                            .modifier(Entrance(appeared: appeared, index: index))
                    }
                }
                .padding(.top, 10)
                .padding(.horizontal, -4)

                if !settlement.transfers.isEmpty {
                    SectionLabel(text: "Then")
                        .padding(.top, 20)
                }
            }

            VStack(spacing: 10.5) {
                ForEach(Array(settlement.transfers.enumerated()), id: \.element.id) { index, transfer in
                    TransferCard(transfer: transfer)
                        .modifier(Entrance(appeared: appeared, index: index + settlement.changeSplit.count))
                }
            }
            .padding(.top, settlement.change > 0 ? 10 : 24)
            .padding(.horizontal, -4)

            Button("How we got here") { showingBreakdown = true }
                .textStyle(.smallLink)
                .foregroundStyle(Palette.raspberryDeep)
                .frame(maxWidth: .infinity)
                .padding(.top, settlement.transfers.isEmpty ? 8 : 26)
                .opacity(appeared ? 1 : 0)
                .animation(.easeOut(duration: 0.4).delay(0.5), value: appeared)
        }
    }

    private var subtitle: String {
        let total = Money.format(settlement.total, currency)
        if settlement.change > 0 {
            let change = Money.format(settlement.change, currency)
            switch settlement.transfers.count {
            case 0: return "Hand out the \(change) change like this and the \(total) bill is settled."
            case 1: return "Hand out the \(change) change like this, then one transfer settles the bill."
            case let n: return "Hand out the \(change) change like this, then \(n) transfers settle the bill."
            }
        }
        switch settlement.transfers.count {
        case 0: return "Everyone already paid their share of the \(total) bill. Nobody owes a thing."
        case 1: return "Just one transfer to settle a \(total) bill."
        case let n: return "Just \(n) transfers to settle a \(total) bill."
        }
    }

    private var summary: String {
        var lines = ["WhoPays · \(DateText.short(date))", "\(Money.format(settlement.total, currency)) bill, \(settlement.lines.count) people", ""]
        if settlement.change > 0 {
            lines.append("Change back: \(Money.format(settlement.change, currency))")
            for share in settlement.changeSplit {
                lines.append("\(share.person.name) keeps \(Money.format(share.cents, currency)) of the change")
            }
            lines.append("")
            if !settlement.transfers.isEmpty { lines.append("Then:") }
        }
        if settlement.transfers.isEmpty && settlement.change == 0 {
            lines.append("All square: nobody owes a thing.")
        } else {
            for t in settlement.transfers {
                lines.append("\(t.from.name) → \(t.to.name): \(Money.format(t.cents, currency))")
            }
        }
        return lines.joined(separator: "\n")
    }
}

/// Le card entrano una dopo l'altra, dal basso.
private struct Entrance: ViewModifier {
    let appeared: Bool
    let index: Int

    func body(content: Content) -> some View {
        content
            .opacity(appeared ? 1 : 0)
            .offset(y: appeared ? 0 : 18)
            .animation(.spring(response: 0.5, dampingFraction: 0.8).delay(0.25 + Double(index) * 0.08), value: appeared)
    }
}

private struct SectionLabel: View {
    let text: String

    var body: some View {
        Text(text)
            .textStyle(.section)
            .foregroundStyle(Palette.raspberry)
            .padding(.leading, 2)
            .accessibilityAddTraits(.isHeader)
    }
}

/// "Resto → Claudia": la parte del resto che una persona si tiene al tavolo.
private struct ChangeCard: View {
    @Environment(\.currency) private var currency
    let share: Settlement.ChangeShare

    var body: some View {
        HStack(spacing: 0) {
            Image(systemName: "banknote")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(Palette.pinkIcon)
                .frame(width: 38, height: 38)
                .background(Circle().fill(Palette.pinkSoft))
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 6.5) {
                    Text("Change")
                        .foregroundStyle(Palette.raspberry)
                    Image(systemName: "arrow.right")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Palette.arrow)
                    Text(share.person.name)
                        .foregroundStyle(Palette.ink)
                }
                .textStyle(.transferName)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
                Text("\(share.person.name) keeps it")
                    .textStyle(.transferSub)
                    .foregroundStyle(Palette.gray)
                    .lineLimit(1)
                    .minimumScaleFactor(0.85)
            }
            .padding(.leading, 11)
            Spacer(minLength: 10)
            Text(Money.format(share.cents, currency))
                .textStyle(.transferAmount)
                .foregroundStyle(Palette.ink)
                .fixedSize()
        }
        .padding(.leading, 20)
        .padding(.trailing, 19)
        .frame(height: 76)
        .card(radius: 26)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(share.person.name) keeps \(Money.format(share.cents, currency)) of the change")
    }
}

private struct SuccessMark: View {
    let drawn: Bool

    var body: some View {
        ZStack {
            Circle()
                .fill(Palette.success)
                .shadow(color: Palette.success.opacity(0.3), radius: 14, y: 6)
            CheckShape()
                .trim(from: 0, to: drawn ? 1 : 0)
                .stroke(.white, style: StrokeStyle(lineWidth: 3.2, lineCap: .round, lineJoin: .round))
                .frame(width: 22, height: 16)
                .animation(.easeOut(duration: 0.35).delay(0.2), value: drawn)
        }
        .frame(width: 56, height: 56)
        .scaleEffect(drawn ? 1 : 0.6)
        .opacity(drawn ? 1 : 0)
        .animation(.spring(response: 0.45, dampingFraction: 0.6), value: drawn)
        .accessibilityHidden(true)
    }

    private struct CheckShape: Shape {
        func path(in rect: CGRect) -> Path {
            Path { p in
                p.move(to: CGPoint(x: rect.minX, y: rect.midY + rect.height * 0.05))
                p.addLine(to: CGPoint(x: rect.minX + rect.width * 0.36, y: rect.maxY))
                p.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
            }
        }
    }
}

private struct TransferCard: View {
    @Environment(\.currency) private var currency
    let transfer: Settlement.Transfer

    var body: some View {
        HStack(spacing: 0) {
            Avatar(person: transfer.from, size: 38)
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 6.5) {
                    Text(transfer.from.name)
                    Image(systemName: "arrow.right")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Palette.arrow)
                    Text(transfer.to.name)
                }
                .textStyle(.transferName)
                .foregroundStyle(Palette.ink)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
                Text("\(transfer.from.name) pays \(transfer.to.name)")
                    .textStyle(.transferSub)
                    .foregroundStyle(Palette.gray)
                    .lineLimit(1)
            }
            .padding(.leading, 11)
            Spacer(minLength: 10)
            Text(Money.format(transfer.cents, currency))
                .textStyle(.transferAmount)
                .foregroundStyle(Palette.ink)
                .fixedSize()
        }
        .padding(.leading, 20)
        .padding(.trailing, 19)
        .frame(height: 76)
        .card(radius: 26)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(transfer.from.name) pays \(transfer.to.name) \(Money.format(transfer.cents, currency))")
    }
}

/// "Come ci siamo arrivati": per ciascuno quanto doveva, quanto ha pagato e il saldo.
private struct BreakdownSheet: View {
    let settlement: Settlement
    private var currency: Currency { settlement.currency }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("How we got here")
                .textStyle(.sheetTitle)
                .foregroundStyle(Palette.ink)
                .padding(.top, 30)
            Text("For each person: their share of the bill, what they paid, and the difference.")
                .textStyle(TextStyle(face: .regular, size: 15, lineHeight: 21))
                .foregroundStyle(Palette.inkSoft)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 6)

            ScrollView {
                VStack(spacing: 8) {
                    ForEach(settlement.lines) { line in
                        HStack(spacing: 12) {
                            Avatar(person: line.person, size: 38)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(line.person.name)
                                    .textStyle(.personNameStrong)
                                    .foregroundStyle(Palette.ink)
                                Group {
                                    Text("Share \(Money.format(line.owed, currency)) · paid \(Money.format(line.paid, currency))")
                                    if line.changeKept > 0 {
                                        Text("Kept \(Money.format(line.changeKept, currency)) of the change")
                                    }
                                }
                                .textStyle(TextStyle(face: .medium, size: 12.5))
                                .foregroundStyle(Palette.gray)
                                .lineLimit(1)
                                .minimumScaleFactor(0.85)
                            }
                            Spacer(minLength: 8)
                            VStack(alignment: .trailing, spacing: 1) {
                                Text((line.balance > 0 ? "+" : "") + Money.format(line.balance, currency))
                                    .textStyle(TextStyle(face: .bold, size: 17))
                                    .foregroundStyle(line.balance > 0 ? Palette.successText : line.balance < 0 ? Palette.raspberry : Palette.gray)
                                Text(line.balance > 0 ? "gets back" : line.balance < 0 ? "owes" : "even")
                                    .textStyle(TextStyle(face: .medium, size: 12))
                                    .foregroundStyle(Palette.gray)
                            }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .frame(minHeight: 64)
                        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(Palette.card))
                    }
                }
                .padding(.vertical, 18)
            }
            .scrollIndicators(.hidden)
            .plainScrollEdges()
        }
        .padding(.horizontal, 22)
        .presentationDetents([.medium, .large])
        .presentationCornerRadius(38)
        .presentationDragIndicator(.visible)
        .presentationBackground {
            LinearGradient(colors: [Palette.sheetTop, Palette.sheetBottom], startPoint: .top, endPoint: .bottom)
        }
    }
}
