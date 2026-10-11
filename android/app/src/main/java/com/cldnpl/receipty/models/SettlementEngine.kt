package com.cldnpl.receipty.models

import java.util.UUID
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** La matematica del conto. Tutto in centesimi interi. */
object SettlementEngine {

    // Quote

    /**
     * Quanto ha consumato ciascuno: ogni voce si divide in parti uguali tra chi l'ha presa.
     * Le frazioni di centesimo si assegnano col metodo del resto più grande, così la somma
     * delle quote è esattamente il totale delle voci.
     */
    fun itemizedShares(items: List<BillItem>, people: List<UUID>, unit: Int = 1): Map<UUID, Int> {
        val exact = LinkedHashMap<UUID, Double>()
        people.forEach { exact[it] = 0.0 }
        var total = 0
        for (item in items) {
            val who = people.filter { item.assignees.contains(it) }
            if (who.isEmpty()) continue
            total += item.cents
            val each = item.cents.toDouble() / who.size
            for (id in who) exact[id] = (exact[id] ?: 0.0) + each
        }
        return roundPreservingSum(exact, people, total, emptyMap(), unit)
    }

    /**
     * Divisione in parti uguali. I centesimi che avanzano (es. €100 in 3) vanno a chi ha
     * pagato di più: così chi deve dare soldi paga cifre tonde.
     */
    fun equalShares(total: Int, people: List<UUID>, paid: Map<UUID, Int>, unit: Int = 1): Map<UUID, Int> {
        if (people.isEmpty()) return emptyMap()
        val each = total.toDouble() / people.size
        val exact = people.associateWith { each }
        return roundPreservingSum(exact, people, total, paid, unit)
    }

    /**
     * Lo sconto sul conto a voci: ogni quota cala in proporzione a quanto ha consumato,
     * e la somma delle quote resta esattamente il totale scontato.
     */
    fun applyDiscount(shares: Map<UUID, Int>, order: List<UUID>, subtotal: Int, discount: Int, unit: Int = 1): Map<UUID, Int> {
        val assigned = shares.values.sum()
        if (discount <= 0 || subtotal <= 0 || assigned <= 0) return shares
        val ratio = (subtotal - discount).toDouble() / subtotal
        // Con voci ancora da assegnare (mentre si assegnano) lo sconto tocca solo la parte assegnata.
        val target = if (assigned == subtotal) subtotal - discount
        else Math.round(assigned * ratio / unit).toInt() * unit
        val exact = shares.mapValues { it.value * ratio }
        return roundPreservingSum(exact, order, target, emptyMap(), unit)
    }

    /**
     * @param unit il passo in centesimi (1, oppure 100 per le valute senza decimali:
     *   1000 yen in 3 fanno 334 + 333 + 333, non 333,33).
     */
    fun roundPreservingSum(
        exact: Map<UUID, Double>,
        order: List<UUID>,
        total: Int,
        priority: Map<UUID, Int>,
        unit: Int = 1,
    ): Map<UUID, Int> {
        if (order.isEmpty()) return emptyMap()
        if (unit > 1 && total % unit == 0) {
            val scaled = exact.mapValues { it.value / unit }
            return roundPreservingSum(scaled, order, total / unit, priority).mapValues { it.value * unit }
        }
        val result = LinkedHashMap<UUID, Int>()
        var assigned = 0
        for (id in order) {
            val v = floor(exact[id] ?: 0.0).toInt()
            result[id] = v
            assigned += v
        }
        var remaining = total - assigned
        val ranked = order.withIndex().sortedWith { a, b ->
            val fa = (exact[a.value] ?: 0.0) - (result[a.value] ?: 0)
            val fb = (exact[b.value] ?: 0.0) - (result[b.value] ?: 0)
            if (abs(fa - fb) > 1e-9) return@sortedWith if (fa > fb) -1 else 1
            val pa = priority[a.value] ?: 0
            val pb = priority[b.value] ?: 0
            if (pa != pb) return@sortedWith pb.compareTo(pa)
            a.index.compareTo(b.index)
        }.map { it.value }
        var i = 0
        while (remaining > 0) {
            val id = ranked[i % ranked.size]
            result[id] = (result[id] ?: 0) + 1
            remaining -= 1
            i += 1
        }
        while (remaining < 0) {
            val id = ranked[ranked.size - 1 - (i % ranked.size)]
            result[id] = (result[id] ?: 0) - 1
            remaining += 1
            i += 1
        }
        return result
    }

    // Trasferimenti

    data class Move(val from: Int, val to: Int, val cents: Int)

    /**
     * Il numero minimo di trasferimenti che azzera tutti i saldi.
     *
     * Con n persone non in pari servono al massimo n-1 trasferimenti; se però il gruppo si
     * divide in k sottogruppi che si compensano da soli (es. A deve 10 a B e C deve 5 a D)
     * ne bastano n-k. Trovare la divisione con più sottogruppi è una programmazione dinamica
     * sugli insiemi, fattibile fino a una ventina di persone: oltre si usa il solo metodo greedy.
     * @param balances pagato meno dovuto, per persona. La somma deve essere zero.
     */
    fun minimalTransfers(balances: List<Int>): List<Move> {
        val active = balances.indices.filter { balances[it] != 0 }
        if (active.isEmpty()) return emptyList()

        val groups: List<List<Int>> = if (active.size <= 18) {
            zeroSumGroups(active.map { balances[it] }).map { g -> g.map { active[it] } }
        } else {
            listOf(active)
        }

        val moves = mutableListOf<Move>()
        for (group in groups) moves += greedy(group, balances)
        return moves
    }

    /** Divide i saldi nel maggior numero possibile di gruppi a somma zero. */
    fun zeroSumGroups(values: List<Int>): List<List<Int>> {
        val n = values.size
        val full = (1 shl n) - 1
        val sum = IntArray(1 shl n)
        val dp = IntArray(1 shl n)
        if (n > 0) {
            for (mask in 1..full) {
                val low = mask and -mask
                sum[mask] = sum[mask xor low] + values[Integer.numberOfTrailingZeros(low)]
                var best = 0
                var bits = mask
                while (bits != 0) {
                    val b = bits and -bits
                    bits = bits xor b
                    best = max(best, dp[mask xor b])
                }
                dp[mask] = best + (if (sum[mask] == 0) 1 else 0)
            }
        }

        // Ricostruisce un ordine degli elementi: i prefissi a somma zero delimitano i gruppi.
        val order = mutableListOf<Int>()
        var mask = full
        while (mask != 0) {
            val target = dp[mask] - (if (sum[mask] == 0) 1 else 0)
            var bits = mask
            var chosen = Integer.numberOfTrailingZeros(mask)
            while (bits != 0) {
                val b = bits and -bits
                bits = bits xor b
                if (dp[mask xor b] == target) {
                    chosen = Integer.numberOfTrailingZeros(b)
                    break
                }
            }
            order.add(chosen)
            mask = mask xor (1 shl chosen)
        }
        order.reverse()

        val groups = mutableListOf<List<Int>>()
        var current = mutableListOf<Int>()
        var running = 0
        for (i in order) {
            current.add(i)
            running += values[i]
            if (running == 0) {
                groups.add(current)
                current = mutableListOf()
            }
        }
        if (current.isNotEmpty()) groups.add(current)
        return groups
    }

    /**
     * Dentro un gruppo a somma zero: chi deve di più paga chi deve ricevere di più.
     * Ogni passo azzera almeno una persona, quindi al massimo k-1 trasferimenti.
     */
    fun greedy(group: List<Int>, balances: List<Int>): List<Move> {
        val left = LinkedHashMap<Int, Int>()
        group.forEach { left[it] = balances[it] }
        val moves = mutableListOf<Move>()
        while (true) {
            val d = left.entries.filter { it.value < 0 }
                .sortedWith(compareBy<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
                .firstOrNull() ?: break
            val c = left.entries.filter { it.value > 0 }
                .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
                .firstOrNull() ?: break
            val (dKey, dValue) = d.key to d.value
            val (cKey, cValue) = c.key to c.value
            val amount = min(-dValue, cValue)
            moves.add(Move(dKey, cKey, amount))
            left[dKey] = dValue + amount
            left[cKey] = cValue - amount
        }
        return moves
    }

    // Resto

    /**
     * Chi si tiene il resto che torna dal ristorante.
     *
     * Il resto va solo a chi ha messo più della sua parte, e mai oltre quanto gli spetta.
     * Tra tutti i modi di spartirlo si sceglie quello che lascia meno trasferimenti tra
     * persone; a parità, quello con meno passaggi di contanti. Esempio: conto da 52 in 4,
     * Claudia mette 20 e Marco 40, tornano 8: Claudia si tiene 7 (è pari), Marco 1, e
     * restano solo Olga → Marco 13 e Kekko → Marco 13.
     * @param balances pagato meno dovuto; la somma è il resto.
     */
    fun splitChange(change: Int, balances: List<Int>): List<Int> {
        val kept = IntArray(balances.size)
        if (change <= 0) return kept.toList()
        val creditors = balances.indices.filter { balances[it] > 0 }.sortedByDescending { balances[it] }

        // Prima soluzione, sempre valida: si riempie chi deve ricevere di più, poi il successivo.
        var left = change
        for (i in creditors) {
            if (left <= 0) break
            val take = min(left, balances[i])
            kept[i] = take
            left -= take
        }

        // Ricerca esaustiva finché il numero di combinazioni resta piccolo (i casi reali).
        val active = balances.count { it != 0 }
        val candidates = (1L shl creditors.size) * max(1, creditors.size)
        if (creditors.size > 10 || active > 14 || candidates * (1L shl active) > 30_000_000L) return kept.toList()

        fun cost(k: List<Int>): Pair<Int, Int> {
            val net = balances.indices.map { balances[it] - k[it] }
            return transferCount(net) to k.count { it > 0 }
        }

        fun less(a: Pair<Int, Int>, b: Pair<Int, Int>) =
            a.first < b.first || (a.first == b.first && a.second < b.second)

        var best = kept.toList()
        var bestCost = cost(best)
        for (mask in 0 until (1 shl creditors.size)) {
            val full = creditors.indices.filter { mask and (1 shl it) != 0 }.map { creditors[it] }
            val covered = full.sumOf { balances[it] }
            if (covered > change) continue
            val base = IntArray(balances.size)
            for (i in full) base[i] = balances[i]
            val rest = change - covered
            val options = mutableListOf<List<Int>>()
            if (rest == 0) {
                options.add(base.toList())
            } else {
                for (j in creditors) {
                    if (!full.contains(j) && balances[j] >= rest) {
                        val k = base.copyOf()
                        k[j] = rest
                        options.add(k.toList())
                    }
                }
            }
            for (k in options) {
                val c = cost(k)
                if (less(c, bestCost)) {
                    best = k
                    bestCost = c
                }
            }
        }
        return best
    }

    fun transferCount(balances: List<Int>): Int {
        val active = balances.filter { it != 0 }
        if (active.isEmpty()) return 0
        return active.size - zeroSumGroups(active).size
    }

    // Conto completo

    fun settle(people: List<Person>, owed: Map<UUID, Int>, paid: Map<UUID, Int>, total: Int): Settlement {
        var lines = people.map { Settlement.Line(it, owed[it.id] ?: 0, paid[it.id] ?: 0) }
        val change = max(0, lines.sumOf { it.paid } - total)
        val kept = splitChange(change, lines.map { it.paid - it.owed })
        lines = lines.mapIndexed { i, line -> line.copy(changeKept = kept[i]) }
        val changeSplit = lines.filter { it.changeKept > 0 }
            .sortedByDescending { it.changeKept }
            .map { Settlement.ChangeShare(it.person, it.changeKept) }

        fun index(p: Person) = people.indexOfFirst { it.id == p.id }.coerceAtLeast(0)

        val moves = minimalTransfers(lines.map { it.balance })
        val transfers = moves
            .map { Settlement.Transfer(people[it.from], people[it.to], it.cents) }
            .sortedWith { a, b ->
                // Raggruppati per chi riceve (nell'ordine del tavolo), poi dal più grande.
                val ia = index(a.to)
                val ib = index(b.to)
                when {
                    ia != ib -> ia.compareTo(ib)
                    a.cents != b.cents -> b.cents.compareTo(a.cents)
                    else -> index(a.from).compareTo(index(b.from))
                }
            }
        return Settlement(total = total, lines = lines, transfers = transfers, changeSplit = changeSplit)
    }
}
