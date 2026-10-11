package com.cldnpl.receipty.models

import com.cldnpl.receipty.localization.I18n
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Tutti gli importi dell'app sono centesimi interi: niente errori di arrotondamento
 * tra quello che si vede e quello che si somma.
 */
object Money {
    /** Il separatore decimale della lingua dell'app: "." in inglese, "," in italiano. */
    val separator: String get() = I18n.decimalSeparator

    /** Per i campi di testo: 1200 → "12", 1250 → "12.50" (o "12,50"). */
    fun editable(cents: Int): String {
        val value = abs(cents)
        val whole = (if (cents < 0) "-" else "") + (value / 100).toString()
        return if (value % 100 == 0) whole else whole + separator + "%02d".format(Locale.ROOT, value % 100)
    }

    /** 1250 → "12.50" / "12,50": quello che resta nel campo quando si smette di scrivere. */
    fun editableFixed(cents: Int, decimals: Boolean): String =
        if (decimals) (cents / 100).toString() + separator + "%02d".format(Locale.ROOT, cents % 100)
        else (cents / 100).toString()

    /** Legge quello che una persona scrive, con la virgola o col punto: "12", "12,5", "12.50", "€ 1,234.50". */
    fun parse(text: String): Int? {
        var t = text.filter { !it.isWhitespace() && Character.getType(it) != Character.CURRENCY_SYMBOL.toInt() }
        if (t.isEmpty()) return null
        var negative = false
        if (t.startsWith("-") || t.startsWith("−")) {
            negative = true
            t = t.substring(1)
        }
        if (t.isEmpty() || !t.all { it in '0'..'9' || it == ',' || it == '.' }) return null

        var euros = t
        var fraction = ""
        val sep = t.indexOfLast { it == ',' || it == '.' }
        if (sep >= 0) {
            val tail = t.substring(sep + 1)
            val head = t.substring(0, sep)
            if (tail.length == 3 && head.isNotEmpty()) {
                // "1.234" o "1,234": separatore delle migliaia, non decimali.
                euros = head + tail
            } else if (tail.length <= 2) {
                euros = head
                fraction = tail
            } else {
                return null
            }
        }
        euros = euros.filter { it in '0'..'9' }
        if (euros.isEmpty() && fraction.isEmpty()) return null
        val e = (if (euros.isEmpty()) "0" else euros).toIntOrNull() ?: return null
        if (e >= 1_000_000) return null
        val f = fraction.padEnd(2, '0').take(2).toIntOrNull() ?: 0
        val cents = e * 100 + f
        return if (negative) -cents else cents
    }

    /**
     * Filtra quello che si digita in un campo importo: cifre e un solo separatore (mostrato come
     * quello della lingua in uso, qualunque sia la tastiera), al massimo due decimali.
     * Niente decimali per yen & co.
     */
    fun sanitizeInput(text: String, decimals: Boolean = true): String {
        val separator = separator
        val out = StringBuilder()
        var seenSeparator = false
        var digits = 0
        for (ch in text) {
            if (ch in '0'..'9') {
                if (seenSeparator) {
                    if (digits >= 2) continue
                    digits += 1
                }
                if (out.length >= 7 && !seenSeparator) continue
                out.append(ch)
            } else if (decimals && (ch == ',' || ch == '.') && !seenSeparator) {
                seenSeparator = true
                if (out.isEmpty()) out.append("0")
                out.append(separator)
            }
        }
        return out.toString()
    }
}

object DateText {
    private var cached: Pair<Locale, SimpleDateFormat>? = null

    /**
     * "24 Sep 2026", "24 set 2026", "2026年9月24日": giorno, mese e anno come si scrivono
     * nella lingua dell'app.
     */
    @Synchronized
    fun short(millis: Long): String {
        val locale = I18n.locale
        val formatter = cached?.takeIf { it.first == locale }?.second ?: run {
            val pattern = runCatching { android.text.format.DateFormat.getBestDateTimePattern(locale, "d MMM y") }
                .getOrNull() ?: "d MMM y"
            SimpleDateFormat(pattern, locale).also { cached = locale to it }
        }
        return formatter.format(Date(millis))
    }
}
