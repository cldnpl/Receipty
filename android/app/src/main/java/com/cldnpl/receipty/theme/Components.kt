package com.cldnpl.receipty.theme

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cldnpl.receipty.localization.t
import com.cldnpl.receipty.models.LocalCurrency
import com.cldnpl.receipty.models.Money
import com.cldnpl.receipty.models.Person
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Torna indietro di una schermata: lo fornisce RootView a ogni destinazione. */
val LocalBack = staticCompositionLocalOf<() -> Unit> { {} }

/** L'altezza della finestra in pixel: serve per ritagliare la fetta giusta del gradiente. */
val LocalScreenHeight = staticCompositionLocalOf { 2000f }

// Bottoni

/** Scala leggermente al tocco: per card e pulsanti secondari. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressable(
    scale: Float = 0.97f,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) scale else 1f, swiftSpring(0.25f, 0.7f), label = "press")
    return this
        .graphicsLayer { scaleX = s; scaleY = s }
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}

/** Il bottone rosa a tutta larghezza in fondo alle schermate. */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.(color: Color) -> Unit,
) {
    val fill by animateColorAsState(if (enabled) Palette.pink else Palette.disabledFill, tween(200), label = "fill")
    val text by animateColorAsState(if (enabled) Color.White else Palette.disabledText, tween(200), label = "text")
    Box(
        modifier
            .fillMaxWidth()
            .height(Metrics.buttonHeight)
            .pressable(enabled = enabled, onClick = onClick)
            .background(fill, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            content(text)
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    PrimaryButton(onClick, modifier, enabled) { color ->
        Text(text, style = Type.button, color = color, maxLines = 1)
    }
}

@Composable
fun BackButton(action: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val back = LocalBack.current
    val label = t("common.back")
    Box(
        modifier
            .size(Metrics.backSize)
            .pressable(0.92f) { (action ?: back)() }
            .softShadow(CircleShape, Palette.shadow, 0.10f, 12.dp, 5.dp)
            .background(Palette.card, CircleShape)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Palette.ink, modifier = Modifier.size(21.dp))
    }
}

@Composable
fun CircleIconButton(icon: ImageVector, contentDescription: String?, modifier: Modifier = Modifier, size: Dp = 54.dp, onClick: () -> Unit) {
    Box(
        modifier
            .size(size)
            .pressable(0.92f, onClick = onClick)
            .softShadow(CircleShape, Palette.shadow, 0.12f, 16.dp, 7.dp)
            .background(Palette.card, CircleShape)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Palette.pink, modifier = Modifier.size(26.dp))
    }
}

// Persone

@Composable
fun Avatar(person: Person, modifier: Modifier = Modifier, size: Dp = 38.dp, selected: Boolean = false) {
    Initial(person.initial, modifier, size, selected)
}

@Composable
fun Initial(text: String, modifier: Modifier = Modifier, size: Dp = 38.dp, selected: Boolean = false) {
    Box(
        modifier
            .size(size)
            .background(if (selected) Palette.pinkLight else Palette.pinkSoft, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) Color.White else Palette.raspberry,
            style = fixedStyle(Face.semibold, size.value * 0.47f),
            maxLines = 1,
        )
    }
}

/** Icona rosa dentro un cerchio rosa chiaro (card dei conti, opzioni del nuovo conto). */
@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 54.dp, inverted: Boolean = false) {
    Box(
        modifier
            .size(size)
            .background(if (inverted) Palette.pinkLight else Palette.pinkSoft, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (inverted) Color.White else Palette.pinkIcon, modifier = Modifier.size(size * 0.44f))
    }
}

// Layout

/**
 * La struttura delle schermate del percorso: intestazione fissa, contenuto che scorre e
 * fascia in basso con l'azione principale. Il contenuto passa sotto la fascia; `content`
 * riceve l'altezza della fascia, da lasciare libera in fondo alla lista.
 */
@Composable
fun FlowScaffold(
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable (bottomInset: Dp) -> Unit,
) {
    var barHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    Box(modifier.fillMaxSize()) {
        BackdropView()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
            header()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                content(with(density) { barHeight.toDp() })
                if (bottomBar != null) {
                    Box(Modifier.align(Alignment.BottomCenter).onSizeChanged { barHeight = it.height }) {
                        bottomBar()
                    }
                }
            }
        }
        overlay()
    }
}

/**
 * La fascia in basso con l'azione principale. Il contenuto che scorre sotto sfuma
 * nel gradiente di sfondo, con gli stessi colori del gradiente in quel punto: il gradiente
 * a tutto schermo si disegna spostato, così dietro la fascia c'è proprio la sua fetta.
 */
@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    topPadding: Dp = 26.dp,
    bottomPadding: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = LocalDark.current
    val screenHeight = LocalScreenHeight.current
    var top by remember { mutableFloatStateOf(0f) }
    Column(
        modifier
            .fillMaxWidth()
            .onGloballyPositioned { top = it.positionInRoot().y }
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawBehind {
                drawRect(Backdrop.brush(dark, screenHeight, top))
                drawRect(
                    Brush.verticalGradient(0f to Color.Transparent, 0.3f to Color.Black),
                    blendMode = BlendMode.DstIn,
                )
            }
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(horizontal = 24.dp)
            .padding(top = topPadding)
            .navigationBarsPadding()
            .padding(bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

/** Sfuma il bordo alto di una lista sotto un'intestazione fissa. */
fun Modifier.topFade(height: Dp = 18.dp): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val h = height.toPx()
        drawRect(
            Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, startY = 0f, endY = h),
            size = Size(size.width, h),
            blendMode = BlendMode.DstIn,
        )
    }

// Intestazione delle schermate del percorso

@Composable
fun FlowHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    /** Una riga d'esempio sotto il sottotitolo, più piccola e colorata. */
    example: String? = null,
    backAction: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 20.dp)
    ) {
        BackButton(backAction, Modifier.padding(top = 4.dp))
        TightTitle(title, Type.flowTitle, Modifier.padding(top = 11.dp, start = 1.dp))
        AnimatedVisibility(subtitle != null, enter = fadeIn(), exit = fadeOut()) {
            Text(
                subtitle ?: "",
                style = Type.lead16,
                color = Palette.inkSoft,
                modifier = Modifier.padding(top = 13.5.dp, start = 1.dp),
            )
        }
        AnimatedVisibility(example != null, enter = fadeIn(), exit = fadeOut()) {
            Text(
                example ?: "",
                style = style(Face.medium, 14.5f, lineHeight = 20f),
                color = Palette.raspberry,
                modifier = Modifier.padding(top = 6.dp, start = 1.dp),
            )
        }
    }
}

// Scorri per eliminare

/** Una riga che scorre a sinistra e scopre il bottone Elimina, come in Mail. */
@Composable
fun SwipeToDelete(
    height: Dp,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    buttonWidth: Dp = 78.dp,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val gap = 10.dp
    val open = with(density) { -(buttonWidth + gap).toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var width by remember { mutableIntStateOf(0) }
    val drag = rememberDraggableState { delta ->
        scope.launch { offset.snapTo((offset.value + delta).coerceIn(open * 1.25f, 0f)) }
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .onSizeChanged { width = it.width }
    ) {
        val progress = (offset.value / open).coerceIn(0f, 1f)
        // Fuori dalla riga il bottone non si vede: compare man mano che la card scorre.
        Box(
            Modifier
                .offset { IntOffset((width + with(density) { gap.toPx() } + offset.value).roundToInt(), 0) }
                .width(buttonWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    alpha = progress
                    scaleX = 0.8f + 0.2f * progress
                    scaleY = 0.8f + 0.2f * progress
                }
                .pressable(0.94f) {
                    onDelete()
                    scope.launch { offset.snapTo(0f) }
                }
                .background(
                    if (LocalDark.current) hex(0xD9466F) else hex(0xE0527A),
                    RoundedCornerShape(Metrics.cardRadius),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Outlined.Delete, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Text(t("common.delete"), style = style(Face.semibold, 13f), color = Color.White, maxLines = 1)
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .fillMaxSize()
                .draggable(
                    drag,
                    Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        // La riga si ferma solo chiusa o tutta aperta, a seconda di dove e con che slancio la si lascia.
                        val shouldOpen = if (abs(velocity) > 300f) velocity < 0 else offset.value < open / 2
                        offset.animateTo(if (shouldOpen) open else 0f, swiftSpring(0.35f, 0.85f))
                    },
                )
        ) {
            content()
        }
    }
}

// Stati

/** "I conti tornano" / "Mancano €5,00". */
@Composable
fun StatusPill(text: String, positive: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(33.5.dp)
            .background(Palette.pillFill, CircleShape)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(text, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "pill") { value ->
            Text(
                value,
                style = style(Face.semibold, 14f),
                color = if (positive) Palette.successText else Palette.raspberry,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun Caption(text: String, modifier: Modifier = Modifier) {
    AnimatedContent(text, modifier, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "caption") { value ->
        Text(value, style = Type.caption, color = Palette.raspberry, textAlign = TextAlign.Center)
    }
}

// Campi

/** Campo importo "€ 30" delle righe "Chi ha pagato". */
@Composable
fun AmountField(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 104.dp,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocusChanged: (Boolean) -> Unit = {},
    onDone: () -> Unit = {},
) {
    val currency = LocalCurrency.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .width(width)
            .height(45.5.dp)
            .background(Palette.fieldFill, shape)
            .border(1.5.dp, Palette.fieldBorder, shape)
            .pointerInput(Unit) { detectTapGestures { focusRequester.requestFocus() } }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            currency.symbol,
            style = style(Face.semibold, 17f),
            color = Palette.raspberry,
            maxLines = 1,
            softWrap = false,
        )
        val valueStyle = Type.fieldValue.copy(color = Palette.ink, textAlign = TextAlign.End)
        BasicTextField(
            value = text,
            onValueChange = { onTextChange(Money.sanitizeInput(it, currency.minorDigits > 0)) },
            singleLine = true,
            textStyle = valueStyle,
            cursorBrush = SolidColor(Palette.pink),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusChanged(it.isFocused) },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (text.isEmpty()) Text("0", style = valueStyle.copy(color = Palette.fieldPlaceholder), maxLines = 1)
                    inner()
                }
            },
        )
    }
}

/** Bordo tratteggiato delle chip "+ Anna" e "Tutti". `radius` nullo = capsula. */
fun Modifier.dashedBorder(color: Color, radius: Dp? = null, width: Dp = 1.3.dp, dash: Float = 3.5f, gap: Float = 3f): Modifier =
    drawBehind {
        val stroke = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.dp.toPx(), gap.dp.toPx())),
        )
        val inset = stroke.width / 2
        val r = (radius?.toPx() ?: (size.height / 2)) - inset
        drawRoundRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke.width, size.height - stroke.width),
            cornerRadius = CornerRadius(r, r),
            style = stroke,
        )
    }

// Icone disegnate

/** La calcolatrice del pulsante "Calcola" (non c'è un'icona Material equivalente). */
@Composable
fun CalculatorIcon(color: Color = Color.White) {
    Canvas(Modifier.size(15.dp, 18.dp)) {
        val w = size.width
        val h = size.height
        val px = 1.dp.toPx()
        val body = androidx.compose.ui.geometry.Rect(0.75f * px, 0.75f * px, w - 0.75f * px, h - 0.75f * px)
        drawRoundRect(color, body.topLeft, body.size, CornerRadius(2.5f * px), style = Stroke(1.5f * px))
        drawRoundRect(
            color,
            Offset(body.left + w * 0.2f, body.top + h * 0.14f),
            Size(body.width - w * 0.4f, h * 0.14f),
            CornerRadius(0.8f * px),
        )
        val dot = w * 0.13f
        for (row in 0 until 3) for (col in 0 until 3) {
            val x = body.left + w * 0.2f + col * (body.width - w * 0.4f - dot) / 2
            val y = body.top + h * 0.43f + row * h * 0.16f
            drawRoundRect(color, Offset(x, y), Size(dot, dot), CornerRadius(0.8f * px))
        }
    }
}

/** Il simbolo ÷ della divisione in parti uguali. */
val DivideIcon: ImageVector by lazy {
    ImageVector.Builder("Divide", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(4.5f, 11.1f); lineTo(19.5f, 11.1f); lineTo(19.5f, 12.9f); lineTo(4.5f, 12.9f); close()
        }
        for (cy in listOf(6.6f, 17.4f)) {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10.3f, cy)
                arcToRelative(1.7f, 1.7f, 0f, true, true, 3.4f, 0f)
                arcToRelative(1.7f, 1.7f, 0f, true, true, -3.4f, 0f)
                close()
            }
        }
    }.build()
}

// Fogli

/**
 * Un foglio che sale dal basso, con la maniglia e il gradiente rosa dei fogli iOS.
 * Si chiude toccando fuori, trascinandolo giù o col tasto indietro.
 */
@Composable
fun BoxScope.BottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(visible) { onDismiss() }
    AnimatedVisibility(visible, enter = fadeIn(tween(250)), exit = fadeOut(tween(200))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Palette.scrim.copy(alpha = 0.32f))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
        )
    }
    AnimatedVisibility(
        visible,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(swiftSpring(0.42f, 0.88f, IntOffset.VisibilityThreshold)) { it },
        exit = slideOutVertically(tween(250)) { it },
    ) {
        SheetSurface(onDismiss, modifier, showHandle = true, content = content)
    }
}

@Composable
fun SheetSurface(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showHandle: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    val density = LocalDensity.current
    val state = rememberDraggableState { delta -> scope.launch { drag.snapTo((drag.value + delta).coerceAtLeast(0f)) } }
    Column(
        modifier
            .fillMaxWidth()
            .offset { IntOffset(0, drag.value.roundToInt()) }
            .clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp))
            .background(Brush.verticalGradient(listOf(Palette.sheetTop, Palette.sheetBottom)))
            .pointerInput(Unit) { detectTapGestures { } }
            .draggable(
                state,
                Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (drag.value > with(density) { 90.dp.toPx() } || velocity > 1200f) {
                        onDismiss()
                    } else {
                        drag.animateTo(0f, swiftSpring(0.3f, 0.85f))
                    }
                },
            )
            .navigationBarsPadding()
            .imePadding()
    ) {
        if (showHandle) {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(36.dp, 5.dp).background(Palette.handle, CircleShape))
            }
        }
        content()
    }
}

/**
 * Dà il fuoco a un campo appena compare. Dentro una lista il campo nasce un attimo dopo
 * la schermata (e durante l'animazione d'ingresso): si riprova finché non c'è.
 */
suspend fun FocusRequester.requestWhenReady(isFocused: () -> Boolean): Boolean {
    repeat(30) {
        runCatching { requestFocus() }
        kotlinx.coroutines.delay(50)
        if (isFocused()) return true
    }
    return false
}

/** Uno spazio verticale. */
@Composable
fun VSpace(height: Dp) = Spacer(Modifier.height(height))

/** Padding orizzontale e verticale insieme, come in SwiftUI. */
fun Modifier.paddingHV(horizontal: Dp = 0.dp, top: Dp = 0.dp, bottom: Dp = 0.dp): Modifier =
    padding(start = horizontal, end = horizontal, top = top, bottom = bottom)

fun contentPadding(horizontal: Dp, top: Dp, bottom: Dp) =
    PaddingValues(start = horizontal, end = horizontal, top = top, bottom = bottom)

// Aptico

object Haptics {
    var view: View? = null

    fun tap() {
        view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun select() {
        view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun success() {
        view?.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
        )
    }

    fun warning() {
        view?.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        )
    }
}
