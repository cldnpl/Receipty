package com.cldnpl.receipty.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cldnpl.receipty.localization.t
import com.cldnpl.receipty.models.AppModel
import com.cldnpl.receipty.models.BillDraft
import com.cldnpl.receipty.models.DateText
import com.cldnpl.receipty.models.Money
import com.cldnpl.receipty.models.Settlement
import com.cldnpl.receipty.models.format
import com.cldnpl.receipty.theme.Avatar
import com.cldnpl.receipty.theme.BackButton
import com.cldnpl.receipty.theme.BottomBar
import com.cldnpl.receipty.theme.BottomSheet
import com.cldnpl.receipty.theme.Face
import com.cldnpl.receipty.theme.FlowScaffold
import com.cldnpl.receipty.theme.Haptics
import com.cldnpl.receipty.theme.Metrics
import com.cldnpl.receipty.theme.Palette
import com.cldnpl.receipty.theme.Type
import com.cldnpl.receipty.theme.card
import com.cldnpl.receipty.theme.pressable
import com.cldnpl.receipty.theme.softShadow
import com.cldnpl.receipty.theme.style
import com.cldnpl.receipty.theme.swiftSpring
import com.cldnpl.receipty.theme.topFade
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Calcola il risultato del conto in corso, lo salva tra i recenti e lo mostra. */
@Composable
fun DraftResultView(app: AppModel, draft: BillDraft) {
    // Il conto si fa una volta sola, quando la schermata nasce.
    val settlement = remember(draft) { draft.settlement() }
    LaunchedEffect(draft) { app.record(draft) }
    ResultView(settlement, draft.createdAt) { app.finish() }
}

/** "Siete a posto.": chi dà quanto a chi. La schermata per cui esiste l'app. */
@Composable
fun ResultView(settlement: Settlement, date: Long, onDone: () -> Unit) {
    val currency = settlement.currency
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var appeared by rememberSaveable { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    var showingBreakdown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!appeared) {
            Haptics.success()
            appeared = true
        }
    }

    val subtitle = run {
        val total = Money.format(settlement.total, currency)
        if (settlement.change > 0) {
            val change = Money.format(settlement.change, currency)
            when (val n = settlement.transfers.size) {
                0 -> t("result.subtitle.change.none", change, total)
                1 -> t("result.subtitle.change.one", change)
                else -> t("result.subtitle.change.many", change, n)
            }
        } else when (val n = settlement.transfers.size) {
            0 -> t("result.subtitle.none", total)
            1 -> t("result.subtitle.one", total)
            else -> t("result.subtitle.many", n, total)
        }
    }

    fun summary(): String {
        val lines = mutableListOf(
            t("summary.header", DateText.short(date)),
            t("summary.bill", Money.format(settlement.total, currency), settlement.lines.size),
        )
        if (settlement.discount > 0) lines.add(t("summary.discount", Money.format(settlement.discount, currency)))
        lines.add("")
        if (settlement.change > 0) {
            lines.add(t("summary.changeBack", Money.format(settlement.change, currency)))
            for (share in settlement.changeSplit) {
                lines.add(t("result.keepsChange", share.person.name, Money.format(share.cents, currency)))
            }
            lines.add("")
            if (settlement.transfers.isNotEmpty()) lines.add(t("summary.then"))
        }
        if (settlement.transfers.isEmpty() && settlement.change == 0) {
            lines.add(t("summary.allSquare"))
        } else {
            for (tr in settlement.transfers) {
                lines.add("${tr.from.name} → ${tr.to.name}: ${Money.format(tr.cents, currency)}")
            }
        }
        return lines.joinToString("\n")
    }

    FlowScaffold(
        header = { BackButton(modifier = Modifier.padding(top = 4.dp, start = 24.dp)) },
        bottomBar = {
            BottomBar {
                Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    Box(
                        Modifier
                            .width(153.dp)
                            .height(Metrics.buttonHeight)
                            .pressable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Receipty", summary()))
                                Haptics.success()
                                copied = true
                                scope.launch {
                                    delay(1800)
                                    copied = false
                                }
                            }
                            .softShadow(CircleShape, Palette.shadow, 0.1f, 14.dp, 6.dp)
                            .background(Palette.pillFill, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(copied, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "copy") {
                            Text(t(if (it) "result.copied" else "result.copy"), style = style(Face.medium, 16f), color = Palette.ink, maxLines = 1)
                        }
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(Metrics.buttonHeight)
                            .pressable(onClick = onDone)
                            .background(Palette.inverseFill, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(t("common.done"), style = style(Face.semibold, 17f), color = Palette.inverseText)
                    }
                }
            }
        },
        overlay = {
            BottomSheet(showingBreakdown, onDismiss = { showingBreakdown = false }) {
                BreakdownSheet(settlement)
            }
        },
    ) { bottomInset ->
        Column(
            Modifier
                .fillMaxSize()
                .topFade()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = bottomInset + 24.dp)
        ) {
            SuccessMark(appeared, Modifier.padding(top = 19.dp))

            Text(
                t("result.title"),
                style = Type.resultTitle,
                color = Palette.ink,
                modifier = Modifier
                    .padding(top = 5.dp, start = 1.dp)
                    .semantics { heading() },
            )
            Text(
                subtitle,
                style = Type.lead,
                color = Palette.inkSoft,
                modifier = Modifier
                    .padding(top = 8.dp, start = 1.dp)
                    .widthIn(max = 322.dp),
            )

            if (settlement.discount > 0) {
                Row(
                    Modifier
                        .padding(top = 12.dp)
                        .height(32.dp)
                        .background(Palette.pillFill, CircleShape)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Outlined.LocalOffer, null, tint = Palette.raspberry, modifier = Modifier.size(15.dp))
                    Text(t("result.discount", Money.format(settlement.discount, currency)), style = style(Face.semibold, 14f), color = Palette.raspberry)
                }
            }

            if (settlement.change > 0) {
                SectionLabel(t("result.changeBack", Money.format(settlement.change, currency)), Modifier.padding(top = 22.dp))
                Column(
                    Modifier.padding(top = 10.dp).offsetHorizontal(),
                    verticalArrangement = Arrangement.spacedBy(10.5.dp),
                ) {
                    settlement.changeSplit.forEachIndexed { index, share ->
                        Entrance(appeared, index) { ChangeCard(share, currency) }
                    }
                }
                if (settlement.transfers.isNotEmpty()) {
                    SectionLabel(t("result.then"), Modifier.padding(top = 20.dp))
                }
            }

            Column(
                Modifier.padding(top = if (settlement.change > 0) 10.dp else 24.dp).offsetHorizontal(),
                verticalArrangement = Arrangement.spacedBy(10.5.dp),
            ) {
                settlement.transfers.forEachIndexed { index, transfer ->
                    Entrance(appeared, index + settlement.changeSplit.size) { TransferCard(transfer, currency) }
                }
            }

            val linkAlpha by animateFloatAsState(if (appeared) 1f else 0f, tween(400, delayMillis = 500), label = "link")
            Box(Modifier.fillMaxWidth().padding(top = if (settlement.transfers.isEmpty()) 8.dp else 26.dp), contentAlignment = Alignment.Center) {
                Text(
                    t("result.breakdown"),
                    style = Type.smallLink,
                    color = Palette.raspberryDeep,
                    modifier = Modifier
                        .graphicsLayer { alpha = linkAlpha }
                        .pressable(0.96f) { showingBreakdown = true }
                        .padding(8.dp),
                )
            }
        }
    }
}

/** Le card sporgono di 4 punti dai margini, come nel mockup. */
private fun Modifier.offsetHorizontal(): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val extra = 8.dp.roundToPx()
        val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra, minWidth = constraints.minWidth + extra))
        layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
    }
)

/** Le card entrano una dopo l'altra, dal basso. */
@Composable
private fun Entrance(appeared: Boolean, index: Int, content: @Composable () -> Unit) {
    val progress = remember { Animatable(if (appeared) 1f else 0f) }
    LaunchedEffect(appeared) {
        if (appeared && progress.value < 1f) {
            delay(250L + index * 80L)
            progress.animateTo(1f, swiftSpring(0.5f, 0.8f))
        }
    }
    Box(
        Modifier.graphicsLayer {
            alpha = progress.value.coerceIn(0f, 1f)
            translationY = (1 - progress.value) * 18.dp.toPx()
        }
    ) { content() }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Type.section,
        color = Palette.raspberry,
        modifier = modifier
            .padding(start = 2.dp)
            .semantics { heading() },
    )
}

/** "Resto → Claudia": la parte del resto che una persona si tiene al tavolo. */
@Composable
private fun ChangeCard(share: Settlement.ChangeShare, currency: com.cldnpl.receipty.models.Currency) {
    val amount = Money.format(share.cents, currency)
    val label = t("result.keepsChange", share.person.name, amount)
    ResultCard(label) {
        Box(Modifier.size(38.dp).background(Palette.pinkSoft, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Payments, null, tint = Palette.pinkIcon, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f).padding(start = 11.dp, end = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.5.dp)) {
                Text(t("result.change"), style = Type.transferName, color = Palette.raspberry, maxLines = 1)
                Arrow()
                Text(share.person.name, style = Type.transferName, color = Palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(t("result.keepsIt", share.person.name), style = Type.transferSub, color = Palette.gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(amount, style = Type.transferAmount, color = Palette.ink, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun TransferCard(transfer: Settlement.Transfer, currency: com.cldnpl.receipty.models.Currency) {
    val amount = Money.format(transfer.cents, currency)
    val label = t("result.paysAmount", transfer.from.name, transfer.to.name, amount)
    ResultCard(label) {
        Avatar(transfer.from, size = 38.dp)
        Column(Modifier.weight(1f).padding(start = 11.dp, end = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.5.dp)) {
                Text(transfer.from.name, style = Type.transferName, color = Palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Arrow()
                Text(transfer.to.name, style = Type.transferName, color = Palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
            Text(
                t("result.pays", transfer.from.name, transfer.to.name),
                style = Type.transferSub,
                color = Palette.gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(amount, style = Type.transferAmount, color = Palette.ink, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun Arrow() {
    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Palette.arrow, modifier = Modifier.size(16.dp))
}

@Composable
private fun ResultCard(label: String, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(76.dp)
            .card(radius = 26.dp)
            .padding(start = 20.dp, end = 19.dp)
            .clearAndSetSemantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun SuccessMark(drawn: Boolean, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(if (drawn) 1f else 0.6f, swiftSpring(0.45f, 0.6f), label = "scale")
    val alpha by animateFloatAsState(if (drawn) 1f else 0f, swiftSpring(0.45f, 0.6f), label = "alpha")
    val trim by animateFloatAsState(if (drawn) 1f else 0f, tween(350, delayMillis = 200), label = "trim")
    val success = Palette.success
    Box(
        modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha.coerceIn(0f, 1f)
            }
            .softShadow(CircleShape, success, 0.3f, 14.dp, 6.dp)
            .background(success, CircleShape)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(22.dp, 16.dp)) {
            val path = Path().apply {
                moveTo(0f, size.height / 2 + size.height * 0.05f)
                lineTo(size.width * 0.36f, size.height)
                lineTo(size.width, 0f)
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val out = Path()
            measure.getSegment(0f, measure.length * trim, out, true)
            drawPath(out, Color.White, style = Stroke(3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** "Come ci siamo arrivati": per ciascuno quanto doveva, quanto ha pagato e il saldo. */
@Composable
private fun BreakdownSheet(settlement: Settlement) {
    val currency = settlement.currency
    Column(Modifier.padding(horizontal = 22.dp).heightIn(max = 640.dp)) {
        Text(t("result.breakdown"), style = Type.sheetTitle, color = Palette.ink, modifier = Modifier.padding(top = 22.dp))
        Text(
            t("breakdown.subtitle"),
            style = style(Face.regular, 15f, lineHeight = 21f),
            color = Palette.inkSoft,
            modifier = Modifier.padding(top = 6.dp),
        )
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(settlement.lines, key = { it.id }) { line ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .background(Palette.card, RoundedCornerShape(22.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Avatar(line.person, size = 38.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(line.person.name, style = Type.personNameStrong, color = Palette.ink, maxLines = 1)
                        val small = style(Face.medium, 12.5f)
                        Text(
                            t("breakdown.line", Money.format(line.owed, currency), Money.format(line.paid, currency)),
                            style = small, color = Palette.gray, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        if (line.changeKept > 0) {
                            Text(
                                t("breakdown.kept", Money.format(line.changeKept, currency)),
                                style = small, color = Palette.gray, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        val balance = line.balance
                        Text(
                            (if (balance > 0) "+" else "") + Money.format(balance, currency),
                            style = style(Face.bold, 17f),
                            color = when {
                                balance > 0 -> Palette.successText
                                balance < 0 -> Palette.raspberry
                                else -> Palette.gray
                            },
                        )
                        Text(
                            t(
                                when {
                                    balance > 0 -> "breakdown.getsBack"
                                    balance < 0 -> "breakdown.owes"
                                    else -> "breakdown.even"
                                }
                            ),
                            style = style(Face.medium, 12f),
                            color = Palette.gray,
                        )
                    }
                }
            }
        }
    }
}
