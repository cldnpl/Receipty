package com.cldnpl.receipty.app

import android.content.Context
import com.cldnpl.receipty.BuildConfig
import com.cldnpl.receipty.models.AppModel
import com.cldnpl.receipty.models.BillDraft
import com.cldnpl.receipty.models.BillItem
import com.cldnpl.receipty.models.BillSource
import com.cldnpl.receipty.models.BillStore
import com.cldnpl.receipty.models.Currency
import com.cldnpl.receipty.models.Money
import com.cldnpl.receipty.models.Person
import com.cldnpl.receipty.models.Route
import com.cldnpl.receipty.models.SavedBill
import com.cldnpl.receipty.models.Settlement
import java.io.File
import java.util.Calendar
import java.util.UUID

/**
 * Solo in Debug: `screen <nome>` all'avvio apre una schermata con i dati del mockup,
 * per confrontarla col Figma e per gli screenshot. I recenti finti vivono in una cartella temporanea.
 *
 *     adb shell am start -n com.cldnpl.whopays/com.cldnpl.receipty.MainActivity --es screen review
 */
object DemoSeed {
    var screen: String? = null
        set(value) {
            field = if (BuildConfig.DEBUG) value else null
        }

    fun makeStore(context: Context): BillStore? {
        val screen = screen ?: return null
        val store = BillStore(File(context.cacheDir, "demo-${UUID.randomUUID()}"))
        fun date(day: Int) = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, day, 21, 0, 0) }.timeInMillis
        fun bill(day: Int, names: List<String>, total: Int, source: BillSource): SavedBill {
            val people = names.map { Person(name = it) }
            val lines = people.map { Settlement.Line(it, total / people.size, 0) }
            return SavedBill(UUID.randomUUID(), date(day), source, 3, Settlement(total, lines, emptyList()))
        }
        if (screen == "people") {
            // I "Recenti" del mockup.
            store.upsert(bill(2, listOf("Anna", "Sara", "Paolo"), 6000, BillSource.equal))
        } else if (screen != "empty") {
            store.upsert(bill(12, listOf("Chris", "Mia", "Ben"), 4500, BillSource.equal))
            store.upsert(bill(18, listOf("Alice", "Tom", "Sam"), 8400, BillSource.equal))
            store.upsert(bill(24, listOf("Marco", "Sarah", "Leo"), 2230, BillSource.scan))
        }
        return store
    }

    fun apply(app: AppModel) {
        val screen = screen ?: return
        val names = listOf("Cla", "Marco", "Giulia", "Luca")

        fun mockupDraft(): BillDraft {
            val d = BillDraft(BillSource.scan, Currency("EUR"))
            d.items.addAll(
                listOf(
                    BillItem(name = "Margherita pizza", cents = 800),
                    BillItem(name = "Carbonara", cents = 1200),
                    BillItem(name = "Draft beer", cents = 500),
                    BillItem(name = "Still water", cents = 200),
                    BillItem(name = "Tiramis?", cents = 600, issue = BillItem.Issue.NAME),
                    BillItem(name = "Cover charge", cents = 700, issue = BillItem.Issue.PRICE),
                )
            )
            d.printedTotal = 4000
            names.forEach { d.addPerson(it) }
            return d
        }

        fun assigned(d: BillDraft, full: Boolean) {
            val p = d.people.map { it.id }
            d.items[4] = d.items[4].copy(name = "Tiramisu", issue = null)
            d.items[5] = d.items[5].copy(issue = null)
            d.items[0] = d.items[0].copy(assignees = setOf(p[0]))
            d.items[1] = d.items[1].copy(assignees = setOf(p[1]))
            d.items[2] = d.items[2].copy(assignees = setOf(p[0], p[2]))
            if (full) {
                d.items[3] = d.items[3].copy(assignees = setOf(p[3]))
                d.items[4] = d.items[4].copy(assignees = setOf(p[2]))
                d.items[5] = d.items[5].copy(assignees = p.toSet())
            }
        }

        when (screen) {
            "sheet" -> app.showingNewBill = true
            "scan" -> {
                app.draft = BillDraft(BillSource.scan, Currency("EUR"))
                app.setPath(listOf(Route.Scan))
            }
            "review" -> {
                app.draft = mockupDraft()
                app.setPath(listOf(Route.Scan, Route.Review))
            }
            "people" -> {
                app.draft = mockupDraft()
                app.setPath(listOf(Route.Scan, Route.Review, Route.People))
            }
            "assign" -> {
                val d = mockupDraft()
                assigned(d, full = false)
                app.draft = d
                app.setPath(listOf(Route.Scan, Route.Review, Route.People, Route.Assign))
            }
            "payments" -> {
                val d = mockupDraft()
                assigned(d, full = true)
                d.paidText[d.people[0].id] = "30"
                d.paidText[d.people[1].id] = "10"
                app.draft = d
                app.setPath(listOf(Route.Scan, Route.Review, Route.People, Route.Assign, Route.Payments))
            }
            "keyboard" -> {
                val d = BillDraft(BillSource.equal, Currency("EUR"))
                listOf("Elbek", "Claudia").forEach { d.addPerson(it) }
                app.draft = d
                app.setPath(listOf(Route.People, Route.Payments))
            }
            "manual" -> {
                val d = BillDraft(BillSource.equal, Currency("EUR"))
                names.forEach { d.addPerson(it) }
                d.equalTotalText = Money.editableFixed(7000, decimals = true)
                d.paidText[d.people[0].id] = "70"
                app.draft = d
                app.setPath(listOf(Route.People, Route.Payments))
            }
            "result" -> {
                val d = mockupDraft()
                assigned(d, full = true)
                d.paidText[d.people[0].id] = "30"
                d.paidText[d.people[1].id] = "10"
                app.draft = d
                app.setPath(listOf(Route.Scan, Route.Review, Route.People, Route.Assign, Route.Payments, Route.Result))
            }
            "change", "changeresult" -> {
                // Il caso del resto: conto da €52 in 4, Claudia mette 20 e Marco 40.
                val d = BillDraft(BillSource.equal, Currency("EUR"))
                listOf("Claudia", "Marco", "Olga", "Kekko").forEach { d.addPerson(it) }
                d.equalTotalText = "52"
                d.paidText[d.people[0].id] = "20"
                d.paidText[d.people[1].id] = "40"
                app.draft = d
                app.setPath(
                    if (screen == "change") listOf(Route.People, Route.Payments)
                    else listOf(Route.People, Route.Payments, Route.Result)
                )
            }
        }
    }
}
