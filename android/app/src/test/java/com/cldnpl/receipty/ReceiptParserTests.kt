package com.cldnpl.receipty

import com.cldnpl.receipty.models.BillItem
import com.cldnpl.receipty.scanner.Box
import com.cldnpl.receipty.scanner.OCRFragment
import com.cldnpl.receipty.scanner.ReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.tan

class ReceiptParserTests {

    /** Righe già separate: nome a sinistra e prezzo a destra, come le restituisce l'OCR. */
    private fun lines(rows: List<Pair<String, String?>>, confidence: Float = 1f): List<OCRFragment> =
        rows.flatMapIndexed { i, row ->
            val y = i * 40f
            val out = mutableListOf(OCRFragment(row.first, confidence, box = Box(20f, y, 200f, 24f)))
            row.second?.let { out.add(OCRFragment(it, confidence, box = Box(400f, y + 2, 70f, 24f))) }
            out
        }

    @Test
    fun mockupReceipt() {
        val parsed = ReceiptParser.parse(
            lines(
                listOf(
                    "TRATTORIA DA NINO" to null,
                    "Via Roma 12 - Tel. 06 1234567" to null,
                    "MARGHERITA" to "8,00",
                    "CARBONARA" to "12,00",
                    "BIRRA MEDIA" to "5,00",
                    "ACQUA NAT." to "2,00",
                    "TIRAMIS?" to "6,00",
                    "COPERTO" to "7,O0",
                    "TOTALE" to "40,00",
                    "CONTANTI" to "50,00",
                    "RESTO" to "10,00",
                )
            )
        )
        assertEquals(listOf("Margherita", "Carbonara", "Birra media", "Acqua nat.", "Tiramis?", "Coperto"), parsed.items.map { it.name })
        assertEquals(listOf(800, 1200, 500, 200, 600, 700), parsed.items.map { it.cents })
        assertEquals(listOf(null, null, null, null, BillItem.Issue.NAME, BillItem.Issue.PRICE), parsed.items.map { it.issue })
        assertEquals(4000, parsed.total)
    }

    @Test
    fun skewedReceiptStillGroupsLines() {
        // Scontrino inclinato di ~4°: il prezzo sta più in basso del nome della stessa riga.
        val angle = 0.07f
        val frags = mutableListOf<OCRFragment>()
        listOf("PASTA" to "9,50", "VINO" to "14,00", "CAFFE" to "1,50").forEachIndexed { i, row ->
            val y = i * 36f
            frags.add(OCRFragment(row.first, 1f, box = Box(20f, y + 20 * tan(angle), 120f, 22f), angle = angle))
            frags.add(OCRFragment(row.second, 1f, box = Box(500f, y + 520 * tan(angle), 60f, 22f), angle = angle))
        }
        val parsed = ReceiptParser.parse(frags)
        assertEquals(listOf("Pasta", "Vino", "Caffe"), parsed.items.map { it.name })
        assertEquals(listOf(950, 1400, 150), parsed.items.map { it.cents })
    }

    @Test
    fun quantities() {
        val parsed = ReceiptParser.parse(
            lines(
                listOf(
                    "2 x 2,50" to null,
                    "BIRRA" to "5,00",
                    "2x Caffè" to "3,00",
                    "ACQUA 3 x 1,50" to "4,50",
                    "4 FORMAGGI" to "9,00",
                    "3 x 2,00" to null,
                    "SPRITZ" to "7,00",
                )
            )
        )
        assertEquals(listOf("Birra", "Caffè", "Acqua", "4 formaggi", "Spritz"), parsed.items.map { it.name })
        assertEquals(listOf(2, 2, 3, 1, 3), parsed.items.map { it.quantity })
        // 3 × 2,00 non fa 7,00: il prezzo va confermato.
        assertEquals(BillItem.Issue.PRICE, parsed.items.last().issue)
        assertNull(parsed.items.first().issue)
    }

    @Test
    fun ignoresVatDatesAndPayments() {
        val parsed = ReceiptParser.parse(
            lines(
                listOf(
                    "DOCUMENTO COMMERCIALE" to null,
                    "28/09/2026 20:31" to null,
                    "PIZZA DIAVOLA 10%" to "9,00 B",
                    "Fiore di latte" to "3,00",
                    "Cassata" to "5,50",
                    "IVA 10%" to "1,59",
                    "TOTALE COMPLESSIVO" to "17,50",
                    "PAGAMENTO ELETTRONICO" to "17,50",
                )
            )
        )
        assertEquals(listOf("Pizza diavola", "Fiore di latte", "Cassata"), parsed.items.map { it.name })
        assertEquals(1750, parsed.total)
    }

    @Test
    fun wrappedNameAndSplitPrice() {
        val parsed = ReceiptParser.parse(
            lines(
                listOf(
                    "TAGLIATA DI MANZO CON" to null,
                    "RUCOLA E GRANA" to "18,00",
                    "Tagliere misto" to "€ 14,00",
                    "Dolce" to "5 ,00",
                )
            )
        )
        assertEquals(listOf(1800, 1400, 500), parsed.items.map { it.cents })
        assertEquals("Tagliere misto", parsed.items[1].name)
    }

    @Test
    fun englishReceipt() {
        val parsed = ReceiptParser.parse(
            lines(
                listOf(
                    "THE RED LION" to null,
                    "Table 12   Guests 4" to null,
                    "Fish & Chips" to "14.50",
                    "2 x Pint Lager" to "11.00",
                    "Sticky toffee pudding" to "6.75",
                    "Service charge 12.5%" to "4.03",
                    "SUBTOTAL" to "36.28",
                    "VAT 20%" to "6.05",
                    "TOTAL" to "36.28",
                    "CASH" to "40.00",
                    "CHANGE" to "3.72",
                )
            )
        )
        assertEquals(listOf("Fish & Chips", "Pint Lager", "Sticky toffee pudding", "Service charge"), parsed.items.map { it.name })
        assertEquals(listOf(1450, 1100, 675, 403), parsed.items.map { it.cents })
        assertEquals(2, parsed.items[1].quantity)
        assertEquals(3628, parsed.total)
    }

    @Test
    fun shopNameIsNotAnItem() {
        // Insegna e ragione sociale sopra le voci, con il primo prezzo andato a capo sotto il nome del locale.
        val parsed = ReceiptParser.parse(
            lines(
                listOf(
                    "OSTERIA AL PONTE" to null,
                    "" to "4,00",
                    "di Rossi Mario & C. s.n.c." to null,
                    "" to "3,00",
                    "PIZZERIA BELLA NAPOLI SRL" to "2,00",
                    "Cap. Soc. 10.000,00" to null,
                    "Corso Umberto 12 Napoli" to "1,00",
                    "Margherita" to "7,00",
                    "Birra" to "4,00",
                    "TOTALE" to "11,00",
                )
            )
        )
        assertEquals(listOf("Margherita", "Birra"), parsed.items.map { it.name })
    }

    @Test
    fun firstLineIsTheShopName() {
        // Nessuna parola che dica "locale": è comunque la prima riga, e lì c'è l'insegna.
        val parsed = ReceiptParser.parse(
            lines(
                listOf(
                    "LA LANTERNA" to null,
                    "" to "8,00",
                    "MARGHERITA" to null,
                    "" to "9,50",
                    "TOTALE" to "17,50",
                )
            )
        )
        assertEquals(listOf("Margherita"), parsed.items.map { it.name })
    }

    @Test
    fun largeOrCenteredHeaderIsNotAnItem() {
        // L'insegna a doppia altezza e lo slogan centrato, finiti sulla riga di un prezzo.
        val frags = mutableListOf(
            OCRFragment("JOE'S DINER", 1f, box = Box(150f, 0f, 220f, 56f)),
            OCRFragment("5.00", 1f, box = Box(400f, 14f, 70f, 24f)),
            OCRFragment("Best burgers in town", 1f, box = Box(130f, 70f, 260f, 24f)),
            OCRFragment("3.00", 1f, box = Box(400f, 72f, 70f, 24f)),
        )
        frags += lines(listOf("Cheeseburger" to "12.50", "Fries" to "4.00", "Protein bar" to "2.50"))
            .map { it.copy(box = it.box.copy(top = it.box.top + 120f)) }
        val parsed = ReceiptParser.parse(frags)
        assertEquals(listOf("Cheeseburger", "Fries", "Protein bar"), parsed.items.map { it.name })
    }

    @Test
    fun productNamedLikeAShopIsKept() {
        // "Bar" basta a riconoscere l'insegna solo senza prezzo: con il prezzo è un prodotto.
        val parsed = ReceiptParser.parse(lines(listOf("Protein bar" to "2,50", "Caffè" to "1,20")))
        assertEquals(listOf("Protein bar", "Caffè"), parsed.items.map { it.name })
    }

    @Test
    fun otherCurrencies() {
        val parsed = ReceiptParser.parse(
            lines(
                listOf(
                    "Burger" to "$12.50",
                    "Fries" to "4.00 USD",
                    "Fondue" to "CHF38.00",
                    "Laksa" to "S$9.80",
                    "Pint" to "£6.20",
                )
            )
        )
        assertEquals(listOf(1250, 400, 3800, 980, 620), parsed.items.map { it.cents })
        assertTrue(parsed.items.all { it.issue == null })
    }

    @Test
    fun yenReceiptWithoutDecimals() {
        val parsed = ReceiptParser.parse(
            lines(
                listOf(
                    "Table 12" to null,
                    "Ramen" to "¥1,200",
                    "Gyoza" to "¥480",
                    "Beer" to "650",
                    "TOTAL" to "¥2,330",
                )
            ),
            minorDigits = 0,
        )
        assertEquals(listOf("Ramen", "Gyoza", "Beer"), parsed.items.map { it.name })
        assertEquals(listOf(120_000, 48_000, 65_000), parsed.items.map { it.cents })
        assertEquals(233_000, parsed.total)
    }

    @Test
    fun lowConfidenceFlagsItems() {
        val parsed = ReceiptParser.parse(lines(listOf("CARBONARA" to "12,00"), confidence = 0.3f))
        assertEquals(BillItem.Issue.PRICE, parsed.items.first().issue)
    }

    @Test
    fun amountParsing() {
        assertEquals(800, ReceiptParser.parseAmount("8,00")?.first)
        assertEquals(1250, ReceiptParser.parseAmount("€12.50")?.first)
        assertEquals(123400, ReceiptParser.parseAmount("1.234,00")?.first)
        assertEquals(-200, ReceiptParser.parseAmount("-2,00")?.first)
        assertEquals(true, ReceiptParser.parseAmount("7,O0")?.second)
        assertNull(ReceiptParser.parseAmount("12"))
        assertNull(ReceiptParser.parseAmount("SO"))
        assertNull(ReceiptParser.parseAmount("Pizza"))
    }
}
