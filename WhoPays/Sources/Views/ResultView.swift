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

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                BackButton()
                    .padding(.top, 4)

                SuccessMark(drawn: appeared)
                    .padding(.top, 19)

                Text("Siete a posto.")
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

                VStack(spacing: 10.5) {
                    ForEach(Array(settlement.transfers.enumerated()), id: \.element.id) { index, transfer in
                        TransferCard(transfer: transfer)
                            .opacity(appeared ? 1 : 0)
                            .offset(y: appeared ? 0 : 18)
                            .animation(.spring(response: 0.5, dampingFraction: 0.8).delay(0.25 + Double(index) * 0.08), value: appeared)
                    }
                }
                .padding(.top, 24)
                .padding(.horizontal, -4)

                Button("Come ci siamo arrivati") { showingBreakdown = true }
                    .textStyle(.smallLink)
                    .foregroundStyle(Palette.raspberryDeep)
                    .frame(maxWidth: .infinity)
                    .padding(.top, settlement.transfers.isEmpty ? 8 : 26)
                    .opacity(appeared ? 1 : 0)
                    .animation(.easeOut(duration: 0.4).delay(0.5), value: appeared)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
        .scrollIndicators(.hidden)
        .plainScrollEdges()
        .scrollBounceBehavior(.basedOnSize)
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
                        Text(copied ? "Copiato" : "Copia riepilogo")
                            .textStyle(TextStyle(face: .medium, size: 16))
                            .foregroundStyle(Palette.ink)
                            .contentTransition(.opacity)
                            .frame(maxWidth: .infinity)
                            .frame(height: Metrics.buttonHeight)
                            .background(Capsule().fill(Color(hex: 0xFFF8FA)).shadow(color: Palette.shadow.opacity(0.1), radius: 14, y: 6))
                    }
                    .buttonStyle(PressableStyle())
                    .frame(width: 153)

                    Button(action: onDone) {
                        Text("Fatto")
                            .textStyle(TextStyle(face: .semibold, size: 17))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: Metrics.buttonHeight)
                            .background(Capsule().fill(Palette.ink))
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
        .onAppear {
            guard !appeared else { return }
            Haptics.success()
            appeared = true
        }
    }

    private var subtitle: String {
        let total = Money.format(settlement.total)
        switch settlement.transfers.count {
        case 0: return "Ognuno ha già pagato la sua parte di un conto da \(total). Nessuno deve niente."
        case 1: return "Basta un trasferimento per chiudere un conto da \(total)."
        case let n: return "Bastano \(n) trasferimenti per chiudere un conto da \(total)."
        }
    }

    private var summary: String {
        var lines = ["Chi paga? · \(DateText.short(date))", "Conto da \(Money.format(settlement.total)), \(settlement.lines.count) persone", ""]
        if settlement.transfers.isEmpty {
            lines.append("Siete già pari: nessuno deve niente.")
        } else {
            for t in settlement.transfers {
                lines.append("\(t.from.name) → \(t.to.name): \(Money.format(t.cents))")
            }
        }
        return lines.joined(separator: "\n")
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
                Text("\(transfer.from.name) dà a \(transfer.to.name)")
                    .textStyle(.transferSub)
                    .foregroundStyle(Palette.gray)
                    .lineLimit(1)
            }
            .padding(.leading, 11)
            Spacer(minLength: 10)
            Text(Money.format(transfer.cents))
                .textStyle(.transferAmount)
                .foregroundStyle(Palette.ink)
                .fixedSize()
        }
        .padding(.leading, 20)
        .padding(.trailing, 19)
        .frame(height: 76)
        .card(radius: 26)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(transfer.from.name) dà \(Money.format(transfer.cents)) a \(transfer.to.name)")
    }
}

/// "Come ci siamo arrivati": per ciascuno quanto doveva, quanto ha pagato e il saldo.
private struct BreakdownSheet: View {
    let settlement: Settlement

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Come ci siamo arrivati")
                .textStyle(.sheetTitle)
                .foregroundStyle(Palette.ink)
                .padding(.top, 30)
            Text("Per ognuno: la sua parte del conto, quanto ha pagato e la differenza.")
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
                                Text("Parte \(Money.format(line.owed)) · pagato \(Money.format(line.paid))")
                                    .textStyle(TextStyle(face: .medium, size: 12.5))
                                    .foregroundStyle(Palette.gray)
                            }
                            Spacer(minLength: 8)
                            VStack(alignment: .trailing, spacing: 1) {
                                Text(line.balance == 0 ? "€0,00" : (line.balance > 0 ? "+" : "") + Money.format(line.balance))
                                    .textStyle(TextStyle(face: .bold, size: 17))
                                    .foregroundStyle(line.balance > 0 ? Palette.successText : line.balance < 0 ? Palette.raspberry : Palette.gray)
                                Text(line.balance > 0 ? "riceve" : line.balance < 0 ? "deve" : "pari")
                                    .textStyle(TextStyle(face: .medium, size: 12))
                                    .foregroundStyle(Palette.gray)
                            }
                        }
                        .padding(.horizontal, 14)
                        .frame(height: 64)
                        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(.white))
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
            LinearGradient(colors: [Palette.sheetTop, Color(hex: 0xFDE9F0)], startPoint: .top, endPoint: .bottom)
        }
    }
}
