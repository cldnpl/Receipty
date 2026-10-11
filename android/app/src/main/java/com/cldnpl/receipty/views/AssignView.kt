package com.cldnpl.receipty.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cldnpl.receipty.localization.t
import com.cldnpl.receipty.models.AppModel
import com.cldnpl.receipty.models.BillDraft
import com.cldnpl.receipty.models.BillItem
import com.cldnpl.receipty.models.LocalCurrency
import com.cldnpl.receipty.models.Money
import com.cldnpl.receipty.models.Person
import com.cldnpl.receipty.models.Route
import com.cldnpl.receipty.models.format
import com.cldnpl.receipty.models.formatCompact
import com.cldnpl.receipty.theme.Avatar
import com.cldnpl.receipty.theme.BottomBar
import com.cldnpl.receipty.theme.Caption
import com.cldnpl.receipty.theme.Face
import com.cldnpl.receipty.theme.FlowHeader
import com.cldnpl.receipty.theme.FlowScaffold
import com.cldnpl.receipty.theme.Haptics
import com.cldnpl.receipty.theme.Palette
import com.cldnpl.receipty.theme.PrimaryButton
import com.cldnpl.receipty.theme.Type
import com.cldnpl.receipty.theme.card
import com.cldnpl.receipty.theme.dashedBorder
import com.cldnpl.receipty.theme.pressable
import com.cldnpl.receipty.theme.style
import com.cldnpl.receipty.theme.topFade
import kotlin.math.roundToInt

/** "Chi ha ordinato cosa?": per ogni voce si toccano i nomi. Più nomi = prezzo diviso. */
@Composable
fun AssignView(app: AppModel, draft: BillDraft) {
    val caption = when (val n = draft.unassignedCount) {
        0 -> t("assign.caption.ok")
        1 -> t("assign.caption.one")
        else -> t("assign.caption.many", n)
    }

    FlowScaffold(
        header = { FlowHeader(t("assign.title"), subtitle = t("assign.subtitle")) },
        bottomBar = {
            BottomBar {
                RunningTotals(draft, Modifier.padding(bottom = 14.dp))
                PrimaryButton(
                    t("people.whoPaid"),
                    enabled = draft.unassignedCount == 0,
                    onClick = { app.path.add(Route.Payments) },
                )
                Caption(caption, Modifier.padding(top = 13.dp))
            }
        },
    ) { bottomInset ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
                .topFade(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = bottomInset + 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(draft.items, key = { it.id }) { item ->
                AssignCard(item, draft, Modifier.animateItem())
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssignCard(item: BillItem, draft: BillDraft, modifier: Modifier = Modifier) {
    val currency = LocalCurrency.current
    val assigned = draft.people.filter { item.assignees.contains(it.id) }
    val everyone = draft.people.isNotEmpty() && assigned.size == draft.people.size

    /** "2 × Birra" diventa due birre separate, da dare a persone diverse. */
    fun split() {
        val i = draft.items.indexOfFirst { it.id == item.id }
        if (i < 0 || item.quantity <= 1) return
        val n = item.quantity
        val pieces = (0 until n).map { k ->
            val cents = item.cents / n + (if (k < item.cents % n) 1 else 0)
            BillItem(name = item.name, quantity = 1, cents = cents, assignees = item.assignees)
        }
        draft.items.removeAt(i)
        draft.items.addAll(i, pieces)
    }

    Column(
        modifier
            .fillMaxWidth()
            .card()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 17.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, style = Type.itemTitle, color = Palette.ink)
                if (item.quantity > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "${item.quantity} × ${Money.format(item.cents / item.quantity, currency)}",
                            style = Type.perHead,
                            color = Palette.gray,
                        )
                        Text(
                            t("assign.split"),
                            style = Type.perHead,
                            color = Palette.pinkText,
                            modifier = Modifier.pressable(0.94f) { split() },
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(Money.format(item.cents, currency), style = Type.itemTitle, color = Palette.ink)
                AnimatedVisibility(
                    assigned.size > 1,
                    enter = fadeIn() + slideInVertically { -it / 2 },
                    exit = fadeOut() + slideOutVertically { -it / 2 },
                ) {
                    val each = (item.cents.toDouble() / maxOf(1, assigned.size)).roundToInt()
                    Text(t("common.each", Money.format(each, currency)), style = Type.perHead, color = Palette.pinkText)
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            for (person in draft.people) {
                val on = item.assignees.contains(person.id)
                PersonChip(
                    person,
                    on,
                    Modifier
                        .pressable(0.93f) {
                            Haptics.select()
                            draft.toggle(person, item.id)
                        }
                        .semantics { selected = on },
                )
            }
            val fill by animateColorAsState(if (everyone) Palette.pink else Color.Transparent, label = "all")
            Box(
                Modifier
                    .height(33.5.dp)
                    .pressable(0.93f) {
                        Haptics.select()
                        draft.toggleEveryone(item.id)
                    }
                    .background(fill, CircleShape)
                    .then(if (everyone) Modifier else Modifier.dashedBorder(Palette.dashedSoft))
                    .padding(horizontal = 14.dp)
                    .semantics { selected = everyone },
                contentAlignment = Alignment.Center,
            ) {
                Text(t("assign.all"), style = Type.chip, color = if (everyone) Color.White else Palette.chipText)
            }
        }
    }
}

@Composable
private fun PersonChip(person: Person, selected: Boolean, modifier: Modifier = Modifier) {
    val fill by animateColorAsState(if (selected) Palette.pink else Palette.pinkMist, label = "chip")
    Row(
        modifier
            .height(33.5.dp)
            .background(fill, CircleShape)
            .padding(start = 6.dp, end = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Avatar(person, size = 22.dp, selected = selected)
        Text(person.name, style = Type.chip, color = if (selected) Color.White else Palette.ink, maxLines = 1)
    }
}

/** Quanto sta spendendo ciascuno, mentre si assegnano le voci. */
@Composable
private fun RunningTotals(draft: BillDraft, modifier: Modifier = Modifier) {
    val currency = LocalCurrency.current
    val owed = draft.owed
    // La fila scorre da bordo a bordo dello schermo, oltre i margini della fascia.
    Row(
        modifier
            .layout { measurable, constraints ->
                val extra = 48.dp.roundToPx()
                val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra, minWidth = constraints.maxWidth + extra))
                layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
            }
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        for (person in draft.people) {
            Row(Modifier.height(32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Avatar(person, size = 22.dp)
                Text(Money.formatCompact(owed[person.id] ?: 0, currency), style = style(Face.semibold, 15f), color = Palette.ink)
            }
        }
    }
}
