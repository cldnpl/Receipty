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

    /// Lo sconto sul totale, se c'è: si aggiunge in "Chi ha pagato?".
    var hasDiscount = false
    var discountMode = DiscountMode.percent
    var discountText = ""

    init(source: BillSource, currency: Currency = .current) {
        self.source = source
        self.currency = currency
    }

    // MARK: Totali

    var itemsTotal: Int { items.reduce(0) { $0 + $1.cents } }

    /// Il totale prima dello sconto: le voci, o quello scritto a mano.
    var subtotal: Int {
        source.isItemized ? itemsTotal : max(0, Money.parse(equalTotalText) ?? 0)
    }

    /// Lo sconto in centesimi: arrotondato al passo della valuta, mai più del totale.
    var discount: Int {
        guard hasDiscount, let value = Money.parse(discountText), value > 0 else { return 0 }
        // "10" in percentuale arriva come 1000: centesimi di punto, 10000 = 100%.
        let raw = discountMode == .percent
            ? Double(subtotal) * Double(min(value, 10_000)) / 10_000
            : Double(value)
        let unit = currency.unit
        return min(subtotal, Int((raw / Double(unit)).rounded()) * unit)
    }

    /// Quello che il tavolo paga davvero: il totale meno lo sconto.
    var total: Int { subtotal - discount }

    var itemsToCheck: Int { items.filter { $0.issue != nil }.count }
    var unassignedCount: Int { items.filter { item in !people.contains { item.assignees.contains($0.id) } }.count }

    func paid(_ person: Person) -> Int {
        max(0, Money.parse(paidText[person.id] ?? "") ?? 0)
    }

    var paidTotal: Int { people.reduce(0) { $0 + paid($1) } }

    var owed: [UUID: Int] {
        let ids = people.map(\.id)
        if source.isItemized {
            let shares = SettlementEngine.itemizedShares(items: items, people: ids, unit: currency.unit)
            return SettlementEngine.applyDiscount(shares, order: ids, subtotal: subtotal, discount: discount, unit: currency.unit)
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
        result.discount = discount
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
