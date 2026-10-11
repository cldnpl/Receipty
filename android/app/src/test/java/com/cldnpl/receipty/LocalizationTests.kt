package com.cldnpl.receipty

import com.cldnpl.receipty.localization.AppLanguage
import com.cldnpl.receipty.localization.StringsFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Le traduzioni: nessuna chiave dimenticata, nessun segnaposto perso per strada.
 * Un `%s` in meno in una lingua non si vede compilando, ma a schermo manca un importo.
 */
class LocalizationTests {

    private val dir = File("src/main/assets/i18n")

    private fun table(code: String) = StringsFile.parse(File(dir, "$code.strings").readText())
    private fun overrides(code: String) = File(dir, "android/$code.strings").takeIf { it.exists() }
        ?.let { StringsFile.parse(it.readText()) } ?: emptyMap()

    private val placeholder = Regex("%(?:\\d+\\$)?[sd]")
    private fun placeholders(text: String) = placeholder.findAll(text).map { it.value }.sorted().toList()

    @Test
    fun everyLanguageTranslatesEveryKey() {
        val english = table("en")
        assertTrue(english.size > 150)

        for (language in AppLanguage.entries) {
            if (language == AppLanguage.SYSTEM) continue
            val translated = table(language.raw)
            assertEquals("${language.raw}: chiavi diverse dall'inglese", english.keys, translated.keys)
            for ((key, base) in english) {
                val text = translated.getValue(key)
                assertFalse("${language.raw}: $key è vuota", text.isBlank())
                assertEquals("${language.raw}: segnaposto diversi in $key", placeholders(base), placeholders(text))
                // Il formato deve funzionare davvero, con stringhe e numeri al posto giusto.
                val args = placeholders(base).sortedBy { it.filter(Char::isDigit) }.map<String, Any> { if (it.endsWith("d")) 2 else "x" }
                String.format(text, *args.toTypedArray())
            }
        }
    }

    /** Su Android "iPhone" diventa "telefono": le sostituzioni devono esserci in ogni lingua. */
    @Test
    fun androidOverridesCoverEveryLanguage() {
        for (language in AppLanguage.entries) {
            if (language == AppLanguage.SYSTEM) continue
            val merged = table(language.raw) + overrides(language.raw)
            for ((key, text) in merged) {
                assertFalse("${language.raw}: $key dice ancora iPhone", text.contains("iPhone"))
            }
            for (key in overrides(language.raw).keys) {
                assertTrue("${language.raw}: $key non esiste", table("en").containsKey(key))
            }
        }
    }
}
