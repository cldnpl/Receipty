package com.cldnpl.receipty.views

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cldnpl.receipty.localization.t
import com.cldnpl.receipty.models.AppModel
import com.cldnpl.receipty.models.BillSource
import com.cldnpl.receipty.models.DateText
import com.cldnpl.receipty.models.Money
import com.cldnpl.receipty.models.Route
import com.cldnpl.receipty.models.SavedBill
import com.cldnpl.receipty.models.format
import com.cldnpl.receipty.theme.CircleIconButton
import com.cldnpl.receipty.theme.DivideIcon
import com.cldnpl.receipty.theme.Haptics
import com.cldnpl.receipty.theme.IconBadge
import com.cldnpl.receipty.theme.Metrics
import com.cldnpl.receipty.theme.Palette
import com.cldnpl.receipty.theme.SwipeToDelete
import com.cldnpl.receipty.theme.Type
import com.cldnpl.receipty.theme.Wordmark
import com.cldnpl.receipty.theme.card
import com.cldnpl.receipty.theme.dashedBorder
import com.cldnpl.receipty.theme.pressable
import com.cldnpl.receipty.theme.topFade

@Composable
fun HomeView(app: AppModel) {
    val bills = app.store.bills

    fun delete(bill: SavedBill) {
        Haptics.tap()
        app.store.delete(bill.id)
    }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .topFade(10.dp),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 24.dp),
    ) {
        item(key = "header") {
            Column {
                Row(Modifier.padding(start = 6.dp, end = 5.dp), verticalAlignment = Alignment.Top) {
                    Wordmark(Modifier.semantics { heading() })
                    Spacer(Modifier.weight(1f).widthIn(min = 12.dp))
                    CircleIconButton(
                        Icons.Rounded.Add,
                        contentDescription = t("newBill.title"),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .coachTarget(CoachTarget.PLUS_BUTTON),
                    ) { app.showingNewBill = true }
                }
                Text(
                    t("home.subtitle"),
                    style = Type.lead,
                    color = Palette.inkSoft,
                    modifier = Modifier
                        .padding(top = 3.5.dp, start = 6.dp)
                        .widthIn(max = 290.dp),
                )
            }
        }

        if (bills.isEmpty()) {
            item(key = "empty") {
                EmptyRecents(Modifier.padding(top = 44.dp).animateItem())
            }
        } else {
            item(key = "recent") {
                Text(
                    t("home.recent"),
                    style = Type.section,
                    color = Palette.raspberry,
                    modifier = Modifier
                        .padding(top = 41.5.dp, start = 8.dp, bottom = 14.dp)
                        .semantics { heading() }
                        .animateItem(),
                )
            }
            items(bills, key = { it.id }) { bill ->
                Box(Modifier.animateItem().padding(bottom = 16.dp)) {
                    SwipeToDelete(height = 99.dp, onDelete = { delete(bill) }) {
                        BillCard(bill, onOpen = { app.path.add(Route.Saved(bill.id)) }, onDelete = { delete(bill) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BillCard(bill: SavedBill, onOpen: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val amount = Money.format(bill.settlement.total, bill.settlement.currency)
    val date = DateText.short(bill.date)
    val label = "$date, ${bill.names}, $amount, ${t("common.settled")}"
    val deleteLabel = t("common.delete")
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .height(99.dp)
                .pressable(0.98f, onLongClick = { Haptics.tap(); menu = true }, onClick = onOpen)
                .card()
                .padding(horizontal = 22.dp)
                .clearAndSetSemantics {
                    contentDescription = label
                    customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            IconBadge(if (bill.source == BillSource.equal) DivideIcon else Icons.Outlined.Receipt)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.5.dp)) {
                Text(date, style = Type.cardTitle, color = Palette.ink, maxLines = 1)
                Text(bill.names, style = Type.cardSub, color = Palette.gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CheckCircle, null, tint = Palette.check, modifier = Modifier.size(23.dp))
                Text(amount, style = Type.cardMeta, color = Palette.gray, maxLines = 1)
            }
        }
        DropdownMenu(menu, onDismissRequest = { menu = false }, containerColor = Palette.card) {
            DropdownMenuItem(
                text = { Text(t("common.delete"), color = Palette.warning, style = Type.rowName) },
                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = Palette.warning) },
                onClick = {
                    menu = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun EmptyRecents(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Metrics.cardRadius)
    Column(
        modifier
            .fillMaxWidth()
            .background(Palette.cardGhost, shape)
            .dashedBorder(Palette.dashed, radius = Metrics.cardRadius, dash = 4f, gap = 4f)
            .padding(vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconBadge(Icons.Outlined.Receipt, size = 54.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(t("home.empty.title"), style = Type.cardTitle, color = Palette.ink)
            Text(t("home.empty.text"), style = Type.cardSub, color = Palette.gray, textAlign = TextAlign.Center)
        }
    }
}
