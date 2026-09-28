import Foundation

/// La matematica del conto. Tutto in centesimi interi.
enum SettlementEngine {

    // MARK: Quote

    /// Quanto ha consumato ciascuno: ogni voce si divide in parti uguali tra chi l'ha presa.
    /// Le frazioni di centesimo si assegnano col metodo del resto più grande, così la somma
    /// delle quote è esattamente il totale delle voci.
    static func itemizedShares(items: [BillItem], people: [UUID]) -> [UUID: Int] {
        var exact = Dictionary(uniqueKeysWithValues: people.map { ($0, 0.0) })
        var total = 0
        for item in items {
            let who = people.filter { item.assignees.contains($0) }
            guard !who.isEmpty else { continue }
            total += item.cents
            let each = Double(item.cents) / Double(who.count)
            for id in who { exact[id, default: 0] += each }
        }
        return roundPreservingSum(exact, order: people, total: total, priority: [:])
    }

    /// Divisione in parti uguali. I centesimi che avanzano (es. €100 in 3) vanno a chi ha
    /// pagato di più: così chi deve dare soldi paga cifre tonde.
    static func equalShares(total: Int, people: [UUID], paid: [UUID: Int]) -> [UUID: Int] {
        guard !people.isEmpty else { return [:] }
        let each = Double(total) / Double(people.count)
        let exact = Dictionary(uniqueKeysWithValues: people.map { ($0, each) })
        return roundPreservingSum(exact, order: people, total: total, priority: paid)
    }

    static func roundPreservingSum(_ exact: [UUID: Double], order: [UUID], total: Int, priority: [UUID: Int]) -> [UUID: Int] {
        guard !order.isEmpty else { return [:] }
        var result: [UUID: Int] = [:]
        var assigned = 0
        for id in order {
            let v = Int((exact[id] ?? 0).rounded(.down))
            result[id] = v
            assigned += v
        }
        var remaining = total - assigned
        let ranked = order.enumerated().sorted { a, b in
            let fa = (exact[a.element] ?? 0) - Double(result[a.element] ?? 0)
            let fb = (exact[b.element] ?? 0) - Double(result[b.element] ?? 0)
            if abs(fa - fb) > 1e-9 { return fa > fb }
            let pa = priority[a.element] ?? 0, pb = priority[b.element] ?? 0
            if pa != pb { return pa > pb }
            return a.offset < b.offset
        }.map(\.element)
        var i = 0
        while remaining > 0 {
            result[ranked[i % ranked.count], default: 0] += 1
            remaining -= 1
            i += 1
        }
        while remaining < 0 {
            result[ranked[ranked.count - 1 - (i % ranked.count)], default: 0] -= 1
            remaining += 1
            i += 1
        }
        return result
    }

    // MARK: Trasferimenti

    struct Move: Equatable {
        var from: Int
        var to: Int
        var cents: Int
    }

    /// Il numero minimo di trasferimenti che azzera tutti i saldi.
    ///
    /// Con n persone non in pari servono al massimo n-1 trasferimenti; se però il gruppo si
    /// divide in k sottogruppi che si compensano da soli (es. A deve 10 a B e C deve 5 a D)
    /// ne bastano n-k. Trovare la divisione con più sottogruppi è una programmazione dinamica
    /// sugli insiemi, fattibile fino a una ventina di persone: oltre si usa il solo metodo greedy.
    /// - Parameter balances: pagato meno dovuto, per persona. La somma deve essere zero.
    static func minimalTransfers(_ balances: [Int]) -> [Move] {
        let active = balances.indices.filter { balances[$0] != 0 }
        guard !active.isEmpty else { return [] }

        let groups: [[Int]]
        if active.count <= 18 {
            groups = zeroSumGroups(active.map { balances[$0] }).map { $0.map { active[$0] } }
        } else {
            groups = [active]
        }

        var moves: [Move] = []
        for group in groups {
            moves += greedy(group, balances)
        }
        return moves
    }

    /// Divide i saldi nel maggior numero possibile di gruppi a somma zero.
    static func zeroSumGroups(_ values: [Int]) -> [[Int]] {
        let n = values.count
        let full = (1 << n) - 1
        var sum = [Int](repeating: 0, count: 1 << n)
        var dp = [Int](repeating: 0, count: 1 << n)
        if n > 0 {
            for mask in 1...full {
                let low = mask & -mask
                sum[mask] = sum[mask ^ low] + values[low.trailingZeroBitCount]
                var best = 0
                var bits = mask
                while bits != 0 {
                    let b = bits & -bits
                    bits ^= b
                    best = max(best, dp[mask ^ b])
                }
                dp[mask] = best + (sum[mask] == 0 ? 1 : 0)
            }
        }

        // Ricostruisce un ordine degli elementi: i prefissi a somma zero delimitano i gruppi.
        var order: [Int] = []
        var mask = full
        while mask != 0 {
            let target = dp[mask] - (sum[mask] == 0 ? 1 : 0)
            var bits = mask
            var chosen = mask.trailingZeroBitCount
            while bits != 0 {
                let b = bits & -bits
                bits ^= b
                if dp[mask ^ b] == target {
                    chosen = b.trailingZeroBitCount
                    break
                }
            }
            order.append(chosen)
            mask ^= 1 << chosen
        }
        order.reverse()

        var groups: [[Int]] = []
        var current: [Int] = []
        var running = 0
        for i in order {
            current.append(i)
            running += values[i]
            if running == 0 {
                groups.append(current)
                current = []
            }
        }
        if !current.isEmpty { groups.append(current) }
        return groups
    }

    /// Dentro un gruppo a somma zero: chi deve di più paga chi deve ricevere di più.
    /// Ogni passo azzera almeno una persona, quindi al massimo k-1 trasferimenti.
    static func greedy(_ group: [Int], _ balances: [Int]) -> [Move] {
        var left = Dictionary(uniqueKeysWithValues: group.map { ($0, balances[$0]) })
        var moves: [Move] = []
        while true {
            let debtors = left.filter { $0.value < 0 }.sorted { $0.value != $1.value ? $0.value < $1.value : $0.key < $1.key }
            let creditors = left.filter { $0.value > 0 }.sorted { $0.value != $1.value ? $0.value > $1.value : $0.key < $1.key }
            guard let d = debtors.first, let c = creditors.first else { break }
            let amount = min(-d.value, c.value)
            moves.append(Move(from: d.key, to: c.key, cents: amount))
            left[d.key]! += amount
            left[c.key]! -= amount
        }
        return moves
    }

    // MARK: Conto completo

    static func settle(people: [Person], owed: [UUID: Int], paid: [UUID: Int], total: Int) -> Settlement {
        let lines = people.map { Settlement.Line(person: $0, owed: owed[$0.id] ?? 0, paid: paid[$0.id] ?? 0) }
        let moves = minimalTransfers(lines.map(\.balance))
        let transfers = moves
            .map { Settlement.Transfer(from: people[$0.from], to: people[$0.to], cents: $0.cents) }
            .sorted { a, b in
                // Raggruppati per chi riceve (nell'ordine del tavolo), poi dal più grande.
                let ia = people.firstIndex(of: a.to) ?? 0, ib = people.firstIndex(of: b.to) ?? 0
                if ia != ib { return ia < ib }
                if a.cents != b.cents { return a.cents > b.cents }
                return (people.firstIndex(of: a.from) ?? 0) < (people.firstIndex(of: b.from) ?? 0)
            }
        return Settlement(total: total, lines: lines, transfers: transfers)
    }
}
