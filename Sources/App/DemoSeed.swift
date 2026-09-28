#if DEBUG
import Foundation

/// Solo in Debug: `-screen <nome>` all'avvio apre una schermata con i dati del mockup,
/// per confrontarla col Figma e per gli screenshot. I recenti finti vivono in una cartella temporanea.
enum DemoSeed {
    static var screen: String? {
        let args = ProcessInfo.processInfo.arguments
        guard let i = args.firstIndex(of: "-screen"), i + 1 < args.count else { return nil }
        return args[i + 1]
    }

    static func makeStore() -> BillStore? {
        guard screen != nil else { return nil }
        let dir = FileManager.default.temporaryDirectory.appendingPathComponent("demo-\(UUID().uuidString)")
        let store = BillStore(directory: dir)
        let cal = Calendar(identifier: .gregorian)
        func date(_ d: Int) -> Date { cal.date(from: DateComponents(year: 2026, month: 9, day: d, hour: 21))! }
        func bill(_ day: Int, _ names: [String], _ total: Int, _ source: BillSource) -> SavedBill {
            let people = names.map { Person(name: $0) }
            let lines = people.map { Settlement.Line(person: $0, owed: total / people.count, paid: 0) }
            return SavedBill(id: UUID(), date: date(day), source: source, itemCount: 3,
                             settlement: Settlement(total: total, lines: lines, transfers: []))
        }
        if screen == "people" {
            // I "Recenti" del mockup.
            store.upsert(bill(2, ["Anna", "Sara", "Paolo"], 6000, .equal))
        } else if screen != "empty" {
            store.upsert(bill(12, ["Chris", "Mia", "Ben"], 4500, .equal))
            store.upsert(bill(18, ["Alice", "Tom", "Sam"], 8400, .equal))
            store.upsert(bill(24, ["Marco", "Sarah", "Leo"], 2230, .scan))
        }
        return store
    }

    static func apply(to app: AppModel) {
        guard let screen else { return }
        let names = ["Cla", "Marco", "Giulia", "Luca"]

        func mockupDraft() -> BillDraft {
            let d = BillDraft(source: .scan, currency: Currency(code: "EUR"))
            d.items = [
                BillItem(name: "Margherita pizza", cents: 800),
                BillItem(name: "Carbonara", cents: 1200),
                BillItem(name: "Draft beer", cents: 500),
                BillItem(name: "Still water", cents: 200),
                BillItem(name: "Tiramis?", cents: 600, issue: .name),
                BillItem(name: "Cover charge", cents: 700, issue: .price),
            ]
            d.printedTotal = 4000
            names.forEach { d.addPerson(named: $0) }
            return d
        }

        func assigned(_ d: BillDraft, full: Bool) {
            let p = d.people.map(\.id)
            d.items[4].name = "Tiramisu"
            d.items[4].issue = nil
            d.items[5].issue = nil
            d.items[0].assignees = [p[0]]
            d.items[1].assignees = [p[1]]
            d.items[2].assignees = [p[0], p[2]]
            if full {
                d.items[3].assignees = [p[3]]
                d.items[4].assignees = [p[2]]
                d.items[5].assignees = Set(p)
            }
        }

        switch screen {
        case "sheet":
            app.showingNewBill = true
        case "scan":
            app.draft = BillDraft(source: .scan, currency: Currency(code: "EUR"))
            app.path = [.scan]
        case "review":
            app.draft = mockupDraft()
            app.path = [.scan, .review]
        case "people":
            app.draft = mockupDraft()
            app.path = [.scan, .review, .people]
        case "assign":
            let d = mockupDraft()
            assigned(d, full: false)
            // Nel mockup la lista è scorsa: la prima card visibile è la Carbonara.
            app.draft = d
            app.path = [.scan, .review, .people, .assign]
        case "payments":
            let d = mockupDraft()
            assigned(d, full: true)
            d.paidText[d.people[0].id] = "30"
            d.paidText[d.people[1].id] = "10"
            app.draft = d
            app.path = [.scan, .review, .people, .assign, .payments]
        case "manual":
            let d = BillDraft(source: .equal, currency: Currency(code: "EUR"))
            names.forEach { d.addPerson(named: $0) }
            d.equalTotalText = "70.00"
            d.paidText[d.people[0].id] = "70"
            app.draft = d
            app.path = [.people, .payments]
        case "result":
            let d = mockupDraft()
            assigned(d, full: true)
            d.paidText[d.people[0].id] = "30"
            d.paidText[d.people[1].id] = "10"
            app.draft = d
            app.path = [.scan, .review, .people, .assign, .payments, .result]
        case "change", "changeresult":
            // Il caso del resto: conto da €52 in 4, Claudia mette 20 e Marco 40.
            let d = BillDraft(source: .equal, currency: Currency(code: "EUR"))
            ["Claudia", "Marco", "Olga", "Kekko"].forEach { d.addPerson(named: $0) }
            d.equalTotalText = "52"
            d.paidText[d.people[0].id] = "20"
            d.paidText[d.people[1].id] = "40"
            app.draft = d
            app.path = screen == "change" ? [.people, .payments] : [.people, .payments, .result]
        default:
            break
        }
    }
}
#endif
