package com.cldnpl.receipty.views

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.cldnpl.receipty.models.DiscountMode
import com.cldnpl.receipty.theme.fixedStyle
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cldnpl.receipty.localization.t
import com.cldnpl.receipty.models.AppModel
import com.cldnpl.receipty.models.BillDraft
import com.cldnpl.receipty.models.BillSource
import com.cldnpl.receipty.models.LocalCurrency
import com.cldnpl.receipty.models.Money
import com.cldnpl.receipty.models.Person
import com.cldnpl.receipty.models.Route
import com.cldnpl.receipty.models.format
import com.cldnpl.receipty.theme.AmountField
import com.cldnpl.receipty.theme.Avatar
import com.cldnpl.receipty.theme.BottomBar
import com.cldnpl.receipty.theme.CalculatorIcon
import com.cldnpl.receipty.theme.Face
import com.cldnpl.receipty.theme.FlowHeader
import com.cldnpl.receipty.theme.FlowScaffold
import com.cldnpl.receipty.theme.Haptics
import com.cldnpl.receipty.theme.Palette
import com.cldnpl.receipty.theme.PrimaryButton
import com.cldnpl.receipty.theme.StatusPill
import com.cldnpl.receipty.theme.Type
import com.cldnpl.receipty.theme.card
import com.cldnpl.receipty.theme.pressable
import com.cldnpl.receipty.theme.requestWhenReady
import com.cldnpl.receipty.theme.style
import com.cldnpl.receipty.theme.topFade
import java.util.UUID
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Il campo con il cursore: il totale, l'importo di una persona, o nessuno. */
private sealed interface Focus {
    data object Total : Focus
    data object Discount : Focus
    data class Payer(val id: UUID) : Focus
}

/**
 * "Chi ha pagato?": quanto ha messo ciascuno. Con lo scontrino il totale è già noto,
 * nella divisione in parti uguali si scrive qui in alto.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentsView(app: AppModel, draft: BillDraft) {
    val currency = LocalCurrency.current
    val decimals = currency.minorDigits > 0
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var focus by remember { mutableStateOf<Focus?>(null) }
    val totalFocus = remember { FocusRequester() }

    fun setFocus(new: Focus?) {
        val old = focus
        focus = new
        // "52" diventa "52.00" quando si esce dal campo del totale.
        if (old == Focus.Total && new != Focus.Total) {
            val cents = Money.parse(draft.equalTotalText)
            if (cents != null && cents > 0) draft.equalTotalText = Money.editableFixed(cents, decimals)
        }
    }

    fun done() {
        focusManager.clearFocus()
        keyboard?.hide()
    }

    LaunchedEffect(Unit) {
        if (!draft.source.isItemized && draft.subtotal == 0) {
            // Dopo l'animazione d'ingresso, come `onAppear` su iOS.
            delay(400)
            if (totalFocus.requestWhenReady { focus == Focus.Total }) keyboard?.show()
        }
    }

    // Se la tastiera si chiude col tasto indietro, il campo perde il fuoco come su iOS.
    val imeVisible = WindowInsets.isImeVisible
    var imeWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible) {
        if (imeWasVisible && !imeVisible) focusManager.clearFocus()
        imeWasVisible = imeVisible
    }

    val status = status(draft, currency)
    val instructions = t(if (draft.source.isItemized) "payments.instructions.itemized" else "payments.instructions.equal")
    val exampleName = draft.people.firstOrNull()?.name ?: t("payments.example.name")
    val example = t("payments.example", exampleName, exampleName)

    FlowScaffold(
        header = {
            // La spiegazione serve prima di scrivere: con la tastiera aperta si toglie e lascia spazio alle righe.
            FlowHeader(
                t("payments.title"),
                subtitle = if (focus == null) instructions else null,
                example = if (focus == null) example else null,
            )
        },
        bottomBar = {
            // Mentre si scrive la barra grande lascia il posto a una fascia bassa: tra tastiera,
            // pill e bottone intero resterebbe visibile una riga sola.
            AnimatedContent(focus == null, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }, label = "bar") { idle ->
                if (idle) {
                    BottomBar {
                        StatusPill(status.first, status.second, Modifier.padding(bottom = 13.dp))
                        PrimaryButton(
                            onClick = {
                                done()
                                app.path.add(Route.Result)
                            },
                            enabled = draft.canSettle,
                        ) { color ->
                            CalculatorIcon(if (draft.canSettle) Color.White else Palette.disabledText)
                            Spacer(Modifier.width(13.dp))
                            Text(t("payments.calculate"), style = Type.button, color = color)
                        }
                    }
                } else {
                    // Sopra la tastiera: a sinistra come sta andando il conto, a destra il tasto per chiudere.
                    BottomBar(topPadding = 12.dp, bottomPadding = 10.dp) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            StatusPill(status.first, status.second, Modifier.weight(1f, fill = false))
                            Spacer(Modifier.width(12.dp))
                            Box(
                                Modifier
                                    .height(37.dp)
                                    .pressable(0.94f) { done() }
                                    .background(Palette.inverseFill, CircleShape)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(t("common.done"), style = style(Face.semibold, 15.5f), color = Palette.inverseText)
                            }
                        }
                    }
                }
            }
        },
    ) { bottomInset ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
                .topFade(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 13.dp, bottom = bottomInset + 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "total") {
                TotalCard(
                    draft,
                    totalFocus,
                    onFocus = { focused -> if (focused) setFocus(Focus.Total) else if (focus == Focus.Total) setFocus(null) },
                    onDiscountFocus = { focused -> if (focused) setFocus(Focus.Discount) else if (focus == Focus.Discount) setFocus(null) },
                    onDone = ::done,
                    modifier = Modifier.padding(bottom = 4.5.dp),
                )
            }
            items(draft.people, key = { it.id }) { person ->
                val itemized = draft.source.isItemized
                PayerRow(
                    person = person,
                    consumed = if (itemized) draft.owed[person.id] else null,
                    text = draft.paidText[person.id] ?: "",
                    onTextChange = { draft.paidText[person.id] = it },
                    onFocus = { focused ->
                        if (focused) setFocus(Focus.Payer(person.id))
                        else if (focus == Focus.Payer(person.id)) setFocus(null)
                    },
                    onDone = ::done,
                    onEverything = {
                        Haptics.tap()
                        done()
                        draft.paidEverything(person)
                    },
                )
            }
        }
    }
}

private fun status(draft: BillDraft, currency: com.cldnpl.receipty.models.Currency): Pair<String, Boolean> {
    val total = draft.total
    val paid = draft.paidTotal
    if (total == 0) return t("payments.status.enterTotal") to false
    if (paid == total) return t("payments.status.ok") to true
    if (paid < total) return t("payments.status.missing", Money.format(total - paid, currency)) to false
    val change = paid - total
    // Più di €100 di resto e più del conto stesso: quasi sempre un importo scritto male.
    if (change > 10_000 && change > total) {
        return t("payments.status.changeHigh", Money.format(change, currency)) to false
    }
    return t("payments.status.change", Money.format(change, currency)) to true
}

@Composable
private fun TotalCard(
    draft: BillDraft,
    focusRequester: FocusRequester,
    onFocus: (Boolean) -> Unit,
    onDiscountFocus: (Boolean) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .card()
            .padding(horizontal = 21.dp, vertical = 19.5.dp)
            .animateContentSize()
    ) {
        // Il tocco sulla card mette il cursore nel totale: solo sulla parte alta, non sullo sconto.
        TotalSection(
            draft,
            focusRequester,
            onFocus,
            onDone,
            Modifier.pointerInput(draft.source) {
                detectTapGestures { if (!draft.source.isItemized) runCatching { focusRequester.requestFocus() } }
            },
        )
        DiscountSection(draft, onDiscountFocus, onDone)
    }
}

@Composable
private fun TotalSection(
    draft: BillDraft,
    focusRequester: FocusRequester,
    onFocus: (Boolean) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currency = LocalCurrency.current
    val decimals = currency.minorDigits > 0
    Column(modifier.fillMaxWidth()) {
        Text(t("payments.total"), style = style(Face.medium, 14f), color = Palette.raspberry)

        if (draft.source.isItemized) {
            Text(
                Money.format(draft.subtotal, currency),
                style = Type.bigAmount,
                color = Palette.ink,
                modifier = Modifier.padding(top = 5.dp),
            )
            Text(
                t(
                    "payments.source.items",
                    t(if (draft.source == BillSource.scan) "payments.source.scan" else "payments.source.typed"),
                    draft.items.size,
                ),
                style = style(Face.regular, 14f),
                color = Palette.gray,
                modifier = Modifier.padding(top = 6.dp),
            )
        } else {
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.5.dp)) {
                Text(
                    currency.symbol,
                    style = style(Face.extraBold, 44f, -0.02f),
                    color = Palette.pink,
                    modifier = Modifier.alignByBaseline(),
                )
                val valueStyle = style(Face.extraBold, 45.5f, -0.02f).copy(color = Palette.ink)
                val label = t("payments.total")
                BasicTextField(
                    value = draft.equalTotalText,
                    onValueChange = { draft.equalTotalText = Money.sanitizeInput(it, decimals) },
                    singleLine = true,
                    textStyle = valueStyle,
                    cursorBrush = SolidColor(Palette.pink),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onDone() }),
                    modifier = Modifier
                        .weight(1f)
                        .alignByBaseline()
                        .focusRequester(focusRequester)
                        .onFocusChanged { onFocus(it.isFocused) }
                        .semantics { contentDescription = label },
                    decorationBox = { inner ->
                        Box {
                            if (draft.equalTotalText.isEmpty()) {
                                Text(
                                    Money.editableFixed(0, decimals),
                                    style = valueStyle.copy(color = Palette.fieldPlaceholder.copy(alpha = 0.6f)),
                                    maxLines = 1,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
            val n = draft.people.size
            val split = if (draft.total <= 0 || n <= 0) {
                t("payments.split.noTotal", n)
            } else {
                t("payments.split", n, Money.format((draft.total.toDouble() / n).roundToInt(), currency))
            }
            AnimatedContent(split, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "split") {
                Text(it, style = style(Face.regular, 14f), color = Palette.gray, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** "Aggiungi uno sconto", poi: percentuale o importo, quanto, e quanto si paga davvero. */
@Composable
private fun DiscountSection(draft: BillDraft, onFocus: (Boolean) -> Unit, onDone: () -> Unit) {
    val currency = LocalCurrency.current
    val focusManager = LocalFocusManager.current
    val fieldFocus = remember { FocusRequester() }
    var fieldFocused by remember { mutableStateOf(false) }
    // Appena si tocca "Aggiungi uno sconto", il cursore va nel campo.
    var wantsFocus by remember { mutableStateOf(false) }
    LaunchedEffect(wantsFocus) {
        if (wantsFocus) {
            fieldFocus.requestWhenReady { fieldFocused }
            wantsFocus = false
        }
    }

    if (!draft.hasDiscount) {
        Row(
            Modifier
                .padding(top = 14.dp)
                .pressable(0.96f) {
                    draft.hasDiscount = true
                    wantsFocus = true
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.LocalOffer, null, tint = Palette.pinkText, modifier = Modifier.size(16.dp))
            Text(t("payments.discount.add"), style = Type.smallLink, color = Palette.pinkText)
        }
        return
    }

    Column {
        Box(
            Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(Palette.hairline)
        )
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(t("payments.discount"), style = style(Face.semibold, 15f), color = Palette.ink, modifier = Modifier.weight(1f))
            val removeLabel = t("payments.discount.remove")
            Box(
                Modifier
                    .size(32.dp)
                    .pressable(0.9f) {
                        focusManager.clearFocus()
                        draft.hasDiscount = false
                        draft.discountText = ""
                    }
                    .semantics { contentDescription = removeLabel },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, null, tint = Palette.muted, modifier = Modifier.size(17.dp))
            }
        }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DiscountModePicker(draft.discountMode) { draft.discountMode = it }
            DiscountField(
                draft.discountText,
                onTextChange = { draft.discountText = it },
                mode = draft.discountMode,
                focusRequester = fieldFocus,
                onFocusChanged = {
                    fieldFocused = it
                    onFocus(it)
                },
                onDone = onDone,
                modifier = Modifier.weight(1f),
            )
        }
        if (draft.discount > 0) {
            Text(
                t("payments.discount.summary", Money.format(draft.discount, currency), Money.format(draft.total, currency)),
                style = style(Face.semibold, 13.5f),
                color = Palette.raspberry,
                modifier = Modifier.padding(top = 9.dp),
            )
        }
    }
}

/** Percentuale o importo: due pillole, come le chip dell'app. */
@Composable
private fun DiscountModePicker(mode: DiscountMode, onSelect: (DiscountMode) -> Unit) {
    val currency = LocalCurrency.current
    Row(
        Modifier
            .background(Palette.pinkMist, CircleShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for ((value, text, label) in listOf(
            Triple(DiscountMode.PERCENT, "%", t("payments.discount.percent")),
            Triple(DiscountMode.AMOUNT, currency.symbol, t("common.amount")),
        )) {
            val on = mode == value
            val fill by animateColorAsState(if (on) Palette.pink else Color.Transparent, label = "mode")
            Box(
                Modifier
                    .size(44.dp, 34.dp)
                    .selectable(on, role = Role.RadioButton, interactionSource = null, indication = null) {
                        Haptics.select()
                        onSelect(value)
                    }
                    .background(fill, CircleShape)
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text,
                    style = fixedStyle(Face.bold, if (text.length > 2) 11f else 15f),
                    color = if (on) Color.White else Palette.raspberry,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/** Quanto è lo sconto: "10 %" oppure "€ 5". */
@Composable
private fun DiscountField(
    text: String,
    onTextChange: (String) -> Unit,
    mode: DiscountMode,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currency = LocalCurrency.current
    val shape = RoundedCornerShape(14.dp)
    val unitStyle = style(Face.semibold, 17f)
    val label = t("payments.discount")

    /** In percentuale si arriva al massimo a 100. */
    fun sanitize(new: String): String {
        val clean = Money.sanitizeInput(new, mode == DiscountMode.PERCENT || currency.minorDigits > 0)
        val value = Money.parse(clean)
        return if (mode == DiscountMode.PERCENT && value != null && value > 10_000) "100" else clean
    }

    Row(
        modifier
            .height(40.dp)
            .background(Palette.fieldFill, shape)
            .border(1.5.dp, Palette.fieldBorder, shape)
            .pointerInput(Unit) { detectTapGestures { runCatching { focusRequester.requestFocus() } } }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (mode == DiscountMode.AMOUNT) Text(currency.symbol, style = unitStyle, color = Palette.raspberry, maxLines = 1, softWrap = false)
        val valueStyle = Type.fieldValue.copy(color = Palette.ink, textAlign = TextAlign.End)
        BasicTextField(
            value = text,
            onValueChange = { onTextChange(sanitize(it)) },
            singleLine = true,
            textStyle = valueStyle,
            cursorBrush = SolidColor(Palette.pink),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .semantics { contentDescription = label },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (text.isEmpty()) Text("0", style = valueStyle.copy(color = Palette.fieldPlaceholder), maxLines = 1)
                    inner()
                }
            },
        )
        if (mode == DiscountMode.PERCENT) Text("%", style = unitStyle, color = Palette.raspberry, maxLines = 1)
    }
}

@Composable
private fun PayerRow(
    person: Person,
    consumed: Int?,
    text: String,
    onTextChange: (String) -> Unit,
    onFocus: (Boolean) -> Unit,
    onDone: () -> Unit,
    onEverything: () -> Unit,
) {
    val currency = LocalCurrency.current
    val allLabel = t("payments.paidEverything", person.name)
    Row(
        Modifier
            .fillMaxWidth()
            .height(if (consumed == null) 62.dp else 64.dp)
            .background(Palette.cardSoft, RoundedCornerShape(22.dp))
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(person, size = 38.dp)
        Column(
            Modifier
                .weight(1f)
                .padding(start = 9.5.dp, end = 6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(person.name, style = Type.personNameStrong, color = Palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (consumed != null) {
                Text(
                    t("payments.ordered", Money.format(consumed, currency)),
                    style = style(Face.medium, 12f, lineHeight = 14.3f),
                    color = Palette.gray,
                )
            }
        }
        Box(
            Modifier
                .height(35.dp)
                .pressable(0.92f, onClick = onEverything)
                .background(Palette.pinkSoft, CircleShape)
                .padding(horizontal = 11.5.dp)
                .semantics { contentDescription = allLabel },
            contentAlignment = Alignment.Center,
        ) {
            Text(t("payments.all"), style = style(Face.semibold, 14f), color = Palette.raspberry)
        }
        AmountField(
            text = text,
            onTextChange = onTextChange,
            modifier = Modifier.padding(start = 10.5.dp),
            onFocusChanged = onFocus,
            onDone = onDone,
        )
    }
}
