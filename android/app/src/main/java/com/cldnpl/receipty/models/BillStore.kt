package com.cldnpl.receipty.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** I conti recenti, in un file JSON sul telefono. Niente account, niente cloud. */
class BillStore(directory: File) {
    var bills by mutableStateOf<List<SavedBill>>(emptyList())
        private set
    private val file: File
    private val limit = 30

    init {
        directory.mkdirs()
        file = File(directory, "bills.json")
        runCatching { bills = json.decodeFromString<List<SavedBill>>(file.readText()) }
    }

    fun bill(id: UUID): SavedBill? = bills.firstOrNull { it.id == id }

    fun upsert(bill: SavedBill) {
        bills = (listOf(bill) + bills.filter { it.id != bill.id })
            .sortedByDescending { it.date }
            .take(limit)
        persist()
    }

    fun delete(id: UUID) {
        bills = bills.filter { it.id != id }
        persist()
    }

    /** I nomi dei conti passati, dal più recente, per aggiungerli al tavolo con un tocco. */
    fun recentNames(excluding: List<String>, limit: Int = 6): List<String> {
        val seen = excluding.map { it.lowercase() }.toMutableSet()
        val names = mutableListOf<String>()
        for (bill in bills) {
            for (line in bill.settlement.lines) {
                val key = line.person.name.lowercase()
                if (seen.contains(key)) continue
                seen.add(key)
                names.add(line.person.name)
                if (names.size == limit) return names
            }
        }
        return names
    }

    private fun persist() {
        runCatching {
            val tmp = File(file.parentFile, "bills.json.tmp")
            tmp.writeText(json.encodeToString(bills))
            tmp.renameTo(file)
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
