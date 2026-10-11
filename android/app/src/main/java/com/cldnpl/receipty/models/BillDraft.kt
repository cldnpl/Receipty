package com.cldnpl.receipty.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/** Il conto che si sta dividendo, dalla prima schermata al risultato. */
class BillDraft(
    source: BillSource,
    /** Presa dalle impostazioni quando il conto nasce: cambiarla dopo non tocca questo conto. */
    val currency: Currency = Currency.current,
) {
    val id: UUID = UUID.randomUUID()
    val createdAt: Long = System.currentTimeMillis()
    var source by mutableStateOf(source)

    val items = mutableStateListOf<BillItem>()

    /** Il totale stampato sullo scontrino, se l'OCR l'ha trovato: serve solo a verificare le voci. */
    var printedTotal by mutableStateOf<Int?>(null)

    val people = mutableStateListOf<Person>()
    val paidText = mutableStateMapOf<UUID, String>()

    /** Solo per la divisione in parti uguali. */
    var equalTotalText by mutableStateOf("")

    /** Lo sconto sul totale, se c'è: si aggiunge in "Chi ha pagato?". */
    var hasDiscount by mutableStateOf(false)
    var discountMode by mutableStateOf(DiscountMode.PERCENT)
    var discountText by mutableStateOf("")

    // Totali

    val itemsTotal: Int get() = items.sumOf { it.cents }

    /** Il totale prima dello sconto: le voci, o quello scritto a mano. */
    val subtotal: Int
        get() = if (source.isItemized) itemsTotal else max(0, Money.parse(equalTotalText) ?: 0)

    /** Lo sconto in centesimi: arrotondato al passo della valuta, mai più del totale. */
    val discount: Int
        get() {
            if (!hasDiscount) return 0
            val value = Money.parse(discountText)?.takeIf { it > 0 } ?: return 0
            // "10" in percentuale arriva come 1000: centesimi di punto, 10000 = 100%.
            val raw = if (discountMode == DiscountMode.PERCENT) subtotal.toDouble() * min(value, 10_000) / 10_000
            else value.toDouble()
            val unit = currency.unit
            return min(subtotal, Math.round(raw / unit).toInt() * unit)
        }

    /** Quello che il tavolo paga davvero: il totale meno lo sconto. */
    val total: Int get() = subtotal - discount

    val itemsToCheck: Int get() = items.count { it.issue != null }
    val unassignedCount: Int
        get() = items.count { item -> people.none { item.assignees.contains(it.id) } }

    fun paid(person: Person): Int = max(0, Money.parse(paidText[person.id] ?: "") ?: 0)

    val paidTotal: Int get() = people.sumOf { paid(it) }

    val owed: Map<UUID, Int>
        get() {
            val ids = people.map { it.id }
            if (source.isItemized) {
                val shares = SettlementEngine.itemizedShares(items.toList(), ids, currency.unit)
                return SettlementEngine.applyDiscount(shares, ids, subtotal, discount, currency.unit)
            }
            val paid = people.associate { it.id to paid(it) }
            return SettlementEngine.equalShares(total, ids, paid, currency.unit)
        }

    /** Si può calcolare quando sul piatto c'è almeno il totale: quello che avanza è il resto. */
    val canSettle: Boolean get() = total > 0 && paidTotal >= total
    val change: Int get() = max(0, paidTotal - total)

    fun settlement(): Settlement {
        val paid = people.associate { it.id to paid(it) }
        return SettlementEngine.settle(people.toList(), owed, paid, total).copy(currency = currency, discount = discount)
    }

    // Persone

    /** Aggiunge qualcuno al tavolo. Due persone con lo stesso nome diventano "Marco" e "Marco 2". */
    fun addPerson(named: String): Person? {
        val name = named.trim()
        if (name.isEmpty()) return null
        var unique = name
        var n = 2
        while (people.any { it.name.equals(unique, ignoreCase = true) }) {
            unique = "$name $n"
            n += 1
        }
        val person = Person(name = unique)
        people.add(person)
        return person
    }

    fun remove(person: Person) {
        people.removeAll { it.id == person.id }
        paidText.remove(person.id)
        for (i in items.indices) items[i] = items[i].copy(assignees = items[i].assignees - person.id)
    }

    // Chi ha preso cosa

    fun toggle(person: Person, itemID: UUID) {
        val i = items.indexOfFirst { it.id == itemID }
        if (i < 0) return
        val item = items[i]
        items[i] = item.copy(
            assignees = if (item.assignees.contains(person.id)) item.assignees - person.id else item.assignees + person.id
        )
    }

    fun toggleEveryone(itemID: UUID) {
        val i = items.indexOfFirst { it.id == itemID }
        if (i < 0) return
        val everyone = people.map { it.id }.toSet()
        val item = items[i]
        items[i] = item.copy(assignees = if (item.assignees.containsAll(everyone)) emptySet() else everyone)
    }

    /** "Tutto": questa persona ha pagato l'intero conto, gli altri niente. */
    fun paidEverything(person: Person) {
        for (p in people) paidText[p.id] = ""
        paidText[person.id] = Money.editable(total)
    }
}
