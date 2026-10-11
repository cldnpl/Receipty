package com.cldnpl.receipty.models

import androidx.compose.runtime.compositionLocalOf
import com.cldnpl.receipty.localization.I18n
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.text.Collator
import java.text.NumberFormat
import java.util.Locale
import java.util.Currency as JavaCurrency

/**
 * La valuta di un conto. Ogni conto si ricorda la sua: cambiare valuta nelle impostazioni
 * vale per i conti nuovi, non riscrive quelli di un viaggio passato.
 */
@Serializable(with = CurrencySerializer::class)
class Currency(code: String) {
    val code: String = code.uppercase(Locale.ROOT)
    val id: String get() = code

    private val java: JavaCurrency? = runCatching { JavaCurrency.getInstance(this.code) }.getOrNull()

    /**
     * "Euro", "Japanese Yen", "Franco svizzero"…: il nome nella lingua dell'app.
     * L'iniziale maiuscola la mettiamo noi: in italiano e francese il sistema li dà minuscoli,
     * e in un elenco di righe stonano.
     */
    val name: String
        get() {
            val name = java?.getDisplayName(I18n.locale) ?: return code
            if (name.isEmpty()) return code
            return name.substring(0, 1).uppercase(I18n.locale) + name.substring(1)
        }

    /**
     * Il simbolo non segue la lingua: serve corto, come glifo nei cerchietti e davanti ai campi
     * ("$", non "USD").
     */
    val symbol: String get() = java?.getSymbol(SYMBOL_LOCALE) ?: code

    /** 2 per euro e dollaro, 0 per yen, won, corona islandese... */
    val minorDigits: Int get() = java?.defaultFractionDigits?.coerceAtLeast(0) ?: 2

    /** Il passo di arrotondamento in centesimi: 1 centesimo, oppure 1 unità intera (100). */
    val unit: Int get() = if (minorDigits == 0) 100 else 1

    /** Per scrivere un importo intero: "€22.30" in inglese, "22,30 €" in italiano. */
    internal fun localizedFormatter(): NumberFormat = formatter(code, I18n.locale)

    override fun equals(other: Any?) = other is Currency && other.code == code
    override fun hashCode() = code.hashCode()
    override fun toString() = code

    companion object {
        private val SYMBOL_LOCALE: Locale = Locale.US
        private val cache = HashMap<String, NumberFormat>()

        @Synchronized
        private fun formatter(code: String, locale: Locale): NumberFormat {
            val key = "$code|${locale.toLanguageTag()}"
            cache[key]?.let { return it }
            val f = NumberFormat.getCurrencyInstance(locale)
            runCatching {
                val currency = JavaCurrency.getInstance(code)
                f.currency = currency
                val digits = currency.defaultFractionDigits.coerceAtLeast(0)
                f.minimumFractionDigits = digits
                f.maximumFractionDigits = digits
            }
            cache[key] = f
            return f
        }

        /**
         * Le valute proposte: le più usate in viaggio e nel mondo, con 0 o 2 decimali
         * (dinaro kuwaitiano & co. a tre decimali restano fuori: i conti sono in centesimi).
         */
        private val codes = listOf(
            "EUR", "USD", "GBP", "CHF", "JPY", "CNY", "CAD", "AUD", "NZD", "HKD", "SGD", "INR",
            "KRW", "SEK", "NOK", "DKK", "ISK", "PLN", "CZK", "HUF", "RON", "BGN", "RSD", "ALL",
            "MKD", "BAM", "MDL", "UAH", "GEL", "AMD", "AZN", "TRY", "ILS", "AED", "SAR", "QAR",
            "EGP", "MAD", "ZAR", "NGN", "KES", "GHS", "TZS", "UGX", "BRL", "MXN", "ARS", "CLP",
            "COP", "PEN", "UYU", "CRC", "DOP", "THB", "VND", "IDR", "MYR", "PHP", "TWD", "PKR",
            "BDT", "LKR", "NPR", "KZT", "UZS", "MNT", "KHR", "LAK",
        )

        private var sorted: Pair<Locale, List<Currency>>? = null

        /** In ordine alfabetico per nome, quindi un ordine diverso in ogni lingua. */
        val all: List<Currency>
            get() {
                val locale = I18n.locale
                sorted?.let { if (it.first == locale) return it.second }
                val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
                val list = codes.map(::Currency).sortedWith { a, b -> collator.compare(a.name, b.name) }
                sorted = locale to list
                return list
            }

        const val DEFAULTS_KEY = "currency"

        /** La valuta scelta nelle impostazioni, o quella del telefono, o l'euro. */
        val current: Currency
            get() = AppSettings.currencyCode?.let(::Currency) ?: deviceDefault

        val deviceDefault: Currency
            get() {
                // Il paese del telefono, non la lingua scelta nell'app: un italiano a Londra paga in sterline.
                val code = runCatching { JavaCurrency.getInstance(I18n.deviceLocale()).currencyCode }.getOrNull() ?: "EUR"
                return if (codes.contains(code)) Currency(code) else Currency("EUR")
            }
    }
}

object CurrencySerializer : KSerializer<Currency> {
    override val descriptor = PrimitiveSerialDescriptor("Currency", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Currency) = encoder.encodeString(value.code)
    override fun deserialize(decoder: Decoder) = Currency(decoder.decodeString())
}

/** La valuta del conto mostrato: la imposta RootView per il percorso in corso o per il conto salvato. */
val LocalCurrency = compositionLocalOf { Currency.current }

/** 2230 → "€22.30", "22,30 €", "¥2,230": nella lingua dell'app (per lo yen i centesimi sono già unità intere × 100). */
fun Money.format(cents: Int, currency: Currency): String {
    val value = BigDecimal(cents).movePointLeft(2)
    val text = runCatching { currency.localizedFormatter().format(value) }.getOrNull()
        ?: (currency.symbol + editable(cents))
    return text.replace("-", "−")
}

/** Zero scritto corto, come nel mockup: "€0". */
fun Money.formatCompact(cents: Int, currency: Currency): String =
    if (cents == 0) currency.symbol + "0" else format(cents, currency)
