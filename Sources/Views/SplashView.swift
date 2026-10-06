import SwiftUI

/// L'avvio: lo scontrino dell'icona si scrive da solo. Cade dall'alto, la riga di testa
/// viene scarabocchiata a pennarello, poi le tre voci con i loro prezzi che cadono di lato,
/// infine il totale si stampa di colpo e sotto resta il marchio. Si tocca per saltarlo.
///
/// Il fondo è lo stesso gradiente di tutte le schermate, e sotto c'è già l'app: alla fine
/// basta sfumare via scontrino e marchio, lo sfondo non si muove. L'uscita è la transizione
/// che gli mette `RootView`: `onFinish` la fa partire, non aspetta che sia finita.
struct SplashView: View {
    let onFinish: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var paper = false
    @State private var header: CGFloat = 0
    @State private var rule: CGFloat = 0
    @State private var rows: [CGFloat] = [0, 0, 0]
    @State private var prices = [false, false, false]
    @State private var total = false
    @State private var mark = false
    @State private var done = false

    /// Le stesse cifre dell'icona: 20 + 30 + 5, e in fondo il tondo 100.
    private let amounts = ["20", "30", "5"]
    /// Righe di lunghezza diversa: scritte a mano non vengono mai uguali.
    private let widths: [CGFloat] = [92, 79, 86]

    var body: some View {
        ZStack {
            // Si parte dal colore piatto del launch screen di sistema e il gradiente ci sale
            // sopra mentre lo scontrino scende: così il passaggio non si vede.
            Backdrop.color(at: 0).ignoresSafeArea()
            Backdrop.gradient.ignoresSafeArea().opacity(paper ? 1 : 0)

            VStack(spacing: 36) {
                receipt
                wordmark
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { finish() }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Receipty")
        .task { await run() }
    }

    // MARK: Lo scontrino

    private var receipt: some View {
        VStack(alignment: .leading, spacing: 0) {
            Scribble(seed: 0)
                .trim(from: 0, to: header)
                .stroke(Palette.ink, style: strokeStyle(5))
                .frame(height: 26)

            Rectangle()
                .fill(Palette.ink.opacity(0.45))
                .frame(height: 1.2)
                .scaleEffect(x: rule, anchor: .leading)
                .padding(.top, 15)

            ForEach(0..<3, id: \.self) { i in
                HStack(spacing: 8) {
                    Scribble(seed: i + 1)
                        .trim(from: 0, to: rows[i])
                        .stroke(Palette.ink, style: strokeStyle(4.4))
                        .frame(width: widths[i], height: 21)
                    Spacer(minLength: 0)
                    Text(amounts[i])
                        .font(.custom(Display.bold.rawValue, fixedSize: 20))
                        .foregroundStyle(Palette.ink)
                        .opacity(prices[i] ? 1 : 0)
                        .scaleEffect(prices[i] ? 1 : 0.4)
                        .rotationEffect(.degrees(prices[i] ? 0 : -16))
                }
                .padding(.top, i == 0 ? 19 : 13)
            }

            Text("100")
                .font(.custom(Display.bold.rawValue, fixedSize: 31))
                .foregroundStyle(Palette.ink)
                .opacity(total ? 1 : 0)
                .scaleEffect(total ? 1 : 1.7)
                .rotationEffect(.degrees(total ? -2 : 7))
                .padding(.top, 18)
        }
        .padding(.horizontal, 22)
        .padding(.top, 22)
        .padding(.bottom, 32)
        .frame(width: 210)
        .background(
            ReceiptShape()
                .fill(Palette.receiptPaper)
                .shadow(color: Palette.shadow.opacity(0.22), radius: 26, x: 0, y: 16)
                // Nel tema scuro il foglio ha un bordo appena più chiaro: senza, l'ombra nera
                // non lo stacca dal fondo.
                .overlay(ReceiptShape().stroke(Palette.receiptEdge.opacity(0.9), lineWidth: 1))
        )
        // La stessa inclinazione dell'icona.
        .rotationEffect(.degrees(-3.5))
        .opacity(paper ? 1 : 0)
        .scaleEffect(paper ? 1 : 0.86)
        .offset(y: paper ? 0 : -48)
    }

    private var wordmark: some View {
        Wordmark(scales: false)
            .opacity(mark ? 1 : 0)
            .offset(y: mark ? 0 : 18)
            .blur(radius: mark ? 0 : 5)
    }

    private func strokeStyle(_ width: CGFloat) -> StrokeStyle {
        StrokeStyle(lineWidth: width, lineCap: .round, lineJoin: .round)
    }

    // MARK: La sequenza

    private func run() async {
        guard !reduceMotion else {
            paper = true; header = 1; rule = 1
            rows = [1, 1, 1]; prices = [true, true, true]
            total = true; mark = true
            await pause(1.1)
            finish()
            return
        }

        withAnimation(.spring(response: 0.52, dampingFraction: 0.72)) { paper = true }
        await pause(0.18)
        withAnimation(.easeOut(duration: 0.34)) { header = 1 }
        await pause(0.16)
        withAnimation(.easeOut(duration: 0.22)) { rule = 1 }

        for i in rows.indices {
            await pause(0.10)
            withAnimation(.easeOut(duration: 0.28)) { rows[i] = 1 }
            // Il prezzo cade quando la riga è quasi finita, senza aspettarla.
            Task {
                try? await Task.sleep(for: .seconds(0.17))
                withAnimation(.spring(response: 0.34, dampingFraction: 0.55)) { prices[i] = true }
            }
        }

        await pause(0.32)
        Haptics.tap()
        withAnimation(.spring(response: 0.40, dampingFraction: 0.52)) { total = true }
        await pause(0.17)
        withAnimation(.easeOut(duration: 0.46)) { mark = true }
        await pause(0.60)
        finish()
    }

    private func pause(_ seconds: Double) async {
        try? await Task.sleep(for: .seconds(seconds))
    }

    private func finish() {
        guard !done else { return }
        done = true
        onFinish()
    }
}

// MARK: - Forme

/// La carta dello scontrino: angoli alti arrotondati e il bordo basso strappato a zigzag,
/// come nell'icona.
struct ReceiptShape: Shape {
    var teeth = 7
    var toothHeight: CGFloat = 10
    var radius: CGFloat = 7

    func path(in r: CGRect) -> Path {
        var p = Path()
        let hem = r.maxY - toothHeight
        p.move(to: CGPoint(x: r.minX, y: r.minY + radius))
        p.addQuadCurve(to: CGPoint(x: r.minX + radius, y: r.minY), control: CGPoint(x: r.minX, y: r.minY))
        p.addLine(to: CGPoint(x: r.maxX - radius, y: r.minY))
        p.addQuadCurve(to: CGPoint(x: r.maxX, y: r.minY + radius), control: CGPoint(x: r.maxX, y: r.minY))
        p.addLine(to: CGPoint(x: r.maxX, y: hem))
        let step = r.width / CGFloat(teeth)
        for i in stride(from: teeth - 1, through: 0, by: -1) {
            let x = r.minX + CGFloat(i) * step
            p.addLine(to: CGPoint(x: x + step / 2, y: r.maxY))
            p.addLine(to: CGPoint(x: x, y: hem))
        }
        p.closeSubpath()
        return p
    }
}

/// Una riga scarabocchiata: il gancio della prima lettera e poi un'onda che si appiattisce
/// verso destra, come la scrittura illeggibile dell'icona. `seed` cambia frequenza, fase e
/// smorzamento, così non ce ne sono due uguali.
///
/// Il tracciato è campionato fitto: serve a `trim(from:to:)` per farla comparire da sinistra,
/// come se qualcuno la stesse scrivendo.
struct Scribble: Shape {
    var seed: Int

    private static let freq: [Double] = [2.7, 2.1, 2.9, 2.3]
    private static let phase: [Double] = [0.0, 0.7, 1.9, 1.2]
    private static let decay: [Double] = [3.4, 2.5, 3.1, 2.1]

    func path(in r: CGRect) -> Path {
        let i = seed % Self.freq.count
        let (freq, phase, decay) = (Self.freq[i], Self.phase[i], Self.decay[i])
        let width = Double(r.width), mid = Double(r.midY), amp = Double(r.height) * 0.42
        let hook = 0.07

        var p = Path()
        p.move(to: CGPoint(x: r.minX + 1, y: r.maxY))
        p.addQuadCurve(to: CGPoint(x: r.minX + r.width * hook, y: r.minY),
                       control: CGPoint(x: r.minX - r.width * 0.01, y: r.minY + r.height * 0.12))
        var u = hook
        while u <= 1 {
            let v = (u - hook) / (1 - hook)
            let y = mid - amp * exp(-decay * v) * sin(freq * v * 2 * .pi + phase)
            p.addLine(to: CGPoint(x: Double(r.minX) + width * u, y: y))
            u += 0.012
        }
        return p
    }
}

#Preview {
    SplashView {}
}
