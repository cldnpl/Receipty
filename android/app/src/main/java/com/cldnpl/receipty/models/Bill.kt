package com.cldnpl.receipty.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

@Serializable
data class Person(
    @Serializable(with = UUIDSerializer::class) val id: UUID = UUID.randomUUID(),
    val name: String,
) {
    val initial: String
        get() {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return ""
            return trimmed.substring(0, trimmed.offsetByCodePoints(0, 1)).uppercase()
        }
}

data class BillItem(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val quantity: Int = 1,
    /** Prezzo della riga (quantità già inclusa), in centesimi. */
    val cents: Int,
    val assignees: Set<UUID> = emptySet(),
    val issue: Issue? = null,
) {
    enum class Issue {
        /** L'OCR non è sicuro del nome. */
        NAME,

        /** L'OCR non è sicuro del prezzo: finché non lo conferma qualcuno non si va avanti. */
        PRICE,
    }
}

/** Lo sconto sul totale: in percentuale ("10" = 10%) o in soldi ("5" = €5). */
enum class DiscountMode { PERCENT, AMOUNT }

/**
 * Da dove arriva il conto: la schermata iniziale offre solo due strade,
 * ma "Continua manualmente" dallo scanner porta a un conto a voci scritto a mano.
 */
@Serializable
enum class BillSource {
    scan, typed, equal;

    val isItemized: Boolean get() = this != equal
}

/**
 * Il risultato di un conto: quanto doveva e quanto ha pagato ciascuno, e chi dà quanto a chi.
 *
 * Se sul piatto sono finiti più soldi del totale, il ristorante dà il resto: `changeSplit`
 * dice chi si tiene quanto del resto, e `transfers` quello che resta da saldare dopo.
 */
@Serializable
data class Settlement(
    val total: Int,
    val lines: List<Line>,
    val transfers: List<Transfer>,
    val changeSplit: List<ChangeShare> = emptyList(),
    val currency: Currency = Currency("EUR"),
    /** Lo sconto già tolto da `total` (0 se non c'era). */
    val discount: Int = 0,
) {
    @Serializable
    data class Line(
        val person: Person,
        val owed: Int,
        val paid: Int,
        /** La parte del resto che questa persona si tiene. */
        val changeKept: Int = 0,
    ) {
        val id: UUID get() = person.id
        val balance: Int get() = paid - changeKept - owed
    }

    @Serializable
    data class ChangeShare(val person: Person, val cents: Int) {
        val id: UUID get() = person.id
    }

    @Serializable
    data class Transfer(val from: Person, val to: Person, val cents: Int) {
        val id: String get() = "${from.id}-${to.id}"
    }

    val change: Int get() = changeSplit.sumOf { it.cents }
}

@Serializable
data class SavedBill(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    /** Millisecondi dal 1970. */
    val date: Long,
    val source: BillSource,
    val itemCount: Int,
    val settlement: Settlement,
) {
    val names: String get() = settlement.lines.joinToString(", ") { it.person.name }
}
