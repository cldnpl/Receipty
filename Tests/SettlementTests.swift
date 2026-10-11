import XCTest
@testable import Receipty

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

    /// Gli importi si scrivono come vuole la lingua dell'app ("22,30 €" in italiano):
    /// qui la si fissa sull'inglese, altrimenti il test dipenderebbe dall'iPhone che lo esegue.
    func testMoneyFormatting() {
        let language = I18n.shared.language
        I18n.shared.language = .english
        defer { I18n.shared.language = language }

        let eur = Currency(code: "EUR")
        XCTAssertEqual(Money.format(2230, eur), "€22.30")
        XCTAssertEqual(Money.format(123450, eur), "€1,234.50")
        XCTAssertEqual(Money.format(5, eur), "€0.05")
        XCTAssertEqual(Money.format(-2230, eur), "−€22.30")
        XCTAssertEqual(Money.format(120000, Currency(code: "JPY")), "¥1,200")
        XCTAssertEqual(Money.format(999, Currency(code: "USD")), "$9.99")
        XCTAssertEqual(Money.format(1500, Currency(code: "GBP")), "£15.00")
        XCTAssertEqual(Money.formatCompact(0, eur), "€0")
        XCTAssertEqual(Money.parse("1,234.50"), 123450)
        XCTAssertEqual(Money.parse("12"), 1200)
        XCTAssertEqual(Money.parse("12,5"), 1250)
        XCTAssertEqual(Money.parse("12.50"), 1250)
        XCTAssertEqual(Money.parse("€ 1.234,50"), 123450)
        XCTAssertNil(Money.parse(""))
        XCTAssertEqual(Money.sanitizeInput("12,345"), "12.34")
        XCTAssertEqual(Money.sanitizeInput(",5"), "0.5")
        XCTAssertEqual(Money.editable(1200), "12")
        XCTAssertEqual(Money.editable(1250), "12.50")
    }

    /// Valute senza decimali: ¥1000 in 3 fa 334 + 333 + 333 yen, mai frazioni di yen.
    func testZeroDecimalCurrencySplitsInWholeUnits() {
        let draft = BillDraft(source: .equal, currency: Currency(code: "JPY"))
        draft.equalTotalText = "1000"
        let p = ["Aki", "Ben", "Chie"].map { draft.addPerson(named: $0)! }
        draft.paidText[p[0].id] = "1000"
        let owed = draft.owed
        XCTAssertEqual(owed.values.reduce(0, +), 100_000)
        XCTAssertTrue(owed.values.allSatisfy { $0 % 100 == 0 })
        XCTAssertEqual(owed[p[0].id], 33_400)
        let s = draft.settlement()
        XCTAssertEqual(s.currency.code, "JPY")
        XCTAssertEqual(s.transfers.map(\.cents), [33_300, 33_300])
    }

    func testCurrencyListIsUsable() {
        XCTAssertGreaterThan(Currency.all.count, 50)
        XCTAssertTrue(Currency.all.allSatisfy { $0.minorDigits == 0 || $0.minorDigits == 2 })
        XCTAssertEqual(Set(Currency.all.map(\.code)).count, Currency.all.count)
    }

    // MARK: Sconto

    /// €80 in 4 con il 10% di sconto: si pagano €72, €18 a testa.
    func testPercentDiscountOnEqualSplit() {
        let draft = BillDraft(source: .equal, currency: Currency(code: "EUR"))
        draft.equalTotalText = "80"
        ["Anna", "Bea", "Cri", "Dani"].forEach { draft.addPerson(named: $0) }
        draft.hasDiscount = true
        draft.discountText = "10"
        XCTAssertEqual(draft.subtotal, 8000)
        XCTAssertEqual(draft.discount, 800)
        XCTAssertEqual(draft.total, 7200)
        XCTAssertTrue(draft.owed.values.allSatisfy { $0 == 1800 })
        draft.paidText[draft.people[0].id] = "72"
        let s = draft.settlement()
        XCTAssertEqual(s.total, 7200)
        XCTAssertEqual(s.discount, 800)
        XCTAssertEqual(s.change, 0)
        XCTAssertEqual(s.transfers.map(\.cents), [1800, 1800, 1800])
    }

    /// Lo scontrino del mockup (€40) con €4 di sconto: ogni quota cala del 10%, la somma fa €36.
    func testAmountDiscountOnItemizedBill() {
        let p = people("Cla", "Marco", "Giulia", "Luca")
        let (cla, marco, giulia, luca) = (p[0].id, p[1].id, p[2].id, p[3].id)
        let draft = BillDraft(source: .scan, currency: Currency(code: "EUR"))
        draft.people = p
        draft.items = [
            BillItem(name: "Pizza margherita", cents: 800, assignees: [cla]),
            BillItem(name: "Carbonara", cents: 1200, assignees: [marco]),
            BillItem(name: "Birra media", cents: 500, assignees: [cla, giulia]),
            BillItem(name: "Acqua naturale", cents: 200, assignees: [luca]),
            BillItem(name: "Tiramisù", cents: 600, assignees: [giulia]),
            BillItem(name: "Coperto", cents: 700, assignees: [cla, marco, giulia, luca]),
        ]
        draft.hasDiscount = true
        draft.discountMode = .amount
        draft.discountText = "4"
        let owed = draft.owed
        XCTAssertEqual(owed.values.reduce(0, +), 3600)
        XCTAssertEqual(owed[marco], 1238)
        XCTAssertTrue([1102, 1103].contains(owed[cla]!))
        XCTAssertTrue([922, 923].contains(owed[giulia]!))
        XCTAssertTrue([337, 338].contains(owed[luca]!))
        draft.paidText[cla] = "36"
        let s = draft.settlement()
        XCTAssertEqual(s.total, 3600)
        XCTAssertEqual(s.discount, 400)
        XCTAssertEqual(s.transfers.map(\.cents).reduce(0, +), 3600 - s.lines[0].owed)
    }

    /// Lo sconto non va mai oltre il totale, né in soldi né in percentuale.
    func testDiscountNeverExceedsTotal() {
        let draft = BillDraft(source: .equal, currency: Currency(code: "EUR"))
        draft.equalTotalText = "52"
        draft.hasDiscount = true
        draft.discountMode = .amount
        draft.discountText = "100"
        XCTAssertEqual(draft.total, 0)
        draft.discountMode = .percent
        draft.discountText = "150"
        XCTAssertEqual(draft.total, 0)
        draft.hasDiscount = false
        XCTAssertEqual(draft.total, 5200)
    }

    /// Con lo yen lo sconto si arrotonda a yen interi: ¥999 al 15% fa ¥150 di sconto.
    func testDiscountRoundsToWholeYen() {
        let draft = BillDraft(source: .equal, currency: Currency(code: "JPY"))
        draft.equalTotalText = "999"
        draft.hasDiscount = true
        draft.discountText = "15"
        XCTAssertEqual(draft.discount, 15_000)
        XCTAssertEqual(draft.total, 84_900)
    }

    // MARK: Resto

    private func equalBill(total: String, _ payers: [(String, String)], others: [String]) -> Settlement {
        let draft = BillDraft(source: .equal, currency: Currency(code: "EUR"))
        draft.equalTotalText = total
        for (name, amount) in payers {
            let p = draft.addPerson(named: name)!
            draft.paidText[p.id] = amount
        }
        others.forEach { draft.addPerson(named: $0) }
        return draft.settlement()
    }

    /// €52 in 4, Claudia mette 20 e Marco 40: tornano €8.
    func testChangeGoesToWhoOverpaid() {
        let s = equalBill(total: "52", [("Claudia", "20"), ("Marco", "40")], others: ["Olga", "Kekko"])
        XCTAssertEqual(s.change, 800)
        XCTAssertEqual(s.changeSplit.map { "\($0.person.name):\($0.cents)" }, ["Claudia:700", "Marco:100"])
        XCTAssertEqual(s.transfers.map { "\($0.from.name)>\($0.to.name):\($0.cents)" }, ["Olga>Marco:1300", "Kekko>Marco:1300"])
        XCTAssertTrue(s.lines.allSatisfy { $0.owed == 1300 })
    }

    /// Il resto copre chi ha messo poco in più, così restano meno trasferimenti.
    func testChangeMinimisesTransfers() {
        let s = equalBill(total: "52", [("Claudia", "50"), ("Marco", "20")], others: ["Olga", "Kekko"])
        XCTAssertEqual(s.change, 1800)
        XCTAssertEqual(Set(s.changeSplit.map { "\($0.person.name):\($0.cents)" }), ["Marco:700", "Claudia:1100"])
        XCTAssertEqual(s.transfers.count, 2)
        XCTAssertTrue(s.transfers.allSatisfy { $0.to.name == "Claudia" && $0.cents == 1300 })
    }

    /// Una banconota da 100 per un conto da 52: il resto torna tutto a chi l'ha messa.
    func testSinglePayerKeepsAllTheChange() {
        let s = equalBill(total: "52", [("Claudia", "100")], others: ["Marco", "Olga", "Kekko"])
        XCTAssertEqual(s.changeSplit.map(\.cents), [4800])
        XCTAssertEqual(s.changeSplit.first?.person.name, "Claudia")
        XCTAssertEqual(s.transfers.count, 3)
    }

    func testChangeAlwaysBalances() {
        for _ in 0..<200 {
            let n = Int.random(in: 2...7)
            let total = Int.random(in: 1000...20000)
            let people = (0..<n).map { Person(name: "P\($0)") }
            let owed = SettlementEngine.equalShares(total: total, people: people.map(\.id), paid: [:])
            var paid: [UUID: Int] = [:]
            var put = 0
            for p in people.shuffled().prefix(Int.random(in: 1...n)) {
                let v = Int.random(in: 0...total)
                paid[p.id] = v
                put += v
            }
            if put < total { paid[people[0].id, default: 0] += total - put + Int.random(in: 0...3000) }
            let s = SettlementEngine.settle(people: people, owed: owed, paid: paid, total: total)
            XCTAssertEqual(s.change, paid.values.reduce(0, +) - total)
            var left = Dictionary(uniqueKeysWithValues: s.lines.map { ($0.person.id, $0.balance) })
            for t in s.transfers {
                left[t.from.id]! += t.cents
                left[t.to.id]! -= t.cents
            }
            XCTAssertTrue(left.values.allSatisfy { $0 == 0 })
            XCTAssertTrue(s.lines.allSatisfy { $0.changeKept <= max(0, $0.paid - $0.owed) })
        }
    }
}
