package com.cldnpl.receipty.views

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cldnpl.receipty.theme.Backdrop
import com.cldnpl.receipty.theme.Fraunces
import com.cldnpl.receipty.theme.Haptics
import com.cldnpl.receipty.theme.LocalDark
import com.cldnpl.receipty.theme.Palette
import com.cldnpl.receipty.theme.Wordmark
import com.cldnpl.receipty.theme.fixedSp
import com.cldnpl.receipty.theme.softShadow
import com.cldnpl.receipty.theme.swiftSpring
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * L'avvio: lo scontrino dell'icona si scrive da solo. Cade dall'alto, la riga di testa
 * viene scarabocchiata a pennarello, poi le tre voci con i loro prezzi che cadono di lato,
 * infine il totale si stampa di colpo e sotto resta il marchio. Si tocca per saltarlo.
 *
 * Il fondo è lo stesso gradiente di tutte le schermate, e sotto c'è già l'app: alla fine
 * basta sfumare via scontrino e marchio. L'uscita è la transizione che gli mette `RootView`:
 * `onFinish` la fa partire, non aspetta che sia finita.
 */
@Composable
fun SplashView(onFinish: () -> Unit) {
    val context = LocalContext.current
    val reduceMotion = remember {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE) == 0f }
            .getOrDefault(false)
    }
    val dark = LocalDark.current

    val paper = remember { Animatable(0f) }
    val header = remember { Animatable(0f) }
    val rule = remember { Animatable(0f) }
    val rows = remember { List(3) { Animatable(0f) } }
    val prices = remember { List(3) { Animatable(0f) } }
    val total = remember { Animatable(0f) }
    val mark = remember { Animatable(0f) }
    val done = remember { booleanArrayOf(false) }

    fun finish() {
        if (done[0]) return
        done[0] = true
        onFinish()
    }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            (listOf(paper, header, rule, total, mark) + rows + prices).forEach { it.snapTo(1f) }
            delay(1100)
            finish()
            return@LaunchedEffect
        }
        coroutineScope {
            launch { paper.animateTo(1f, swiftSpring(0.52f, 0.72f)) }
            delay(180)
            launch { header.animateTo(1f, tween(340, easing = FastOutSlowInEasing)) }
            delay(160)
            launch { rule.animateTo(1f, tween(220)) }
            for (i in 0 until 3) {
                delay(100)
                launch { rows[i].animateTo(1f, tween(280)) }
                // Il prezzo cade quando la riga è quasi finita, senza aspettarla.
                launch {
                    delay(170)
                    prices[i].animateTo(1f, swiftSpring(0.34f, 0.55f))
                }
            }
            delay(320)
            Haptics.tap()
            launch { total.animateTo(1f, swiftSpring(0.40f, 0.52f)) }
            delay(170)
            launch { mark.animateTo(1f, tween(460)) }
            delay(600)
            finish()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            // Si parte dal colore piatto del launch screen e il gradiente ci sale sopra
            // mentre lo scontrino scende: così il passaggio non si vede.
            .background(Backdrop.color(0f, dark))
            .drawBehind {
                drawRect(Brush.verticalGradient(*Backdrop.stops(dark)), alpha = paper.value.coerceIn(0f, 1f))
            }
            .pointerInput(Unit) { detectTapGestures { finish() } }
            .semantics { contentDescription = "Receipty" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(36.dp)) {
            Receipt(paper.value, header.value, rule.value, rows.map { it.value }, prices.map { it.value }, total.value)
            val m = mark.value
            Wordmark(
                scales = false,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = m.coerceIn(0f, 1f)
                        translationY = (1 - m) * 18.dp.toPx()
                    }
                    .blur(((1 - m) * 5).coerceAtLeast(0f).dp),
            )
        }
    }
}

/** Le stesse cifre dell'icona: 20 + 30 + 5, e in fondo il tondo 100. */
private val amounts = listOf("20", "30", "5")

/** Righe di lunghezza diversa: scritte a mano non vengono mai uguali. */
private val widths = listOf(92.dp, 79.dp, 86.dp)

@Composable
private fun Receipt(paper: Float, header: Float, rule: Float, rows: List<Float>, prices: List<Float>, total: Float) {
    val ink = Palette.ink
    val paperColor = Palette.receiptPaper
    val edge = Palette.receiptEdge
    val shadow = Palette.shadow
    val shape = remember { receiptShape() }
    Column(
        Modifier
            .graphicsLayer {
                // La stessa inclinazione dell'icona.
                rotationZ = -3.5f
                alpha = paper.coerceIn(0f, 1f)
                val s = 0.86f + 0.14f * paper
                scaleX = s
                scaleY = s
                translationY = (1 - paper) * (-48).dp.toPx()
            }
            .width(210.dp)
            .softShadow(shape, shadow, 0.22f, 26.dp, 16.dp)
            .drawBehind {
                val outline = shape.createOutline(size, layoutDirection, this)
                val path = (outline as androidx.compose.ui.graphics.Outline.Generic).path
                drawPath(path, paperColor)
                // Nel tema scuro il foglio ha un bordo appena più chiaro: senza, l'ombra nera
                // non lo stacca dal fondo.
                drawPath(path, edge.copy(alpha = 0.9f), style = Stroke(1.dp.toPx()))
            }
            .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 32.dp)
    ) {
        Scribble(0, header, 5f, ink, Modifier.fillMaxWidth().height(26.dp))
        Box(
            Modifier
                .padding(top = 15.dp)
                .fillMaxWidth()
                .height(1.2.dp)
                .drawBehind { drawRect(ink.copy(alpha = 0.45f), size = Size(size.width * rule, size.height)) }
        )
        for (i in 0 until 3) {
            Row(
                Modifier.fillMaxWidth().padding(top = if (i == 0) 19.dp else 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Scribble(i + 1, rows[i], 4.4f, ink, Modifier.size(widths[i], 21.dp))
                Spacer(Modifier.weight(1f))
                val p = prices[i]
                Text(
                    amounts[i],
                    color = ink,
                    style = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = fixedSp(20f)),
                    modifier = Modifier.graphicsLayer {
                        alpha = p.coerceIn(0f, 1f)
                        val s = 0.4f + 0.6f * p
                        scaleX = s
                        scaleY = s
                        rotationZ = -16f * (1 - p)
                    },
                )
            }
        }
        Text(
            "100",
            color = ink,
            style = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = fixedSp(31f)),
            modifier = Modifier
                .padding(top = 18.dp)
                .wrapContentSize()
                .graphicsLayer {
                    alpha = total.coerceIn(0f, 1f)
                    val s = 1.7f - 0.7f * total
                    scaleX = s
                    scaleY = s
                    rotationZ = 7f - 9f * total
                },
        )
    }
}

/**
 * La carta dello scontrino: angoli alti arrotondati e il bordo basso strappato a zigzag,
 * come nell'icona.
 */
private fun receiptShape(teeth: Int = 7, toothHeightDp: Float = 10f, radiusDp: Float = 7f) =
    GenericShape { size, _ ->
        // GenericShape non conosce la densità: la si ricava dalla larghezza fissa di 210dp.
        val d = size.width / 210f
        val toothHeight = toothHeightDp * d
        val radius = radiusDp * d
        val hem = size.height - toothHeight
        moveTo(0f, radius)
        quadraticTo(0f, 0f, radius, 0f)
        lineTo(size.width - radius, 0f)
        quadraticTo(size.width, 0f, size.width, radius)
        lineTo(size.width, hem)
        val step = size.width / teeth
        for (i in teeth - 1 downTo 0) {
            val x = i * step
            lineTo(x + step / 2, size.height)
            lineTo(x, hem)
        }
        close()
    }

/**
 * Una riga scarabocchiata: il gancio della prima lettera e poi un'onda che si appiattisce
 * verso destra, come la scrittura illeggibile dell'icona. `seed` cambia frequenza, fase e
 * smorzamento, così non ce ne sono due uguali. Compare da sinistra, come se qualcuno la
 * stesse scrivendo.
 */
@Composable
private fun Scribble(seed: Int, progress: Float, lineWidth: Float, color: Color, modifier: Modifier) {
    Box(modifier.drawBehind { drawScribble(seed, progress, lineWidth, color) })
}

private val freqs = doubleArrayOf(2.7, 2.1, 2.9, 2.3)
private val phases = doubleArrayOf(0.0, 0.7, 1.9, 1.2)
private val decays = doubleArrayOf(3.4, 2.5, 3.1, 2.1)

private fun DrawScope.drawScribble(seed: Int, progress: Float, lineWidth: Float, color: Color) {
    if (progress <= 0f) return
    val i = seed % freqs.size
    val w = size.width.toDouble()
    val hgt = size.height
    val mid = hgt / 2.0
    val amp = hgt * 0.42
    val hook = 0.07
    val path = Path().apply {
        moveTo(1f, hgt)
        quadraticTo(-size.width * 0.01f, hgt * 0.12f, (size.width * hook).toFloat(), 0f)
        var u = hook
        while (u <= 1) {
            val v = (u - hook) / (1 - hook)
            val y = mid - amp * exp(-decays[i] * v) * sin(freqs[i] * v * 2 * PI + phases[i])
            lineTo((w * u).toFloat(), y.toFloat())
            u += 0.012
        }
    }
    val measure = PathMeasure().apply { setPath(path, false) }
    val trimmed = Path()
    measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), trimmed, true)
    drawPath(
        trimmed,
        color,
        style = Stroke(lineWidth.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}
