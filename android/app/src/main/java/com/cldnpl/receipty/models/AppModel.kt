package com.cldnpl.receipty.models

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cldnpl.receipty.localization.t
import java.util.UUID

sealed interface Route {
    data object Scan : Route
    data object Review : Route
    data object People : Route
    data object Assign : Route
    data object Payments : Route
    data object Result : Route
    data class Saved(val id: UUID) : Route
}

class AppModel(val store: BillStore) {
    val path = mutableStateListOf<Route>()
    var draft by mutableStateOf<BillDraft?>(null)
    var showingNewBill by mutableStateOf(false)

    fun setPath(routes: List<Route>) {
        path.clear()
        path.addAll(routes)
    }

    fun startScan() {
        draft = BillDraft(BillSource.scan)
        showingNewBill = false
        setPath(listOf(Route.Scan))
    }

    fun startEqualSplit() {
        draft = BillDraft(BillSource.equal)
        showingNewBill = false
        setPath(listOf(Route.People))
    }

    /** Dallo scanner: niente foto, le voci si scrivono a mano. */
    fun typeItemsInstead() {
        draft?.source = BillSource.typed
        path.add(Route.Review)
    }

    /** Salva il conto tra i recenti (sovrascrivendo se si è tornati indietro a correggere). */
    fun record(draft: BillDraft) {
        store.upsert(
            SavedBill(
                id = draft.id,
                date = draft.createdAt,
                source = draft.source,
                itemCount = draft.items.size,
                settlement = draft.settlement(),
            )
        )
    }

    fun finish() {
        path.clear()
        draft = null
    }
}

enum class Appearance(val raw: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");

    val title: String get() = t("appearance.$raw")
}

/** Le preferenze dell'app (`@AppStorage` su iOS): salvate subito, osservabili da Compose. */
object AppSettings {
    private var prefs: SharedPreferences? = null

    var appearance by mutableStateOf(Appearance.SYSTEM)
        private set
    var didOnboard by mutableStateOf(false)
        private set
    var currencyCode by mutableStateOf<String?>(null)
        private set

    fun init(context: Context) {
        val p = context.getSharedPreferences("receipty", Context.MODE_PRIVATE)
        prefs = p
        appearance = Appearance.entries.firstOrNull { it.raw == p.getString("appearance", null) } ?: Appearance.SYSTEM
        didOnboard = p.getBoolean("didOnboard", false)
        currencyCode = p.getString(Currency.DEFAULTS_KEY, null)
    }

    fun string(key: String): String? = prefs?.getString(key, null)
    fun putString(key: String, value: String?) {
        prefs?.edit()?.apply { if (value == null) remove(key) else putString(key, value) }?.apply()
    }

    fun updateAppearance(value: Appearance) {
        appearance = value
        putString("appearance", value.raw)
    }

    fun updateDidOnboard(value: Boolean) {
        didOnboard = value
        prefs?.edit()?.putBoolean("didOnboard", value)?.apply()
    }

    fun updateCurrency(code: String) {
        currencyCode = code
        putString(Currency.DEFAULTS_KEY, code)
    }
}
