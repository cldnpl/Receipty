import SwiftUI
import PhotosUI

struct ScanView: View {
    @Environment(AppModel.self) private var app
    let draft: BillDraft

    @StateObject private var camera = CameraController()
    @State private var phase: Phase = .framing
    @State private var photoItem: PhotosPickerItem?
    @State private var showingPicker = false
    @State private var failed = false

    enum Phase: Equatable {
        case framing
        case reading(UIImage)

        static func == (a: Phase, b: Phase) -> Bool {
            switch (a, b) {
            case (.framing, .framing): true
            case let (.reading(x), .reading(y)): x === y
            default: false
            }
        }
    }

    private var isReading: Bool { phase != .framing }
    private var hasCamera: Bool { camera.status == .ready }

    var body: some View {
        // Schermata fissa, senza scroll: le posizioni sono quelle del mockup. Su telefoni più bassi
        // si accorcia il mirino, non il resto.
        VStack(spacing: 0) {
            BackButton()
                .padding(.top, 3.5)

            TightTitle(text: "Scan the\nreceipt", style: .scanTitle, alignment: .center)
                .multilineTextAlignment(.center)
                .padding(.top, 16.5)

            Text("Frame the receipt: we'll pick out\nthe items and prices.")
                .textStyle(.lead16)
                .foregroundStyle(Palette.inkSoft)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 7.5)
                .padding(.horizontal, 30)

            previewCard
                .padding(.top, 20.5)
                .padding(.horizontal, 23.5)
                .layoutPriority(1)

            Spacer(minLength: 18)

            Group {
                if hasCamera || isReading {
                    Button { Task { await shoot() } } label: {
                        Label { Text(isReading ? "Reading…" : "Capture receipt") } icon: {
                            Image(systemName: "camera").font(.system(size: 19, weight: .medium))
                        }
                        .labelStyle(SpacedLabelStyle())
                    }
                } else {
                    Button { showingPicker = true } label: {
                        Label { Text("Choose a photo") } icon: {
                            Image(systemName: "photo").font(.system(size: 18, weight: .medium))
                        }
                        .labelStyle(SpacedLabelStyle())
                    }
                }
            }
            .buttonStyle(PrimaryButtonStyle())
            .disabled(isReading || camera.status == .starting)
            .padding(.horizontal, 24)

            Button("Continue manually") {
                app.typeItemsInstead()
            }
            .textStyle(.link)
            .foregroundStyle(Palette.raspberryDeep)
            .padding(.top, 21)
            .padding(.bottom, 3)
            .disabled(isReading)
        }
        .frame(maxWidth: .infinity)
        .background(BackdropView())
        .toolbar(.hidden, for: .navigationBar)
        .photosPicker(isPresented: $showingPicker, selection: $photoItem, matching: .images)
        .onChange(of: photoItem) { _, item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self), let image = UIImage(data: data) {
                    await read(image)
                }
                photoItem = nil
            }
        }
        .task { await camera.start() }
        .onDisappear { camera.stop() }
        .alert("Couldn't read the receipt", isPresented: $failed) {
            Button("Try again", role: .cancel) {}
            Button("Enter items manually") { app.typeItemsInstead() }
        } message: {
            Text("Try again with more light, holding the receipt flat and fully in frame.")
        }
    }

    // MARK: Card anteprima

    private var previewCard: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Preview")
                    .textStyle(TextStyle(face: .semibold, size: 16))
                    .foregroundStyle(Palette.ink)
                Spacer()
                Text(badge)
                    .textStyle(TextStyle(face: .semibold, size: 13.5))
                    .foregroundStyle(Palette.raspberry)
                    .padding(.horizontal, 12)
                    .frame(height: 24)
                    .background(Capsule().fill(Palette.pinkSoft))
                    .contentTransition(.opacity)
                    .animation(.snappy, value: badge)
            }
            .padding(.horizontal, 22)
            .frame(height: 46)
            .padding(.top, 9)

            viewfinder
                .padding(.horizontal, 14)
                .padding(.bottom, 13)
        }
        .card()
    }

    private var badge: String {
        if isReading { return "Reading…" }
        switch camera.status {
        case .starting: return "Starting…"
        case .ready: return "Ready"
        case .unavailable: return "Photo"
        case .denied: return "Photo"
        }
    }

    private var viewfinder: some View {
        ZStack {
            Palette.previewDark

            switch phase {
            case .reading(let image):
                // Dentro un overlay: un'immagine "fill" libera allargherebbe lo ZStack e il mirino.
                Color.clear
                    .overlay { Image(uiImage: image).resizable().scaledToFill() }
                    .clipped()
                    .transition(.opacity)
                ScanLine()
            case .framing:
                if hasCamera {
                    CameraPreview(session: camera.session)
                        .transition(.opacity)
                } else {
                    SampleReceipt()
                        .padding(.horizontal, 54)
                        .padding(.vertical, 31)
                }
            }

            CornerBrackets()
                .padding(.horizontal, 18.5)
                .padding(.vertical, 17.5)
                .allowsHitTesting(false)

            if camera.status == .denied && !isReading {
                VStack {
                    Spacer()
                    Button("Allow camera access") {
                        if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
                    }
                    .textStyle(TextStyle(face: .semibold, size: 13.5))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 14)
                    .frame(height: 32)
                    .background(Capsule().fill(.black.opacity(0.45)))
                    .padding(.bottom, 16)
                }
            }

            if hasCamera && !isReading {
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        Button { showingPicker = true } label: {
                            Image(systemName: "photo.on.rectangle")
                                .font(.system(size: 15, weight: .medium))
                                .foregroundStyle(.white)
                                .frame(width: 38, height: 38)
                                .background(Circle().fill(.black.opacity(0.35)))
                        }
                        .accessibilityLabel("Choose a photo")
                        .padding(14)
                    }
                }
            }
        }
        .frame(minHeight: 170, maxHeight: 345)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .animation(.easeInOut(duration: 0.3), value: phase)
    }

    // MARK: Azioni

    private func shoot() async {
        guard hasCamera else { showingPicker = true; return }
        Haptics.tap()
        do {
            let image = try await camera.capture()
            await read(image)
        } catch {
            failed = true
        }
    }

    private func read(_ image: UIImage) async {
        phase = .reading(image)
        camera.stop()
        let started = Date()
        let parsed = try? await ReceiptReader.read(image, currency: draft.currency)
        // La lettura è velocissima: l'animazione deve restare il tempo di capire cosa succede.
        let elapsed = Date().timeIntervalSince(started)
        if elapsed < 1.1 { try? await Task.sleep(for: .seconds(1.1 - elapsed)) }

        guard let parsed, !parsed.items.isEmpty else {
            Haptics.warning()
            phase = .framing
            failed = true
            await camera.start()
            return
        }
        Haptics.success()
        draft.source = .scan
        draft.items = parsed.items
        draft.printedTotal = parsed.total
        app.path.append(.review)
        try? await Task.sleep(for: .seconds(0.6))
        phase = .framing
    }
}

private struct SpacedLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: 11) {
            configuration.icon
            configuration.title
        }
    }
}

/// Gli angoli rosa del mirino.
private struct CornerBrackets: View {
    var body: some View {
        GeometryReader { geo in
            let arm: CGFloat = 36, r: CGFloat = 9
            let w = geo.size.width, h = geo.size.height
            Path { p in
                // alto a sinistra
                p.move(to: CGPoint(x: 0, y: arm)); p.addLine(to: CGPoint(x: 0, y: r))
                p.addQuadCurve(to: CGPoint(x: r, y: 0), control: .zero); p.addLine(to: CGPoint(x: arm, y: 0))
                // alto a destra
                p.move(to: CGPoint(x: w - arm, y: 0)); p.addLine(to: CGPoint(x: w - r, y: 0))
                p.addQuadCurve(to: CGPoint(x: w, y: r), control: CGPoint(x: w, y: 0)); p.addLine(to: CGPoint(x: w, y: arm))
                // basso a destra
                p.move(to: CGPoint(x: w, y: h - arm)); p.addLine(to: CGPoint(x: w, y: h - r))
                p.addQuadCurve(to: CGPoint(x: w - r, y: h), control: CGPoint(x: w, y: h)); p.addLine(to: CGPoint(x: w - arm, y: h))
                // basso a sinistra
                p.move(to: CGPoint(x: arm, y: h)); p.addLine(to: CGPoint(x: r, y: h))
                p.addQuadCurve(to: CGPoint(x: 0, y: h - r), control: CGPoint(x: 0, y: h)); p.addLine(to: CGPoint(x: 0, y: h - arm))
            }
            .stroke(Palette.bracket, style: StrokeStyle(lineWidth: 3, lineCap: .round))
        }
    }
}

/// La riga luminosa che scorre mentre si legge lo scontrino.
private struct ScanLine: View {
    @State private var down = false

    var body: some View {
        GeometryReader { geo in
            LinearGradient(colors: [Palette.pink.opacity(0), Palette.pink.opacity(0.55), Palette.pink.opacity(0)],
                           startPoint: .top, endPoint: .bottom)
                .frame(height: 70)
                .overlay(Rectangle().fill(Palette.bracket).frame(height: 2))
                .offset(y: down ? geo.size.height - 35 : -35)
                .onAppear {
                    withAnimation(.easeInOut(duration: 1.1).repeatForever(autoreverses: true)) { down = true }
                }
        }
        .background(Color.black.opacity(0.18))
        .allowsHitTesting(false)
    }
}

/// Lo scontrino di esempio del mockup, mostrato quando non c'è una fotocamera da usare.
/// Le due righe sbiadite ricordano che l'OCR a volte sbaglia (e che l'app poi chiede conferma).
private struct SampleReceipt: View {
    private let rows: [(String, String, Bool)] = [("MARGHERITA", "8.00", false), ("CARBONARA", "12.00", false),
                                                  ("DRAFT BEER", "5.00", false), ("STILL WATER", "2.00", false),
                                                  ("TIRAMIS▒", "6.00", true), ("COVER", "7.▒0", false)]

    var body: some View {
        VStack(spacing: 0) {
            Text("TRATTORIA DA NINO")
                .font(.custom("Courier New", size: 11).weight(.bold))
                .padding(.top, 20)
            Dashes().padding(.top, 11)
            VStack(spacing: 10.5) {
                ForEach(rows, id: \.0) { row in
                    HStack {
                        Text(row.0)
                        Spacer()
                        Text(row.1)
                    }
                    .opacity(row.2 ? 0.45 : 1)
                }
            }
            .font(.custom("Courier New", size: 10))
            .padding(.top, 12)
            Dashes().padding(.top, 11)
            HStack {
                Text("TOTAL")
                Spacer()
                Text("40.00")
            }
            .font(.custom("Courier New", size: 10).weight(.bold))
            .padding(.top, 12)
            Spacer(minLength: 0)
        }
        .foregroundStyle(Color(hex: 0x3A3033))
        .padding(.horizontal, 17)
        .background(Palette.paper)
        .rotationEffect(.degrees(-1.5))
        .accessibilityHidden(true)
    }

    private struct Dashes: View {
        var body: some View {
            Line()
                .stroke(Color(hex: 0x9C9296), style: StrokeStyle(lineWidth: 0.8, dash: [3, 2]))
                .frame(height: 1)
        }
    }

    private struct Line: Shape {
        func path(in rect: CGRect) -> Path {
            Path { p in
                p.move(to: CGPoint(x: 0, y: rect.midY))
                p.addLine(to: CGPoint(x: rect.maxX, y: rect.midY))
            }
        }
    }
}
