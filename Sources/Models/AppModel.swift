import Foundation
import Observation

enum Route: Hashable {
    case scan
    case review
    case people
    case assign
    case payments
    case result
    case saved(UUID)
}

@Observable
final class AppModel {
    var path: [Route] = []
    var draft: BillDraft?
    var showingNewBill = false
    let store: BillStore

    init() {
        #if DEBUG
        store = DemoSeed.makeStore() ?? BillStore()
        DemoSeed.apply(to: self)
        #else
        store = BillStore()
        #endif
    }

    func startScan() {
        draft = BillDraft(source: .scan)
        showingNewBill = false
        path = [.scan]
    }

    func startEqualSplit() {
        draft = BillDraft(source: .equal)
        showingNewBill = false
        path = [.people]
    }

    /// Dallo scanner: niente foto, le voci si scrivono a mano.
    func typeItemsInstead() {
        draft?.source = .typed
        path.append(.review)
    }

    /// Salva il conto tra i recenti (sovrascrivendo se si è tornati indietro a correggere).
    func record(_ draft: BillDraft) {
        store.upsert(SavedBill(id: draft.id,
                               date: draft.createdAt,
                               source: draft.source,
                               itemCount: draft.items.count,
                               settlement: draft.settlement()))
    }

    func finish() {
        path = []
        draft = nil
    }
}
