import SwiftUI

struct RootView: View {
    @State private var app = AppModel()

    var body: some View {
        GeometryReader { geo in
            ZStack {
                NavigationStack(path: $app.path) {
                    HomeView()
                        .navigationDestination(for: Route.self) { route in
                            destination(route)
                        }
                }

                if app.showingNewBill {
                    Palette.scrim.opacity(0.32)
                        .ignoresSafeArea()
                        .onTapGesture { app.showingNewBill = false }
                        .transition(.opacity)
                        .accessibilityLabel("Chiudi")
                        .accessibilityAddTraits(.isButton)

                    NewBillSheet()
                        .transition(.move(edge: .bottom))
                        .zIndex(1)
                }
            }
            .animation(.spring(response: 0.42, dampingFraction: 0.88), value: app.showingNewBill)
            .environment(\.screenHeight, geo.size.height + geo.safeAreaInsets.top + geo.safeAreaInsets.bottom)
        }
        .environment(app)
    }

    @ViewBuilder
    private func destination(_ route: Route) -> some View {
        switch route {
        case .saved(let id):
            if let bill = app.store.bill(id) {
                ResultView(settlement: bill.settlement, date: bill.date, onDone: { app.path = [] })
            }
        default:
            if let draft = app.draft {
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
        }
    }
}
