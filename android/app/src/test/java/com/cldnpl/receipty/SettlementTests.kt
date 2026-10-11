package com.cldnpl.receipty

import com.cldnpl.receipty.localization.AppLanguage
import com.cldnpl.receipty.localization.I18n
import com.cldnpl.receipty.models.BillDraft
import com.cldnpl.receipty.models.BillItem
import com.cldnpl.receipty.models.BillSource
import com.cldnpl.receipty.models.Currency
import com.cldnpl.receipty.models.DiscountMode
import com.cldnpl.receipty.models.Money
import com.cldnpl.receipty.models.Person
import com.cldnpl.receipty.models.Settlement
import com.cldnpl.receipty.models.SettlementEngine
import com.cldnpl.receipty.models.format
import com.cldnpl.receipty.models.formatCompact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import kotlin.random.Random

class SettlementTests {

    private fun people(vararg names: String) = names.map { Person(name = it) }

    /** L'esempio del brief: 7 amici, €70, Cla ha messo 50 e Marco 20. */
    @Test
    fun equalSplitSevenFriends() {
        val p = people("Cla", "Marco", "Anna", "Luca", "Sara", "Giulia", "Paolo")
        val draft = BillDraft(BillSource.equal, Currency("EUR"))
        draft.people.addAll(p)
        draft.equalTotalText = "70"
        draft.paidText[p[0].id] = "50"
        draft.paidText[p[1].id] = "20"

        val s = draft.settlement()
        assertEquals(5, s.transfers.size)
        assertTrue(s.transfers.all { it.cents == 1000 })
        assertEquals(4, s.transfers.count { it.to.name == "Cla" })
        assertEquals(1, s.transfers.count { it.to.name == "Marco" })
        assertEquals(7000, s.lines.sumOf { it.owed })
    }

    /** Il conto del mockup: scontrino da €40, Cla paga 30 e Marco 10. */
    @Test
    fun itemizedMockupBill() {
        val p = people("Cla", "Marco", "Giulia", "Luca")
        val (cla, marco, giulia, luca) = p.map { it.id }
        val draft = BillDraft(BillSource.scan, Currency("EUR"))
        draft.people.addAll(p)
        draft.items.addAll(
            listOf(
                BillItem(name = "Pizza margherita", cents = 800, assignees = setOf(cla)),
                BillItem(name = "Carbonara", cents = 1200, assignees = setOf(marco)),
                BillItem(name = "Birra media", cents = 500, assignees = setOf(cla, giulia)),
                BillItem(name = "Acqua naturale", cents = 200, assignees = setOf(luca)),
                BillItem(name = "Tiramisù", cents = 600, assignees = setOf(giulia)),
                BillItem(name = "Coperto", cents = 700, assignees = setOf(cla, marco, giulia, luca)),
            )
        )
        draft.paidText[cla] = "30"
        draft.paidText[marco] = "10"

        val owed = draft.owed
        assertEquals(1225, owed[cla])
        assertEquals(1375, owed[marco])
        assertEquals(1025, owed[giulia])
        assertEquals(375, owed[luca])

        val s = draft.settlement()
        assertEquals(
            listOf("Giulia>Cla:1025", "Marco>Cla:375", "Luca>Cla:375"),
            s.transfers.map { "${it.from.name}>${it.to.name}:${it.cents}" },
        )
    }

    /** Due coppie indipendenti: il greedy puro farebbe 3 trasferimenti, il minimo è 2. */
    @Test
    fun findsIndependentGroups() {
        val moves = SettlementEngine.minimalTransfers(listOf(500, -700, 700, -500))
        assertEquals(2, moves.size)
        assertTrue(moves.contains(SettlementEngine.Move(3, 0, 500)))
        assertTrue(moves.contains(SettlementEngine.Move(1, 2, 700)))
    }

    @Test
    fun alwaysSettlesEveryone() {
        val rng = Random(42)
        repeat(300) {
            val n = rng.nextInt(2, 10)
            val balances = MutableList(n - 1) { rng.nextInt(-5000, 5001) }
            balances.add(-balances.sum())
            val moves = SettlementEngine.minimalTransfers(balances)
            val left = balances.toMutableList()
            for (m in moves) {
                assertTrue(m.cents > 0)
                left[m.from] += m.cents
                left[m.to] -= m.cents
            }
            assertEquals(List(n) { 0 }, left)
            assertTrue(moves.size <= maxOf(0, balances.count { it != 0 } - 1))
        }
    }

    @Test
    fun nobodyOwesAnything() {
        assertTrue(SettlementEngine.minimalTransfers(listOf(0, 0, 0)).isEmpty())
    }

    /** €100 in 3: 33,34 + 33,33 + 33,33, e il centesimo in più tocca a chi ha pagato. */
    @Test
    fun oddCentsGoToWhoPaid() {
        val p = people("Anna", "Bea", "Cri")
        val shares = SettlementEngine.equalShares(10000, p.map { it.id }, mapOf(p[2].id to 10000))
        assertEquals(10000, shares.values.sum())
        assertEquals(3334, shares[p[2].id])
        assertEquals(3333, shares[p[0].id])
    }

    @Test
    fun sharedItemSplitsExactly() {
        val p = people("A", "B", "C")
        val item = BillItem(name = "Vino", cents = 1000, assignees = p.map { it.id }.toSet())
        val shares = SettlementEngine.itemizedShares(listOf(item), p.map { it.id })
        assertEquals(1000, shares.values.sum())
        assertEquals(setOf(333, 334), shares.values.toSet())
    }

    /**
     * Gli importi si scrivono come vuole la lingua dell'app ("22,30 €" in italiano):
     * qui la si fissa sull'inglese, altrimenti il test dipenderebbe dal computer che lo esegue.
     */
    @Test
    fun moneyFormatting() {
        val language = I18n.language
        I18n.setForTesting(AppLanguage.ENGLISH)
        try {
            val eur = Currency("EUR")
            assertEquals("€22.30", Money.format(2230, eur))
            assertEquals("€1,234.50", Money.format(123450, eur))
            assertEquals("€0.05", Money.format(5, eur))
            assertEquals("−€22.30", Money.format(-2230, eur))
            assertEquals("¥1,200", Money.format(120000, Currency("JPY")))
            assertEquals("$9.99", Money.format(999, Currency("USD")))
            assertEquals("£15.00", Money.format(1500, Currency("GBP")))
            assertEquals("€0", Money.formatCompact(0, eur))
            assertEquals(123450, Money.parse("1,234.50"))
            assertEquals(1200, Money.parse("12"))
            assertEquals(1250, Money.parse("12,5"))
            assertEquals(1250, Money.parse("12.50"))
            assertEquals(123450, Money.parse("€ 1.234,50"))
            assertNull(Money.parse(""))
            assertEquals("12.34", Money.sanitizeInput("12,345"))
            assertEquals("0.5", Money.sanitizeInput(",5"))
            assertEquals("12", Money.editable(1200))
            assertEquals("12.50", Money.editable(1250))
        } finally {
            I18n.setForTesting(language)
        }
    }

    /** Valute senza decimali: ¥1000 in 3 fa 334 + 333 + 333 yen, mai frazioni di yen. */
    @Test
    fun zeroDecimalCurrencySplitsInWholeUnits() {
        val draft = BillDraft(BillSource.equal, Currency("JPY"))
        draft.equalTotalText = "1000"
        val p = listOf("Aki", "Ben", "Chie").map { draft.addPerson(it)!! }
        draft.paidText[p[0].id] = "1000"
        val owed = draft.owed
        assertEquals(100_000, owed.values.sum())
        assertTrue(owed.values.all { it % 100 == 0 })
        assertEquals(33_400, owed[p[0].id])
        val s = draft.settlement()
        assertEquals("JPY", s.currency.code)
        assertEquals(listOf(33_300, 33_300), s.transfers.map { it.cents })
    }

    @Test
    fun currencyListIsUsable() {
        assertTrue(Currency.all.size > 50)
        assertTrue(Currency.all.all { it.minorDigits == 0 || it.minorDigits == 2 })
        assertEquals(Currency.all.size, Currency.all.map { it.code }.toSet().size)
    }

    // Sconto

    /** €80 in 4 con il 10% di sconto: si pagano €72, €18 a testa. */
    @Test
    fun percentDiscountOnEqualSplit() {
        val draft = BillDraft(BillSource.equal, Currency("EUR"))
        draft.equalTotalText = "80"
        listOf("Anna", "Bea", "Cri", "Dani").forEach { draft.addPerson(it) }
        draft.hasDiscount = true
        draft.discountText = "10"
        assertEquals(8000, draft.subtotal)
        assertEquals(800, draft.discount)
        assertEquals(7200, draft.total)
        assertTrue(draft.owed.values.all { it == 1800 })
        draft.paidText[draft.people[0].id] = "72"
        val s = draft.settlement()
        assertEquals(7200, s.total)
        assertEquals(800, s.discount)
        assertEquals(0, s.change)
        assertEquals(listOf(1800, 1800, 1800), s.transfers.map { it.cents })
    }

    /** Lo scontrino del mockup (€40) con €4 di sconto: ogni quota cala del 10%, la somma fa €36. */
    @Test
    fun amountDiscountOnItemizedBill() {
        val p = people("Cla", "Marco", "Giulia", "Luca")
        val (cla, marco, giulia, luca) = p.map { it.id }
        val draft = BillDraft(BillSource.scan, Currency("EUR"))
        draft.people.addAll(p)
        draft.items.addAll(
            listOf(
                BillItem(name = "Pizza margherita", cents = 800, assignees = setOf(cla)),
                BillItem(name = "Carbonara", cents = 1200, assignees = setOf(marco)),
                BillItem(name = "Birra media", cents = 500, assignees = setOf(cla, giulia)),
                BillItem(name = "Acqua naturale", cents = 200, assignees = setOf(luca)),
                BillItem(name = "Tiramisù", cents = 600, assignees = setOf(giulia)),
                BillItem(name = "Coperto", cents = 700, assignees = setOf(cla, marco, giulia, luca)),
            )
        )
        draft.hasDiscount = true
        draft.discountMode = DiscountMode.AMOUNT
        draft.discountText = "4"
        val owed = draft.owed
        assertEquals(3600, owed.values.sum())
        assertEquals(1238, owed[marco])
        assertTrue(owed[cla] in listOf(1102, 1103))
        assertTrue(owed[giulia] in listOf(922, 923))
        assertTrue(owed[luca] in listOf(337, 338))
        draft.paidText[cla] = "36"
        val s = draft.settlement()
        assertEquals(3600, s.total)
        assertEquals(400, s.discount)
        assertEquals(3600 - s.lines[0].owed, s.transfers.sumOf { it.cents })
    }

    /** Lo sconto non va mai oltre il totale, né in soldi né in percentuale. */
    @Test
    fun discountNeverExceedsTotal() {
        val draft = BillDraft(BillSource.equal, Currency("EUR"))
        draft.equalTotalText = "52"
        draft.hasDiscount = true
        draft.discountMode = DiscountMode.AMOUNT
        draft.discountText = "100"
        assertEquals(0, draft.total)
        draft.discountMode = DiscountMode.PERCENT
        draft.discountText = "150"
        assertEquals(0, draft.total)
        draft.hasDiscount = false
        assertEquals(5200, draft.total)
    }

    /** Con lo yen lo sconto si arrotonda a yen interi: ¥999 al 15% fa ¥150 di sconto. */
    @Test
    fun discountRoundsToWholeYen() {
        val draft = BillDraft(BillSource.equal, Currency("JPY"))
        draft.equalTotalText = "999"
        draft.hasDiscount = true
        draft.discountText = "15"
        assertEquals(15_000, draft.discount)
        assertEquals(84_900, draft.total)
    }

    // Resto

    private fun equalBill(total: String, payers: List<Pair<String, String>>, others: List<String>): Settlement {
        val draft = BillDraft(BillSource.equal, Currency("EUR"))
        draft.equalTotalText = total
        for ((name, amount) in payers) {
            val p = draft.addPerson(name)!!
            draft.paidText[p.id] = amount
        }
        others.forEach { draft.addPerson(it) }
        return draft.settlement()
    }

    /** €52 in 4, Claudia mette 20 e Marco 40: tornano €8. */
    @Test
    fun changeGoesToWhoOverpaid() {
        val s = equalBill("52", listOf("Claudia" to "20", "Marco" to "40"), listOf("Olga", "Kekko"))
        assertEquals(800, s.change)
        assertEquals(listOf("Claudia:700", "Marco:100"), s.changeSplit.map { "${it.person.name}:${it.cents}" })
        assertEquals(listOf("Olga>Marco:1300", "Kekko>Marco:1300"), s.transfers.map { "${it.from.name}>${it.to.name}:${it.cents}" })
        assertTrue(s.lines.all { it.owed == 1300 })
    }

    /** Il resto copre chi ha messo poco in più, così restano meno trasferimenti. */
    @Test
    fun changeMinimisesTransfers() {
        val s = equalBill("52", listOf("Claudia" to "50", "Marco" to "20"), listOf("Olga", "Kekko"))
        assertEquals(1800, s.change)
        assertEquals(setOf("Marco:700", "Claudia:1100"), s.changeSplit.map { "${it.person.name}:${it.cents}" }.toSet())
        assertEquals(2, s.transfers.size)
        assertTrue(s.transfers.all { it.to.name == "Claudia" && it.cents == 1300 })
    }

    /** Una banconota da 100 per un conto da 52: il resto torna tutto a chi l'ha messa. */
    @Test
    fun singlePayerKeepsAllTheChange() {
        val s = equalBill("52", listOf("Claudia" to "100"), listOf("Marco", "Olga", "Kekko"))
        assertEquals(listOf(4800), s.changeSplit.map { it.cents })
        assertEquals("Claudia", s.changeSplit.first().person.name)
        assertEquals(3, s.transfers.size)
    }

    @Test
    fun changeAlwaysBalances() {
        val rng = Random(7)
        repeat(200) {
            val n = rng.nextInt(2, 8)
            val total = rng.nextInt(1000, 20001)
            val people = (0 until n).map { Person(name = "P$it") }
            val owed = SettlementEngine.equalShares(total, people.map { it.id }, emptyMap())
            val paid = HashMap<UUID, Int>()
            var put = 0
            for (p in people.shuffled(rng).take(rng.nextInt(1, n + 1))) {
                val v = rng.nextInt(0, total + 1)
                paid[p.id] = v
                put += v
            }
            if (put < total) paid[people[0].id] = (paid[people[0].id] ?: 0) + total - put + rng.nextInt(0, 3001)
            val s = SettlementEngine.settle(people, owed, paid, total)
            assertEquals(paid.values.sum() - total, s.change)
            val left = s.lines.associate { it.person.id to it.balance }.toMutableMap()
            for (t in s.transfers) {
                left[t.from.id] = left.getValue(t.from.id) + t.cents
                left[t.to.id] = left.getValue(t.to.id) - t.cents
            }
            assertTrue(left.values.all { it == 0 })
            assertTrue(s.lines.all { it.changeKept <= maxOf(0, it.paid - it.owed) })
        }
    }
}
