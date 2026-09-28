import SwiftUI

enum AppTab: Hashable {
    case bills, settings
}

struct RootView: View {
    @State private var app = AppModel()
    @State private var coach = Coach()
    @State private var tab = AppTab.bills
    @State private var settingsPath: [SettingsRoute] = []
    @AppStorage("appearance") private var appearance = Appearance.system
    @AppStorage("didOnboard") private var didOnboard = false

    var body: some View {
        GeometryReader { geo in
            ZStack {
                TabView(selection: $tab) {
                    NavigationStack(path: $app.path) {
                        HomeView()
                            .navigationDestination(for: Route.self) { route in
                                destination(route)
                                    .toolbar(.hidden, for: .tabBar)
                            }
                    }
                    .tabItem { Label("Bills", systemImage: "receipt") }
                    .tag(AppTab.bills)

                    NavigationStack(path: $settingsPath) {
                        SettingsView(path: $settingsPath) { replayTutorial() }
                    }
                    .tabItem { Label("Settings", systemImage: "gearshape") }
                    .tag(AppTab.settings)
                }
                .tint(Palette.pink)

                if app.showingNewBill {
                    Palette.scrim.opacity(0.32)
                        .ignoresSafeArea()
                        .onTapGesture { app.showingNewBill = false }
                        .transition(.opacity)
                        .accessibilityLabel("Close")
                        .accessibilityAddTraits(.isButton)

                    NewBillSheet()
                        .transition(.move(edge: .bottom))
                        .zIndex(1)
                }

                if coach.isActive {
                    CoachOverlay(coach: coach, onNext: next, onSkip: endTutorial)
                        .zIndex(2)
                        .transition(.opacity)
                }
            }
            .animation(.spring(response: 0.42, dampingFraction: 0.88), value: app.showingNewBill)
            .environment(\.screenHeight, geo.size.height + geo.safeAreaInsets.top + geo.safeAreaInsets.bottom)
        }
        .environment(app)
        .environment(\.coach, coach)
        .preferredColorScheme(appearance.colorScheme)
        .fullScreenCover(isPresented: onboardingBinding) {
            OnboardingView {
                didOnboard = true
                Task {
                    try? await Task.sleep(for: .seconds(0.6))
                    startTutorial()
                }
            }
            .preferredColorScheme(appearance.colorScheme)
        }
        .onChange(of: app.showingNewBill) { _, open in
            // Al primo passo si tocca il + vero: quando il foglio si apre, si passa alle due opzioni.
            if open, coach.step == .plus {
                Task {
                    try? await Task.sleep(for: .seconds(0.45))
                    coach.step = .scan
                }
            }
        }
        .task {
            #if DEBUG
            if DemoSeed.screen == "tutorial" { startTutorial() }
            #endif
        }
    }

    private var onboardingBinding: Binding<Bool> {
        Binding(get: {
            #if DEBUG
            if let screen = DemoSeed.screen { return screen == "onboarding" && !didOnboard }
            #endif
            return !didOnboard
        }, set: { if !$0 { didOnboard = true } })
    }

    // MARK: Tutorial

    private func startTutorial() {
        tab = .bills
        app.path = []
        app.showingNewBill = false
        coach.wide = true
        coach.step = .plus
        Task {
            // Prima la patina copre lo schermo, poi si stringe attorno al +.
            try? await Task.sleep(for: .seconds(0.05))
            coach.wide = false
        }
    }

    private func replayTutorial() {
        settingsPath = []
        startTutorial()
    }

    private func next(_ step: Coach.Step) {
        Haptics.tap()
        switch step {
        case .plus:
            app.showingNewBill = true
        case .scan:
            coach.step = .manual
        case .manual:
            app.showingNewBill = false
            Task {
                try? await Task.sleep(for: .seconds(0.35))
                coach.step = .newBill
            }
        case .newBill:
            endTutorial()
        }
    }

    private func endTutorial() {
        app.showingNewBill = false
        coach.wide = true
        Task {
            try? await Task.sleep(for: .seconds(0.45))
            withAnimation(.easeOut(duration: 0.25)) { coach.step = nil }
        }
    }

    // MARK: Percorso

    @ViewBuilder
    private func destination(_ route: Route) -> some View {
        switch route {
        case .saved(let id):
            if let bill = app.store.bill(id) {
                ResultView(settlement: bill.settlement, date: bill.date, onDone: { app.path = [] })
                    .environment(\.currency, bill.settlement.currency)
            }
        default:
            if let draft = app.draft {
                Group {
                    switch route {
                    case .scan: ScanView(draft: draft)
                    case .review: ReviewView(draft: draft)
                    case .people: PeopleView(draft: draft)
                    case .assign: AssignView(draft: draft)
                    case .payments: PaymentsView(draft: draft)
                    case .result: DraftResultView(draft: draft)
                    case .saved: EmptyView()
                    }
                }
                .environment(\.currency, draft.currency)
            }
        }
    }
}
