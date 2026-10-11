package com.cldnpl.receipty

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cldnpl.receipty.models.Currency
import com.cldnpl.receipty.scanner.ReceiptReader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** OCR vero (ML Kit) su foto di scontrini: lo stesso percorso dell'app, dalla foto alle voci. */
@RunWith(AndroidJUnit4::class)
class ReceiptReaderTests {

    private fun photo(name: String) = InstrumentationRegistry.getInstrumentation().context.assets
        .open("$name.jpg").use { BitmapFactory.decodeStream(it) }

    /** Scontrino fotografato storto (3°) su un tavolo: i prezzi devono restare sulla loro riga. */
    @Test
    fun tiltedReceiptPhoto() = runBlocking {
        val image = photo("tilted_receipt")
        val parsed = ReceiptReader.read(image, Currency("EUR"))
        // ML Kit a volte spezza una parola ("Margheri ta") dove Vision non lo fa: i nomi si
        // confrontano senza spazi, gli importi restano esatti. Il nome si corregge con un tocco.
        assertEquals(
            listOf("Margherita", "Carbonara", "Draftbeer", "Stillwater", "Tiramisu", "Covercharge"),
            parsed.items.map { it.name.replace(" ", "") },
        )
        assertEquals(listOf(800, 1200, 500, 200, 600, 700), parsed.items.map { it.cents })
        assertEquals(2, parsed.items[2].quantity)
        assertEquals(4000, parsed.total)
    }
}
