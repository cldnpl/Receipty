import Foundation
import Observation

/// Il conto che si sta dividendo, dalla prima schermata al risultato.
@Observable
final class BillDraft {
    let id = UUID()
    let createdAt = Date()
    var source: BillSource
    /// Presa dalle impostazioni quando il conto nasce: cambiarla dopo non tocca questo conto.
    let currency: Currency

    var items: [BillItem] = []
    /// Il totale stampato sullo scontrino, se l'OCR l'ha trovato: serve solo a verificare le voci.
    var printedTotal: Int?

    var people: [Person] = []
    var paidText: [UUID: String] = [:]
    /// Solo per la divisione in parti uguali.
    var equalTotalText = ""

    init(source: BillSource, currency: Currency = .current) {
        self.source = source
        self.currency = currency
    }

    // MARK: Totali

    var itemsTotal: Int { items.reduce(0) { $0 + $1.cents } }

    var total: Int {
        source.isItemized ? itemsTotal : max(0, Money.parse(equalTotalText) ?? 0)
    }

    var itemsToCheck: Int { items.filter { $0.issue != nil }.count }
    var unassignedCount: Int { items.filter { item in !people.contains { item.assignees.contains($0.id) } }.count }

    func paid(_ person: Person) -> Int {
        max(0, Money.parse(paidText[person.id] ?? "") ?? 0)
    }

    var paidTotal: Int { people.reduce(0) { $0 + paid($1) } }

    var owed: [UUID: Int] {
        let ids = people.map(\.id)
        if source.isItemized {
            return SettlementEngine.itemizedShares(items: items, people: ids, unit: currency.unit)
        }
        let paid = Dictionary(uniqueKeysWithValues: people.map { ($0.id, self.paid($0)) })
        return SettlementEngine.equalShares(total: total, people: ids, paid: paid, unit: currency.unit)
    }

    /// Si può calcolare quando sul piatto c'è almeno il totale: quello che avanza è il resto.
    var canSettle: Bool { total > 0 && paidTotal >= total }
    var change: Int { max(0, paidTotal - total) }

    func settlement() -> Settlement {
        let paid = Dictionary(uniqueKeysWithValues: people.map { ($0.id, self.paid($0)) })
        var result = SettlementEngine.settle(people: people, owed: owed, paid: paid, total: total)
        result.currency = currency
        return result
    }

    // MARK: Persone

    /// Aggiunge qualcuno al tavolo. Due persone con lo stesso nome diventano "Marco" e "Marco 2".
    @discardableResult
    func addPerson(named raw: String) -> Person? {
        let name = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else { return nil }
        var unique = name
        var n = 2
        while people.contains(where: { $0.name.caseInsensitiveCompare(unique) == .orderedSame }) {
            unique = "\(name) \(n)"
            n += 1
        }
        let person = Person(name: unique)
        people.append(person)
        return person
    }

    func remove(_ person: Person) {
        people.removeAll { $0.id == person.id }
        paidText[person.id] = nil
        for i in items.indices { items[i].assignees.remove(person.id) }
    }

    // MARK: Chi ha preso cosa

    func toggle(_ person: Person, on itemID: BillItem.ID) {
        guard let i = items.firstIndex(where: { $0.id == itemID }) else { return }
        if items[i].assignees.contains(person.id) {
            items[i].assignees.remove(person.id)
        } else {
            items[i].assignees.insert(person.id)
        }
    }

    func toggleEveryone(on itemID: BillItem.ID) {
        guard let i = items.firstIndex(where: { $0.id == itemID }) else { return }
        let everyone = Set(people.map(\.id))
        items[i].assignees = items[i].assignees.isSuperset(of: everyone) ? [] : everyone
    }

    /// "Tutto": questa persona ha pagato l'intero conto, gli altri niente.
    func paidEverything(_ person: Person) {
        for p in people { paidText[p.id] = "" }
        paidText[person.id] = Money.editable(total)
    }
}
