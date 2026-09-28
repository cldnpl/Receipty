import Foundation
import Observation

/// I conti recenti, in un file JSON sul telefono. Niente account, niente cloud.
@Observable
final class BillStore {
    private(set) var bills: [SavedBill] = []
    private let url: URL
    private let limit = 30

    init(directory: URL? = nil) {
        let dir = directory ?? FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        url = dir.appendingPathComponent("bills.json")
        if let data = try? Data(contentsOf: url),
           let decoded = try? JSONDecoder().decode([SavedBill].self, from: data) {
            bills = decoded
        }
    }

    func bill(_ id: UUID) -> SavedBill? { bills.first { $0.id == id } }

    func upsert(_ bill: SavedBill) {
        bills.removeAll { $0.id == bill.id }
        bills.insert(bill, at: 0)
        bills.sort { $0.date > $1.date }
        if bills.count > limit { bills.removeLast(bills.count - limit) }
        persist()
    }

    func delete(_ id: UUID) {
        bills.removeAll { $0.id == id }
        persist()
    }

    /// I nomi dei conti passati, dal più recente, per aggiungerli al tavolo con un tocco.
    func recentNames(excluding current: [String], limit: Int = 6) -> [String] {
        var seen = Set(current.map { $0.lowercased() })
        var names: [String] = []
        for bill in bills {
            for line in bill.settlement.lines where !seen.contains(line.person.name.lowercased()) {
                seen.insert(line.person.name.lowercased())
                names.append(line.person.name)
                if names.count == limit { return names }
            }
        }
        return names
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(bills) else { return }
        try? data.write(to: url, options: [.atomic])
    }
}
