import XCTest
@testable import ChiPaga

final class SettlementTests: XCTestCase {

    private func people(_ names: String...) -> [Person] { names.map { Person(name: $0) } }

    /// L'esempio del brief: 7 amici, €70, Cla ha messo 50 e Marco 20.
    func testEqualSplitSevenFriends() {
        let p = people("Cla", "Marco", "Anna", "Luca", "Sara", "Giulia", "Paolo")
        let draft = BillDraft(source: .equal)
        draft.people = p
        draft.equalTotalText = "70"
        draft.paidText[p[0].id] = "50"
        draft.paidText[p[1].id] = "20"

        let s = draft.settlement()
        XCTAssertEqual(s.transfers.count, 5)
        XCTAssertTrue(s.transfers.allSatisfy { $0.cents == 1000 })
        XCTAssertEqual(s.transfers.filter { $0.to.name == "Cla" }.count, 4)
        XCTAssertEqual(s.transfers.filter { $0.to.name == "Marco" }.count, 1)
        XCTAssertEqual(s.lines.map(\.owed).reduce(0, +), 7000)
    }

    /// Il conto del mockup: scontrino da €40, Cla paga 30 e Marco 10.
    func testItemizedMockupBill() {
        let p = people("Cla", "Marco", "Giulia", "Luca")
        let (cla, marco, giulia, luca) = (p[0].id, p[1].id, p[2].id, p[3].id)
        let draft = BillDraft(source: .scan)
        draft.people = p
        draft.items = [
            BillItem(name: "Pizza margherita", cents: 800, assignees: [cla]),
            BillItem(name: "Carbonara", cents: 1200, assignees: [marco]),
            BillItem(name: "Birra media", cents: 500, assignees: [cla, giulia]),
            BillItem(name: "Acqua naturale", cents: 200, assignees: [luca]),
            BillItem(name: "Tiramisù", cents: 600, assignees: [giulia]),
            BillItem(name: "Coperto", cents: 700, assignees: [cla, marco, giulia, luca]),
        ]
        draft.paidText[cla] = "30"
        draft.paidText[marco] = "10"

        let owed = draft.owed
        XCTAssertEqual(owed[cla], 1225)
        XCTAssertEqual(owed[marco], 1375)
        XCTAssertEqual(owed[giulia], 1025)
        XCTAssertEqual(owed[luca], 375)

        let s = draft.settlement()
        XCTAssertEqual(s.transfers.map { "\($0.from.name)>\($0.to.name):\($0.cents)" },
                       ["Giulia>Cla:1025", "Marco>Cla:375", "Luca>Cla:375"])
    }

    /// Due coppie indipendenti: il greedy puro farebbe 3 trasferimenti, il minimo è 2.
    func testFindsIndependentGroups() {
        // A +5, B -5, C +7, D -7 → A←B e C←D
        let moves = SettlementEngine.minimalTransfers([500, -700, 700, -500])
        XCTAssertEqual(moves.count, 2)
        XCTAssertTrue(moves.contains(.init(from: 3, to: 0, cents: 500)))
        XCTAssertTrue(moves.contains(.init(from: 1, to: 2, cents: 700)))
    }

    func testAlwaysSettlesEveryone() {
        var rng = SystemRandomNumberGenerator()
        for _ in 0..<300 {
            let n = Int.random(in: 2...9, using: &rng)
            var balances = (0..<(n - 1)).map { _ in Int.random(in: -5000...5000, using: &rng) }
            balances.append(-balances.reduce(0, +))
            let moves = SettlementEngine.minimalTransfers(balances)
            var left = balances
            for m in moves {
                XCTAssertGreaterThan(m.cents, 0)
                left[m.from] += m.cents
                left[m.to] -= m.cents
            }
            XCTAssertEqual(left, Array(repeating: 0, count: n))
            XCTAssertLessThanOrEqual(moves.count, max(0, balances.filter { $0 != 0 }.count - 1))
        }
    }

    func testNobodyOwesAnything() {
        XCTAssertTrue(SettlementEngine.minimalTransfers([0, 0, 0]).isEmpty)
    }

    /// €100 in 3: 33,34 + 33,33 + 33,33, e il centesimo in più tocca a chi ha pagato.
    func testOddCentsGoToWhoPaid() {
        let p = people("Anna", "Bea", "Cri")
        let shares = SettlementEngine.equalShares(total: 10000, people: p.map(\.id), paid: [p[2].id: 10000])
        XCTAssertEqual(shares.values.reduce(0, +), 10000)
        XCTAssertEqual(shares[p[2].id], 3334)
        XCTAssertEqual(shares[p[0].id], 3333)
    }

    func testSharedItemSplitsExactly() {
        let p = people("A", "B", "C")
        let item = BillItem(name: "Vino", cents: 1000, assignees: Set(p.map(\.id)))
        let shares = SettlementEngine.itemizedShares(items: [item], people: p.map(\.id))
        XCTAssertEqual(shares.values.reduce(0, +), 1000)
        XCTAssertEqual(Set(shares.values), [333, 334])
    }

    func testMoneyFormatting() {
        XCTAssertEqual(Money.format(2230), "€22,30")
        XCTAssertEqual(Money.format(123450), "€1.234,50")
        XCTAssertEqual(Money.format(5), "€0,05")
        XCTAssertEqual(Money.parse("12"), 1200)
        XCTAssertEqual(Money.parse("12,5"), 1250)
        XCTAssertEqual(Money.parse("12.50"), 1250)
        XCTAssertEqual(Money.parse("€ 1.234,50"), 123450)
        XCTAssertNil(Money.parse(""))
        XCTAssertEqual(Money.sanitizeInput("12,345"), "12,34")
        XCTAssertEqual(Money.sanitizeInput(",5"), "0,5")
        XCTAssertEqual(Money.editable(1200), "12")
        XCTAssertEqual(Money.editable(1250), "12,50")
    }
}
