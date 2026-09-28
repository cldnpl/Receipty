import Foundation

struct Person: Identifiable, Hashable, Codable {
    var id = UUID()
    var name: String

    var initial: String {
        String(name.trimmingCharacters(in: .whitespaces).prefix(1)).uppercased()
    }
}

struct BillItem: Identifiable, Hashable, Codable {
    enum Issue: String, Codable {
        /// L'OCR non è sicuro del nome.
        case name
        /// L'OCR non è sicuro del prezzo: finché non lo conferma qualcuno non si va avanti.
        case price
    }

    var id = UUID()
    var name: String
    var quantity: Int = 1
    /// Prezzo della riga (quantità già inclusa), in centesimi.
    var cents: Int
    var assignees: Set<UUID> = []
    var issue: Issue? = nil
}

/// Da dove arriva il conto: la schermata iniziale offre solo due strade,
/// ma "Continua manualmente" dallo scanner porta a un conto a voci scritto a mano.
enum BillSource: String, Codable {
    case scan
    case typed
    case equal

    var isItemized: Bool { self != .equal }
}

/// Il risultato di un conto: quanto doveva e quanto ha pagato ciascuno, e chi dà quanto a chi.
///
/// Se sul piatto sono finiti più soldi del totale, il ristorante dà il resto: `changeSplit`
/// dice chi si tiene quanto del resto, e `transfers` quello che resta da saldare dopo.
struct Settlement: Codable, Hashable {
    struct Line: Codable, Hashable, Identifiable {
        var person: Person
        var owed: Int
        var paid: Int
        /// La parte del resto che questa persona si tiene.
        var changeKept: Int = 0
        var id: UUID { person.id }
        var balance: Int { paid - changeKept - owed }
    }

    struct ChangeShare: Codable, Hashable, Identifiable {
        var person: Person
        var cents: Int
        var id: UUID { person.id }
    }

    struct Transfer: Codable, Hashable, Identifiable {
        var from: Person
        var to: Person
        var cents: Int
        var id: String { "\(from.id)-\(to.id)" }
    }

    var total: Int
    var lines: [Line]
    var transfers: [Transfer]
    var changeSplit: [ChangeShare] = []

    var change: Int { changeSplit.reduce(0) { $0 + $1.cents } }
}

struct SavedBill: Codable, Hashable, Identifiable {
    var id: UUID
    var date: Date
    var source: BillSource
    var itemCount: Int
    var settlement: Settlement

    var names: String {
        settlement.lines.map(\.person.name).joined(separator: ", ")
    }
}
