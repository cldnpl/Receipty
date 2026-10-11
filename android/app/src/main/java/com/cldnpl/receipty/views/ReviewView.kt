package com.cldnpl.receipty.views

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cldnpl.receipty.localization.t
import com.cldnpl.receipty.models.AppModel
import com.cldnpl.receipty.models.BillDraft
import com.cldnpl.receipty.models.BillItem
import com.cldnpl.receipty.models.BillSource
import com.cldnpl.receipty.models.LocalCurrency
import com.cldnpl.receipty.models.Money
import com.cldnpl.receipty.models.Route
import com.cldnpl.receipty.models.format
import com.cldnpl.receipty.theme.BottomBar
import com.cldnpl.receipty.theme.BottomSheet
import com.cldnpl.receipty.theme.Caption
import com.cldnpl.receipty.theme.Face
import com.cldnpl.receipty.theme.FlowHeader
import com.cldnpl.receipty.theme.FlowScaffold
import com.cldnpl.receipty.theme.Haptics
import com.cldnpl.receipty.theme.Metrics
import com.cldnpl.receipty.theme.Palette
import com.cldnpl.receipty.theme.PrimaryButton
import com.cldnpl.receipty.theme.Type
import com.cldnpl.receipty.theme.card
import com.cldnpl.receipty.theme.pressable
import com.cldnpl.receipty.theme.requestWhenReady
import com.cldnpl.receipty.theme.softShadow
import com.cldnpl.receipty.theme.style
import com.cldnpl.receipty.theme.topFade

/** Chi si sta modificando: una voce esistente, o una nuova (`item` nullo). */
private class EditorTarget(val item: BillItem?)

/** "È tutto giusto?": le voci lette dallo scontrino, da confermare prima di andare avanti. */
@Composable
fun ReviewView(app: AppModel, draft: BillDraft) {
    val currency = LocalCurrency.current
    val typed = draft.source == BillSource.typed
    var editing by remember { mutableStateOf<EditorTarget?>(null) }
    var lastEditing by remember { mutableStateOf<EditorTarget?>(null) }
    if (editing != null) lastEditing = editing

    val caption = when {
        draft.items.isEmpty() -> t("review.caption.empty")
        draft.itemsToCheck == 0 -> if (draft.items.size == 1) t("review.caption.one") else t("review.caption.many", draft.items.size)
        draft.itemsToCheck == 1 -> t("review.caption.check.one")
        else -> t("review.caption.check.many", draft.itemsToCheck)
    }

    FlowScaffold(
        header = {
            FlowHeader(
                t(if (typed) "review.title.typed" else "review.title.scan"),
                subtitle = t(if (typed) "review.subtitle.typed" else "review.subtitle.scan"),
            )
        },
        bottomBar = {
            BottomBar {
                PrimaryButton(
                    t("common.continue"),
                    onClick = { app.path.add(Route.People) },
                    enabled = draft.items.isNotEmpty() && draft.itemsToCheck == 0,
                )
                Caption(caption, Modifier.padding(top = 13.dp))
            }
        },
        overlay = {
            BottomSheet(editing != null, onDismiss = { editing = null }) {
                val target = lastEditing
                if (target != null) {
                    ItemEditor(
                        original = target.item,
                        onSave = { saved ->
                            val i = draft.items.indexOfFirst { it.id == saved.id }
                            if (i >= 0) draft.items[i] = saved else draft.items.add(saved)
                        },
                        onDelete = { id -> draft.items.removeAll { it.id == id } },
                        onClose = { editing = null },
                    )
                }
            }
        },
    ) { bottomInset ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
                .topFade(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 15.5.dp, bottom = bottomInset + 12.dp),
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .card()
                        .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 18.dp)
                        .animateContentSize()
                ) {
                    for (item in draft.items) {
                        androidx.compose.runtime.key(item.id) {
                            ItemRow(item, Modifier.pressable(0.985f) { editing = EditorTarget(item) })
                        }
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .pressable(0.98f) { editing = EditorTarget(null) }
                            .padding(horizontal = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = Palette.pinkText, modifier = Modifier.size(21.dp))
                        Text(t("review.addItem"), style = style(Face.semibold, 16f), color = Palette.pinkText)
                    }

                    Box(
                        Modifier
                            .padding(horizontal = 12.dp)
                            .padding(top = 3.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Palette.hairline)
                    )

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 13.dp)
                            .padding(top = 15.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            t(if (typed) "review.total" else "review.total.recognized"),
                            style = Type.totalLabel,
                            color = Palette.gray,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            Money.format(draft.itemsTotal, currency),
                            style = Type.totalValue,
                            color = Palette.ink,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }

                    val printed = draft.printedTotal
                    if (printed != null && printed != draft.itemsTotal) {
                        Row(
                            Modifier
                                .padding(horizontal = 13.dp)
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Outlined.ErrorOutline, null, tint = Palette.warning, modifier = Modifier.size(15.dp))
                            Text(t("review.printedTotal", Money.format(printed, currency)), style = Type.rowWarning, color = Palette.warning)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: BillItem, modifier: Modifier = Modifier) {
    val currency = LocalCurrency.current
    val hint = t("review.editHint")
    Row(
        modifier
            .padding(vertical = if (item.issue == null) 0.dp else 2.dp)
            .fillMaxWidth()
            .heightIn(min = if (item.issue == null) 49.5.dp else 63.dp)
            .background(if (item.issue == null) Color.Transparent else Palette.flag, RoundedCornerShape(18.dp))
            .padding(horizontal = 13.dp)
            .semantics(mergeDescendants = true) { onClick(hint) { false } },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            val gray = Palette.gray
            val ink = Palette.ink
            Text(
                buildAnnotatedString {
                    if (item.quantity > 1) withStyle(SpanStyle(color = gray)) { append("${item.quantity}× ") }
                    withStyle(SpanStyle(color = ink)) { append(item.name) }
                },
                style = Type.rowName,
                maxLines = 2,
            )
            val issue = item.issue
            if (issue != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Outlined.ErrorOutline, null, tint = Palette.warning, modifier = Modifier.size(15.dp))
                    Text(
                        t(if (issue == BillItem.Issue.NAME) "review.issue.name" else "review.issue.price"),
                        style = Type.rowWarning,
                        color = Palette.warning,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(Money.format(item.cents, currency), style = Type.rowPrice, color = Palette.ink)
    }
}

/** Correggere o aggiungere una voce: nome, quantità, prezzo. */
@Composable
private fun ItemEditor(
    original: BillItem?,
    onSave: (BillItem) -> Unit,
    onDelete: (java.util.UUID) -> Unit,
    onClose: () -> Unit,
) {
    val currency = LocalCurrency.current
    val decimals = currency.minorDigits > 0
    // Il cursore parte in fondo al testo, come su iOS.
    var nameField by remember(original) { mutableStateOf((original?.name ?: "").let { TextFieldValue(it, TextRange(it.length)) }) }
    val name = nameField.text
    var quantity by remember(original) { mutableIntStateOf(original?.quantity ?: 1) }
    var priceField by remember(original) {
        mutableStateOf((original?.let { Money.editable(it.cents) } ?: "").let { TextFieldValue(it, TextRange(it.length)) })
    }
    val price = priceField.text
    val nameFocus = remember { FocusRequester() }
    var focusedField by remember { mutableStateOf<String?>(null) }
    val priceFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val cents = Money.parse(price)
    val canSave = name.isNotBlank() && cents != null

    LaunchedEffect(original) {
        val target = when {
            original == null -> nameFocus
            original.issue == BillItem.Issue.PRICE -> priceFocus
            original.issue == BillItem.Issue.NAME -> nameFocus
            else -> null
        }
        if (target != null && target.requestWhenReady { focusedField != null }) keyboard?.show()
    }

    fun close() {
        keyboard?.hide()
        onClose()
    }

    val fieldShape = CircleShape
    val fieldModifier = Modifier
        .height(54.dp)
        .softShadow(fieldShape, Palette.shadow, 0.06f, 10.dp, 4.dp)
        .background(Palette.card, fieldShape)

    Column(Modifier.padding(horizontal = 22.dp)) {
        Text(
            t(if (original == null) "item.new" else "item.edit"),
            style = Type.sheetTitle,
            color = Palette.ink,
            modifier = Modifier.padding(top = 20.dp),
        )

        val issue = original?.issue
        if (issue != null) {
            Row(
                Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Outlined.ErrorOutline, null, tint = Palette.warning, modifier = Modifier.size(15.dp))
                Text(
                    t(if (issue == BillItem.Issue.NAME) "item.issue.name" else "item.issue.price"),
                    style = Type.rowWarning,
                    color = Palette.warning,
                )
            }
        }

        EditorLabel(t("item.name"), Modifier.padding(top = 20.dp))
        val nameStyle = style(Face.medium, 17f).copy(color = Palette.ink)
        BasicTextField(
            value = nameField,
            onValueChange = { nameField = it },
            singleLine = true,
            textStyle = nameStyle,
            cursorBrush = SolidColor(Palette.pink),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { priceFocus.requestFocus() }),
            modifier = fieldModifier
                .fillMaxWidth()
                .focusRequester(nameFocus)
                .onFocusChanged { if (it.isFocused) focusedField = "name" else if (focusedField == "name") focusedField = null },
            decorationBox = { inner ->
                Box(Modifier.padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) {
                    if (name.isEmpty()) Text(t("item.name.placeholder"), style = nameStyle.copy(color = Palette.placeholder))
                    inner()
                }
            },
        )

        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                EditorLabel(t("item.quantity"))
                Row(fieldModifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    StepperButton(Icons.Rounded.Remove, enabled = quantity > 1) { quantity = maxOf(1, quantity - 1) }
                    AnimatedContent(quantity, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "qty") {
                        Text("$it", style = style(Face.bold, 18f), color = Palette.ink, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 30.dp))
                    }
                    StepperButton(Icons.Rounded.Add) { quantity = minOf(99, quantity + 1) }
                }
            }
            Column(Modifier.weight(1f)) {
                EditorLabel(t("item.lineTotal"))
                Row(
                    fieldModifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(currency.symbol, style = style(Face.semibold, 18f), color = Palette.raspberry, maxLines = 1)
                    val priceStyle = style(Face.bold, 19f).copy(color = Palette.ink)
                    BasicTextField(
                        value = priceField,
                        onValueChange = { new ->
                            val negative = new.text.startsWith("-")
                            val clean = (if (negative) "-" else "") + Money.sanitizeInput(new.text, decimals)
                            priceField = if (clean == new.text) new else TextFieldValue(clean, TextRange(clean.length))
                        },
                        singleLine = true,
                        textStyle = priceStyle,
                        cursorBrush = SolidColor(Palette.pink),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(priceFocus)
                            .onFocusChanged { if (it.isFocused) focusedField = "price" else if (focusedField == "price") focusedField = null },
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (price.isEmpty()) Text(Money.editableFixed(0, decimals), style = priceStyle.copy(color = Palette.fieldPlaceholder))
                                inner()
                            }
                        },
                    )
                }
            }
        }

        if (quantity > 1 && cents != null) {
            Text(
                t("common.each", Money.format(cents / quantity, currency)),
                style = Type.micro,
                color = Palette.gray,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp, end = 20.dp),
            )
        }

        Spacer(Modifier.height(44.dp))

        Row(Modifier.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            if (original != null) {
                Box(
                    Modifier
                        .width(130.dp)
                        .height(Metrics.buttonHeight)
                        .pressable {
                            onDelete(original.id)
                            close()
                        }
                        .softShadow(CircleShape, Palette.shadow, 0.08f, 12.dp, 5.dp)
                        .background(Palette.pillFill, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(t("common.delete"), style = style(Face.semibold, 17f), color = Palette.warning)
                }
            }
            PrimaryButton(
                t(if (original == null) "common.add" else "common.confirm"),
                enabled = canSave,
                modifier = Modifier.weight(1f),
                onClick = {
                    val c = cents ?: return@PrimaryButton
                    val item = (original ?: BillItem(name = "", cents = 0)).copy(
                        name = name.trim(),
                        quantity = quantity,
                        cents = c,
                        issue = null,
                    )
                    Haptics.tap()
                    onSave(item)
                    close()
                },
            )
        }
    }
}

@Composable
private fun EditorLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = style(Face.semibold, 13f),
        color = Palette.raspberry,
        modifier = modifier.padding(start = 6.dp, bottom = 8.dp),
    )
}

@Composable
private fun StepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean = true, size: Dp = 42.dp, action: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .pressable(0.9f, enabled = enabled) {
                Haptics.select()
                action()
            }
            .background(Palette.pinkMist, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Palette.pink.copy(alpha = if (enabled) 1f else 0.4f), modifier = Modifier.size(19.dp))
    }
}
