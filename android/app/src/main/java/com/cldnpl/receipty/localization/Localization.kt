package com.cldnpl.receipty.localization

import android.app.LocaleManager
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cldnpl.receipty.models.AppSettings
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Le lingue dell'app. `SYSTEM` segue quella del telefono, le altre la forzano.
 *
 * `raw` è anche il nome del file in `assets/i18n` (gli stessi `.strings` dell'app iOS).
 */
enum class AppLanguage(val raw: String) {
    SYSTEM("system"),
    ENGLISH("en"),
    ITALIAN("it"),
    SPANISH("es"),
    FRENCH("fr"),
    GERMAN("de"),
    PORTUGUESE("pt-BR"),
    DUTCH("nl"),
    TURKISH("tr"),
    RUSSIAN("ru"),
    JAPANESE("ja"),
    KOREAN("ko"),
    CHINESE("zh-Hans");

    val id: String get() = raw

    /** Il nome della lingua scritto in quella lingua: si riconosce anche senza capire l'inglese. */
    val nativeName: String
        get() = when (this) {
            SYSTEM -> t("language.system")
            ENGLISH -> "English"
            ITALIAN -> "Italiano"
            SPANISH -> "Español"
            FRENCH -> "Français"
            GERMAN -> "Deutsch"
            PORTUGUESE -> "Português (Brasil)"
            DUTCH -> "Nederlands"
            TURKISH -> "Türkçe"
            RUSSIAN -> "Русский"
            JAPANESE -> "日本語"
            KOREAN -> "한국어"
            CHINESE -> "简体中文"
        }

    /**
     * Il nome della lingua nella lingua in uso ("Japanese" se l'app è in inglese).
     * Per `SYSTEM`, la lingua che il telefono ha scelto.
     */
    val localizedName: String?
        get() {
            val code = if (this == SYSTEM) I18n.systemLanguageCode() else raw
            val target = Locale.forLanguageTag(code)
            val name = target.getDisplayName(I18n.locale).takeIf { it.isNotBlank() }
                ?: target.getDisplayName(target).takeIf { it.isNotBlank() }
                ?: return null
            if (name.lowercase() == nativeName.lowercase()) return null
            return name.substring(0, 1).uppercase(I18n.locale) + name.substring(1)
        }

    companion object {
        fun from(raw: String?): AppLanguage? = entries.firstOrNull { it.raw == raw }
    }
}

/**
 * La lingua dell'interfaccia. Le stringhe arrivano dal file della lingua scelta in
 * `assets/i18n`: cambiarla si vede subito, senza riavviare l'app.
 *
 * Lo stato è osservabile da Compose e `t(_)` lo legge: ogni composable che chiama `t`
 * si ridisegna da sé quando la lingua cambia.
 */
object I18n {
    const val DEFAULTS_KEY = "language"

    private var assets: AssetManager? = null
    private var appContext: Context? = null

    var language by mutableStateOf(AppLanguage.SYSTEM)
        private set

    /** La lingua con cui si formattano importi e date. */
    var locale: Locale by mutableStateOf(Locale.getDefault())
        private set

    /** Le stringhe della lingua scelta. */
    private var table by mutableStateOf<Map<String, String>>(emptyMap())

    /** L'inglese: la rete di sicurezza se una chiave manca nella lingua scelta. */
    private var fallback: Map<String, String> = emptyMap()

    fun init(context: Context) {
        appContext = context.applicationContext
        assets = context.assets
        fallback = load("en")
        language = AppLanguage.from(AppSettings.string(DEFAULTS_KEY)) ?: AppLanguage.SYSTEM
        syncWithSystem(context)
        resolve()
    }

    /** Sceglie una lingua. Su Android 13+ la passa anche al sistema (tastiera, avvisi, impostazioni). */
    fun select(language: AppLanguage) {
        if (language == this.language) return
        this.language = language
        AppSettings.putString(DEFAULTS_KEY, language.raw)
        resolve()
        val context = appContext ?: return
        if (Build.VERSION.SDK_INT >= 33) {
            runCatching {
                context.getSystemService(LocaleManager::class.java).applicationLocales =
                    if (language == AppLanguage.SYSTEM) LocaleList.getEmptyLocaleList()
                    else LocaleList.forLanguageTags(language.raw)
            }
        }
    }

    /**
     * Su Android 13+ la lingua dell'app si può cambiare anche dalle impostazioni di sistema:
     * quella scelta lì vince. Da chiamare all'avvio e quando cambia la configurazione.
     */
    fun syncWithSystem(context: Context) {
        if (Build.VERSION.SDK_INT < 33) {
            resolve()
            return
        }
        val locales = runCatching { context.getSystemService(LocaleManager::class.java).applicationLocales }.getOrNull()
            ?: return
        val chosen = if (locales.isEmpty) AppLanguage.SYSTEM else match(locales[0])
        if (chosen != language) {
            language = chosen
            AppSettings.putString(DEFAULTS_KEY, chosen.raw)
        }
        resolve()
    }

    private fun match(locale: Locale): AppLanguage {
        AppLanguage.from(locale.toLanguageTag())?.let { return it }
        return AppLanguage.from(supportedCode(locale)) ?: AppLanguage.ENGLISH
    }

    /** Per i test: la lingua senza toccare preferenze e sistema. */
    fun setForTesting(language: AppLanguage) {
        this.language = language
        resolve()
    }

    private fun resolve() {
        if (language == AppLanguage.SYSTEM) {
            table = load(systemLanguageCode())
            locale = systemLocales().firstOrNull() ?: Locale.getDefault()
        } else {
            table = load(language.raw)
            locale = Locale.forLanguageTag(language.raw)
        }
    }

    /** La lingua e il paese del telefono, qualunque lingua abbia l'app. */
    fun deviceLocale(): Locale = systemLocales().firstOrNull() ?: Locale.getDefault()

    /** Le lingue del telefono, non quelle dell'app (su Android 13+ possono essere diverse). */
    private fun systemLocales(): List<Locale> {
        val list: LocaleList? = runCatching {
            if (Build.VERSION.SDK_INT >= 33) appContext?.getSystemService(LocaleManager::class.java)?.systemLocales
            else null
        }.getOrNull() ?: runCatching { Resources.getSystem().configuration.locales }.getOrNull()
        if (list == null) return listOf(Locale.getDefault())
        return (0 until list.size()).map { list[it] }
    }

    /** La lingua dell'app che corrisponde meglio a quelle del telefono, nell'ordine preferito. */
    fun systemLanguageCode(): String {
        for (l in systemLocales()) {
            val code = supportedCode(l) ?: continue
            return code
        }
        return "en"
    }

    private fun supportedCode(locale: Locale): String? = when (val lang = locale.language) {
        "zh" -> "zh-Hans"
        "pt" -> "pt-BR"
        else -> lang.takeIf { code -> AppLanguage.entries.any { it.raw == code } }
    }

    private fun load(code: String): Map<String, String> {
        val assets = assets ?: return emptyMap()
        fun read(path: String) = runCatching {
            assets.open(path).bufferedReader(Charsets.UTF_8).use { StringsFile.parse(it.readText()) }
        }.getOrNull() ?: emptyMap()
        // Le stringhe di iOS, più le poche che su Android dicono "telefono" invece di "iPhone".
        return read("i18n/$code.strings") + read("i18n/android/$code.strings")
    }

    /** Una stringa tradotta; se manca nella lingua scelta si ripiega sull'inglese, poi sulla chiave. */
    fun string(key: String): String = table[key] ?: fallback[key] ?: key

    /** Il separatore decimale della lingua in uso: i campi importo scrivono questo. */
    val decimalSeparator: String
        get() = DecimalFormatSymbols.getInstance(locale).decimalSeparator.toString()
}

/** I file `.strings` di iOS: `"chiave" = "valore";`, con i commenti `/* */`. */
object StringsFile {
    private val entry = Regex("^\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*=\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*;")

    fun parse(text: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (line in text.lineSequence()) {
            val m = entry.find(line) ?: continue
            out[unescape(m.groupValues[1])] = toJavaFormat(unescape(m.groupValues[2]))
        }
        return out
    }

    private fun unescape(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val n = s[i + 1]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    else -> sb.append(n)
                }
                i += 2
            } else {
                sb.append(c)
                i += 1
            }
        }
        return sb.toString()
    }

    /** `%@` di Objective-C diventa `%s`, anche nella forma posizionale `%1$@`. */
    private fun toJavaFormat(s: String) = s.replace("%@", "%s").replace("$@", "\$s")
}

/** La stringa tradotta per `key`. Chiamata da un composable, lo rende dipendente dalla lingua. */
fun t(key: String): String = I18n.string(key)

/** Come `t(key)`, con i segnaposto (`%s`, `%d`, `%1$s`…) riempiti. */
fun t(key: String, vararg args: Any): String =
    runCatching { String.format(I18n.locale, I18n.string(key), *args) }.getOrElse { I18n.string(key) }
