package com.cldnpl.receipty.scanner

import com.cldnpl.receipty.models.BillItem
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.tan

/** Un rettangolo in coordinate pixel dell'immagine (origine in alto a sinistra). */
data class Box(val left: Float, val top: Float, val width: Float, val height: Float) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
    val midX: Float get() = left + width / 2
    val midY: Float get() = top + height / 2
}

/** Un pezzo di testo riconosciuto dall'OCR. */
data class OCRFragment(
    val text: String,
    val confidence: Float,
    /** Le altre letture possibili, dalla più probabile. */
    val alternatives: List<String> = emptyList(),
    val box: Box,
    /** Inclinazione della riga di testo in radianti (0 = orizzontale). */
    val angle: Float = 0f,
)

data class ParsedReceipt(
    val items: List<BillItem>,
    /** Il TOTALE stampato, se c'è. */
    val total: Int?,
)

/**
 * Trasforma il testo di uno scontrino in voci con prezzo.
 *
 * Regola di fondo: nel dubbio non si indovina. Una voce di cui l'OCR non è sicuro
 * viene segnalata, e chi usa l'app la conferma prima di andare avanti.
 *
 * `minorDigits`: cifre decimali della valuta del conto, 2 di solito, 0 per yen, won & co.
 */
object ReceiptParser {
    private val isoCodes: Set<String> by lazy {
        java.util.Currency.getAvailableCurrencies().map { it.currencyCode.uppercase(Locale.ROOT) }.toSet()
    }
    private val localMarkers = setOf("KR", "ZŁ", "KČ", "FT", "LEI", "ЛВ", "DIN", "RS", "TL", "DH", "RM", "RP", "DKK", "SFR")

    data class Line(val fragments: List<OCRFragment>) {
        val text: String get() = fragments.joinToString(" ") { it.text }
    }

    fun parse(fragments: List<OCRFragment>, minorDigits: Int = 2): ParsedReceipt =
        parseLines(groupIntoLines(fragments, minorDigits), minorDigits)

    // Righe

    /** Raggruppa i frammenti che stanno sulla stessa riga, compensando lo scontrino storto. */
    fun groupIntoLines(fragments: List<OCRFragment>, minorDigits: Int = 2): List<Line> {
        if (fragments.isEmpty()) return emptyList()
        return group(fragments, estimateSlope(fragments, minorDigits))
    }

    /**
     * L'inclinazione delle righe. Gli angoli dell'OCR valgono solo per le scritte lunghe
     * (per "8.00" dicono sempre 0), quindi si provano le pendenze fino a ±8° e si tiene quella
     * che mette sulla stessa riga più coppie "nome … prezzo". A parità, la più vicina
     * all'angolo misurato sulle scritte lunghe.
     */
    fun estimateSlope(fragments: List<OCRFragment>, minorDigits: Int = 2): Float {
        val long = fragments.filter { it.box.width > it.box.height * 5 }.map { it.angle }.sorted()
        val hint = if (long.isEmpty()) 0f else tan(long[long.size / 2])
        var bestScore = -1
        var bestSlope = hint
        for (step in -28..28) {
            val slope = step * 0.005f
            val score = group(fragments, slope).count { isPricedItemLine(it, minorDigits) }
            if (score > bestScore || (score == bestScore && abs(slope - hint) < abs(bestSlope - hint))) {
                bestScore = score
                bestSlope = slope
            }
        }
        return bestSlope
    }

    private fun isPricedItemLine(line: Line, minorDigits: Int): Boolean {
        if (line.fragments.size < 2) return false
        val amount = trailingAmount(line, minorDigits) ?: return false
        return amount.rest.any { it.isLetter() }
    }

    private class Row(var y: Float, var height: Float, val items: MutableList<OCRFragment>)

    fun group(fragments: List<OCRFragment>, slope: Float): List<Line> {
        fun level(f: OCRFragment) = f.box.midY - slope * f.box.midX

        val rows = mutableListOf<Row>()
        for (f in fragments.sortedBy { level(it) }) {
            val y = level(f)
            val i = rows.indices.lastOrNull { abs(rows[it].y - y) < min(rows[it].height, f.box.height) * 0.6f }
            if (i != null) {
                val row = rows[i]
                row.items.add(f)
                row.y += (y - row.y) / row.items.size
                row.height = maxOf(row.height, f.box.height)
            } else {
                rows.add(Row(y, f.box.height, mutableListOf(f)))
            }
        }
        return rows
            .sortedBy { it.y }
            .map { row -> Line(row.items.sortedBy { it.box.left }) }
    }

    // Voci

    fun parseLines(lines: List<Line>, minorDigits: Int = 2): ParsedReceipt {
        val items = mutableListOf<BillItem>()
        var total: Int? = null
        var subtotal: Int? = null
        var itemsEnded = false
        var pendingName: Pair<String, Float>? = null
        var pendingQuantity: Pair<Int, Int>? = null

        for (line in lines) {
            val raw = line.text.trim()
            val folded = fold(raw)
            val price = trailingAmount(line, minorDigits)

            if (isTotalLine(folded)) {
                if (price != null) {
                    if (folded.contains("sub")) subtotal = subtotal ?: price.cents else total = total ?: price.cents
                }
                if (items.isNotEmpty()) itemsEnded = true
                continue
            }
            if (itemsEnded) continue
            if (isNoise(folded)) {
                pendingName = null
                continue
            }
            val q = quantityOnlyLine(raw, minorDigits)
            if (q != null) {
                pendingQuantity = q
                continue
            }
            if (price == null) {
                // Un nome senza prezzo: può essere una voce lunga andata a capo.
                pendingName = if (looksLikeName(raw)) raw to minConfidence(line.fragments) else null
                continue
            }
            var p = price

            var name = p.rest
            var nameConfidence = p.restConfidence
            val pending = pendingName
            if (cleanName(name).isEmpty() && pending != null) {
                name = pending.first
                nameConfidence = pending.second
            }
            pendingName = null

            var quantity = 1
            var unitPrice: Int? = null
            val inline = inlineQuantity(name, minorDigits)
            val leading = if (inline == null) leadingQuantity(name) else null
            if (inline != null) {
                quantity = inline.first
                unitPrice = inline.second
                name = inline.third
            } else if (leading != null) {
                quantity = leading.first
                name = leading.second
            }
            pendingQuantity?.let {
                quantity = it.first
                unitPrice = it.second
                pendingQuantity = null
            }
            val unit = unitPrice
            if (unit != null && unit * quantity != p.cents) {
                p = p.copy(uncertain = true)
            }

            val cleaned = cleanName(name)
            if (cleaned.isEmpty()) continue

            val issue = when {
                p.uncertain || p.cents <= 0 || p.cents >= 100_000 -> BillItem.Issue.PRICE
                nameConfidence < 0.45f || nameLooksGarbled(cleaned) -> BillItem.Issue.NAME
                else -> null
            }
            items.add(BillItem(name = cleaned, quantity = quantity, cents = p.cents, issue = issue))
        }

        return ParsedReceipt(items, total ?: subtotal)
    }

    // Importi

    data class Amount(
        val cents: Int,
        val uncertain: Boolean,
        /** Il testo della riga prima del prezzo. */
        val rest: String,
        val restConfidence: Float,
    )

    private data class Token(val text: String, val fragment: Int)

    private val whitespace = Regex("\\s+")

    /**
     * Il prezzo in fondo alla riga, se c'è. Tollera le sviste tipiche dell'OCR (O al posto di 0,
     * S al posto di 5...) ma in quel caso segna il prezzo come da confermare.
     */
    fun trailingAmount(line: Line, minorDigits: Int = 2): Amount? {
        val tokens = mutableListOf<Token>()
        line.fragments.forEachIndexed { i, f ->
            for (piece in f.text.split(whitespace)) if (piece.isNotEmpty()) tokens.add(Token(piece, i))
        }
        // Via simboli di valuta e codici IVA in coda ("5,00 €", "5,00 B").
        while (tokens.isNotEmpty() && isTrailingMarker(tokens.last().text)) tokens.removeAt(tokens.lastIndex)
        val last = tokens.lastOrNull() ?: return null

        val candidates = mutableListOf(last.text to 1)
        if (tokens.size >= 2) {
            val prev = tokens[tokens.size - 2].text
            // "40, 00" / "40 ,00" / "€ 40,00" spezzati dall'OCR.
            if (prev.endsWith(",") || prev.endsWith(".") || last.text.startsWith(",") || last.text.startsWith(".")) {
                candidates.add(0, (prev + last.text) to 2)
            }
        }

        for ((text, used) in candidates) {
            val parsed = parseAmount(text, minorDigits) ?: continue
            val priceTokens = tokens.takeLast(used)
            val restTokens = tokens.dropLast(used)
            val priceFragments = priceTokens.map { it.fragment }.toSet()
            val restFragments = restTokens.map { it.fragment }.toSet()

            var uncertain = parsed.second
            for (i in priceFragments) {
                val f = line.fragments[i]
                if (f.confidence < 0.5f) uncertain = true
                // Se una lettura alternativa dà un prezzo diverso, l'OCR stava tirando a indovinare.
                for (alt in f.alternatives) {
                    val a = lastAmount(alt, minorDigits)
                    if (a != null && a != parsed.first) uncertain = true
                }
            }
            val rest = restTokens.joinToString(" ") { it.text }
            val confidence = restFragments.minOfOrNull { line.fragments[it].confidence } ?: 1f
            return Amount(parsed.first, uncertain, rest, confidence)
        }
        return null
    }

    private fun lastAmount(text: String, minorDigits: Int): Int? {
        val tokens = text.split(whitespace).filter { it.isNotEmpty() }.toMutableList()
        while (tokens.isNotEmpty() && isTrailingMarker(tokens.last())) tokens.removeAt(tokens.lastIndex)
        val last = tokens.lastOrNull() ?: return null
        return parseAmount(last, minorDigits)?.first
    }

    private val markerWords = setOf("EURO", "*", "-", "B", "C", "D", "E", "A", "S", "N", "V", "IVA", "TAX")

    private fun isCurrencySymbol(c: Char) = Character.getType(c) == Character.CURRENCY_SYMBOL.toInt()

    private fun isTrailingMarker(t: String): Boolean {
        val u = t.uppercase().trim('.')
        if (u in markerWords) return true
        if (u in isoCodes || u in localMarkers) return true
        if (u.isNotEmpty() && u.all(::isCurrencySymbol)) return true
        if (u.endsWith("%") && u.dropLast(1).all { it.isDigit() || it == ',' || it == '.' }) return true
        return false
    }

    private val symbolPrefix = Regex("^[A-Za-z]{1,3}(?=\\p{Sc})")
    private val fixes = mapOf(
        'O' to '0', 'o' to '0', 'D' to '0', 'Q' to '0', 'l' to '1', 'I' to '1',
        '|' to '1', 'i' to '1', 'S' to '5', 's' to '5', 'B' to '8', 'Z' to '2', 'z' to '2',
    )

    /**
     * "8,00" → 800. "€12.00", "$12.00", "CHF12.00", "S$12.00" → 1200. "7,O0" → 700 (corretto).
     * "12" → null (manca la parte decimale), a meno che la valuta non abbia decimali: "¥1,200" → 120000.
     * Restituisce i centesimi e se c'è stato da correggere qualcosa.
     */
    fun parseAmount(token: String, minorDigits: Int = 2): Pair<Int, Boolean>? {
        var t = token.trim('*', ':', ';')
        // Prefissi tipo "US$", "S$", "HK$": le lettere vanno via solo se attaccate a un simbolo.
        t = symbolPrefix.replaceFirst(t, "")
        t = t.filterNot(::isCurrencySymbol)
        // Codici ISO attaccati al numero: "CHF12.50", "12.50EUR".
        if (t.length > 3) {
            val head = t.take(3).uppercase()
            val tail = t.takeLast(3).uppercase()
            if (head in isoCodes) t = t.drop(3) else if (tail in isoCodes) t = t.dropLast(3)
        }
        if (t.isEmpty()) return null
        if (minorDigits == 0) wholeUnits(t)?.let { return (it * 100) to false }

        val out = StringBuilder()
        var corrected = false
        var digits = 0
        for (ch in t) {
            when {
                ch in '0'..'9' -> {
                    out.append(ch)
                    digits += 1
                }
                ch == ',' || ch == '.' || ch == '-' -> out.append(ch)
                fixes.containsKey(ch) -> {
                    out.append(fixes.getValue(ch))
                    corrected = true
                }
                else -> return null
            }
        }
        if (digits < 2) return null

        var s = out.toString()
        var negative = false
        if (s.startsWith("-")) {
            negative = true
            s = s.drop(1)
        }
        if (s.endsWith("-")) {
            negative = true
            s = s.dropLast(1)
        }

        // Ultimo separatore = decimali, e devono essere esattamente due cifre.
        val sep = s.indexOfLast { it == ',' || it == '.' }
        if (sep < 0) return null
        val fraction = s.substring(sep + 1)
        val head = s.substring(0, sep)
        val whole = head.filter { it.isDigit() }
        if (fraction.length != 2 || !fraction.all { it in '0'..'9' } || whole.isEmpty() || whole.length > 5 ||
            !head.all { it in '0'..'9' || it == '.' || it == ',' }
        ) return null
        val w = whole.toIntOrNull() ?: return null
        val f = fraction.toIntOrNull() ?: return null
        val cents = w * 100 + f
        return (if (negative) -cents else cents) to corrected
    }

    private val wholePattern = Regex("^\\d{1,3}([.,]\\d{3})+$|^\\d{2,8}$")

    /**
     * Importi senza decimali per yen, won & co.: "1,200", "1.200", "850". Sotto 50 no:
     * su uno scontrino in yen un "12" è un tavolo, non un prezzo.
     */
    private fun wholeUnits(t: String): Int? {
        if (!wholePattern.containsMatchIn(t)) return null
        val value = t.filter { it.isDigit() }.toIntOrNull() ?: return null
        return if (value >= 50) value else null
    }

    // Quantità

    private val quantityOnly = Regex("^\\s*(\\d{1,2})\\s*[xX×*]\\s*(?:€\\s*)?(\\d{1,4}[.,]\\d{2})\\s*$")
    private val quantityInline = Regex("^(.*?)\\s*(\\d{1,2})\\s*[xX×*]\\s*(?:€\\s*)?(\\d{1,4}[.,]\\d{2})\\s*$")
    private val quantityLeading = Regex("^\\s*(\\d{1,2})\\s*[xX×*]\\s+(.+)$")

    /** Riga "2 x 2,50" sopra la voce, tipica dei documenti commerciali italiani. Quantità e prezzo unitario. */
    fun quantityOnlyLine(text: String, minorDigits: Int = 2): Pair<Int, Int>? {
        val m = quantityOnly.find(text) ?: return null
        val qty = m.groupValues[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
        val unit = parseAmount(m.groupValues[2], minorDigits)?.first ?: return null
        return qty to unit
    }

    /** "Birra 2 x 2,50" → quantità 2 da 2,50, nome "Birra". */
    fun inlineQuantity(text: String, minorDigits: Int = 2): Triple<Int, Int, String>? {
        val m = quantityInline.find(text) ?: return null
        val qty = m.groupValues[2].toIntOrNull()?.takeIf { it > 0 } ?: return null
        val unit = parseAmount(m.groupValues[3], minorDigits)?.first ?: return null
        return Triple(qty, unit, m.groupValues[1])
    }

    /** "2 x Birra" / "2x Birra". Un numero da solo non basta: "4 formaggi" è una pizza. */
    fun leadingQuantity(text: String): Pair<Int, String>? {
        val m = quantityLeading.find(text) ?: return null
        val qty = m.groupValues[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
        return qty to m.groupValues[2]
    }

    // Classificazione

    private val marks = Regex("\\p{Mn}+")

    fun fold(s: String): String =
        marks.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "").lowercase(Locale.ITALIAN)

    private fun words(folded: String) = folded.split(Regex("[^\\p{L}]+")).filter { it.isNotEmpty() }

    private val totalWords = setOf("totale", "total", "tot", "subtotale", "subtotal", "sub", "importo", "amount", "balance", "grand")
    private val totalPhrases = listOf("da pagare", "totale complessivo", "amount due", "balance due", "grand total")

    fun isTotalLine(folded: String): Boolean {
        val first = words(folded).firstOrNull() ?: return false
        if (first in totalWords) return true
        return totalPhrases.any { folded.contains(it) }
    }

    private val noiseWords = setOf(
        "tax", "vat", "cash", "change", "tendered", "card", "debit", "approved", "auth",
        "thank", "thanks", "phone", "date", "time", "iva", "imponibile", "partita", "tel", "telefono", "fax", "resto", "contanti",
        "contante", "pagamento", "pagato", "elettronico", "carta", "bancomat", "visa",
        "mastercard", "credito", "ticket", "buono", "matricola", "documento", "cassa",
        "cassiere", "operatore", "ore", "data", "grazie", "arrivederci", "pezzi",
        "articoli", "descrizione", "prezzo", "rt", "cf",
    )
    private val noisePhrases = listOf("p.iva", "p. iva", "c.f.", "cod. fisc", "scontrino n", "doc.", "www", "http")
    private val datePattern = Regex("\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4}")
    private val timePattern = Regex("\\b\\d{1,2}:\\d{2}\\b")

    /**
     * Righe con un numero che sembra un prezzo ma non sono voci: IVA, pagamento, resto, intestazione.
     * Parole intere, non prefissi: "cassata" è un dolce, "fiore di latte" non è un orario.
     * Scontrini italiani e inglesi.
     */
    fun isNoise(folded: String): Boolean {
        if (words(folded).any { it in noiseWords }) return true
        if (noisePhrases.any { folded.contains(it) }) return true
        // Date e orari ("28/09/2026", "20:31") hanno l'aspetto di importi.
        if (datePattern.containsMatchIn(folded)) return true
        if (timePattern.containsMatchIn(folded)) return true
        return false
    }

    private fun looksLikeName(text: String): Boolean {
        val letters = text.count { it.isLetter() }
        return letters >= 3 && letters.toDouble() / maxOf(1, text.length) > 0.6
    }

    private fun minConfidence(fragments: List<OCRFragment>): Float = fragments.minOfOrNull { it.confidence } ?: 1f

    private val vatRate = Regex("\\d{1,2}([.,]\\d+)?\\s?%")
    private val trailingDots = Regex("(\\s+\\.+|\\.{2,})$")
    private val leadingDots = Regex("^\\.+")
    private val trimSet = "*-:;,_=#".toSet()

    /** Nome pulito, con la maiuscola solo all'inizio se lo scontrino era tutto in maiuscolo. */
    fun cleanName(text: String): String {
        var s = text
        // Aliquote IVA e simboli rimasti in mezzo.
        s = vatRate.replace(s, "")
        s = s.replace("€", "")
        s = s.trim { it.isWhitespace() || it in trimSet }
        // Il punto di un'abbreviazione resta ("Acqua nat."), i puntini di riempimento no ("Pasta .....").
        s = trailingDots.replace(s, "")
        s = leadingDots.replace(s, "")
        s = s.split(whitespace).filter { it.isNotEmpty() }.joinToString(" ")
        if (!s.any { it.isLetter() }) return ""
        if (s == s.uppercase()) {
            val lower = s.lowercase()
            s = lower.take(1).uppercase() + lower.drop(1)
        }
        return s
    }

    private val oddCharacters = "?|_~#@^<>[]{}\\▒░▓■□".toSet()

    /** Caratteri che su un menu non ci sono: segno che l'OCR ha letto male. */
    fun nameLooksGarbled(name: String): Boolean {
        if (name.length < 3) return true
        if (name.any { it in oddCharacters }) return true
        val letters = name.count { it.isLetter() }
        return letters.toDouble() / name.count { !it.isWhitespace() } < 0.6
    }
}
