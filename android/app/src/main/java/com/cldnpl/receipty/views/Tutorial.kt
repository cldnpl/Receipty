package com.cldnpl.receipty.views

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cldnpl.receipty.localization.t
import com.cldnpl.receipty.theme.Face
import com.cldnpl.receipty.theme.Metrics
import com.cldnpl.receipty.theme.Palette
import com.cldnpl.receipty.theme.pressable
import com.cldnpl.receipty.theme.softShadow
import com.cldnpl.receipty.theme.style
import com.cldnpl.receipty.theme.swiftSpring
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** I punti dell'interfaccia che il tutorial illumina. */
enum class CoachTarget { PLUS_BUTTON, SCAN_OPTION, MANUAL_OPTION }

/**
 * Il tutorial della prima volta: lo schermo si scurisce e la patina nera si stringe
 * attorno a un solo elemento, un passo alla volta. Si può sempre saltare.
 */
class Coach {
    enum class Step {
        PLUS, SCAN, MANUAL;

        val target: CoachTarget
            get() = when (this) {
                PLUS -> CoachTarget.PLUS_BUTTON
                SCAN -> CoachTarget.SCAN_OPTION
                MANUAL -> CoachTarget.MANUAL_OPTION
            }

        val title: String get() = t("coach.${ordinal + 1}.title")
        val message: String get() = t("coach.${ordinal + 1}.message")

        /** Al primo passo si tocca davvero il +: il buco nella patina lascia passare il tocco. */
        val tapThrough: Boolean get() = this == PLUS
    }

    var step by mutableStateOf<Step?>(null)

    /** Posizione dei bersagli, nelle coordinate della radice. */
    val frames = mutableStateMapOf<CoachTarget, Rect>()

    /** Quando la patina è "aperta" su tutto lo schermo (inizio e fine dell'animazione). */
    var wide by mutableStateOf(true)

    val isActive: Boolean get() = step != null
}

val LocalCoach = staticCompositionLocalOf<Coach?> { null }

/** Registra la posizione di un elemento che il tutorial può illuminare. */
@Composable
fun Modifier.coachTarget(target: CoachTarget): Modifier {
    val coach = LocalCoach.current ?: return this
    return onGloballyPositioned { coach.frames[target] = it.boundsInRoot() }
}

// Patina

@Composable
fun CoachOverlay(
    coach: Coach,
    onNext: (Coach.Step) -> Unit,
    onSkip: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val screen = Rect(0f, 0f, w, h)

        fun focusedRect(step: Coach.Step): Rect {
            val target = coach.frames[step.target] ?: Rect(Offset(w / 2, h / 2), Size.Zero)
            val pad = with(density) { (if (step == Coach.Step.PLUS) 9.dp else 7.dp).toPx() }
            return target.inflate(pad)
        }

        val step = coach.step ?: Coach.Step.PLUS
        val isCircle = step == Coach.Step.PLUS
        val focused = focusedRect(step)
        // All'inizio il "buco" è più grande dello schermo: la patina non si vede. Poi si stringe.
        val hole = if (coach.wide) screen.inflate(h) else focused
        val radius = when {
            coach.wide -> h
            isCircle -> focused.height / 2
            else -> with(density) { (Metrics.cardRadius + 7.dp).toPx() }
        }
        val animatedHole by animateRectAsState(hole, swiftSpring(0.7f, 0.86f, Rect.VisibilityThreshold), label = "hole")
        val animatedRadius by animateFloatAsState(radius, swiftSpring(0.7f, 0.86f), label = "radius")

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(step, coach.wide, focused) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val inHole = step.tapThrough && !coach.wide && focused.contains(down.position)
                        val up = waitForUpOrCancellation()
                        up?.consume()
                        if (up != null && inHole) onNext(step)
                    }
                }
        ) {
            val path = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                addRoundRect(RoundRect(animatedHole, CornerRadius(animatedRadius)))
            }
            drawPath(path, Color.Black.copy(alpha = 0.74f))
        }

        if (step.tapThrough && !coach.wide) {
            val pulse = rememberInfiniteTransition(label = "pulse")
            val p by pulse.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
                label = "p",
            )
            val pink = Palette.pink
            Canvas(Modifier.fillMaxSize()) {
                val scale = 1f + 0.35f * p
                drawCircle(
                    pink.copy(alpha = 0.9f * (1 - p)),
                    radius = focused.width / 2 * scale,
                    center = focused.center,
                    style = Stroke(3.dp.toPx()),
                )
            }
        }

        AnimatedVisibility(
            !coach.wide,
            enter = fadeIn(tween(250, delayMillis = 400)) + scaleIn(tween(250, delayMillis = 400), initialScale = 0.96f),
            exit = fadeOut(tween(100)),
        ) {
            AnimatedContent(
                step,
                transitionSpec = {
                    (fadeIn(tween(250, delayMillis = 400)) + scaleIn(tween(250, delayMillis = 400), initialScale = 0.96f))
                        .togetherWith(fadeOut(tween(100)))
                },
                label = "callout",
            ) { s ->
                val target = focusedRect(s)
                val calloutWidth = with(density) { min(w - 40.dp.toPx(), 360.dp.toPx()) }
                var measured by remember { mutableIntStateOf(with(density) { 190.dp.roundToPx() }) }
                val centerY = calloutY(target, h, with(density) { 190.dp.toPx() }, with(density) { 24.dp.toPx() }, density.density)
                Box(Modifier.fillMaxSize()) {
                    Callout(
                        s,
                        onNext = onNext,
                        onSkip = onSkip,
                        modifier = Modifier
                            .width(with(density) { calloutWidth.toDp() })
                            .onSizeChanged { measured = it.height }
                            .offset { IntOffset(((w - calloutWidth) / 2).roundToInt(), (centerY - measured / 2f).roundToInt()) },
                    )
                }
            }
        }
    }
}

/** La nuvoletta sta sotto il bersaglio se è in alto, sopra se è in basso. */
private fun calloutY(target: Rect, height: Float, calloutHeight: Float, gap: Float, density: Float): Float {
    if (target.center.y < height / 2) {
        return min(target.bottom + gap + calloutHeight / 2, height - calloutHeight / 2 - 40 * density)
    }
    return max(target.top - gap - calloutHeight / 2, calloutHeight / 2 + 60 * density)
}

@Composable
private fun Callout(step: Coach.Step, onNext: (Coach.Step) -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .softShadow(shape, Color.Black, 0.25f, 24.dp, 10.dp)
            .background(Palette.card, shape)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                t("coach.step", step.ordinal + 1, Coach.Step.entries.size),
                style = style(Face.semibold, 13f),
                color = Palette.raspberry,
            )
            Spacer(Modifier.weight(1f))
            Text(
                t("coach.skip"),
                style = style(Face.semibold, 14f),
                color = Palette.gray,
                modifier = Modifier.pressable(0.95f, onClick = onSkip),
            )
        }
        Text(
            step.title,
            style = style(Face.extraBold, 24f, -0.02f),
            color = Palette.ink,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            step.message,
            style = style(Face.regular, 16f, lineHeight = 22.5f),
            color = Palette.inkSoft,
            modifier = Modifier.padding(top = 6.dp),
        )
        Row(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Spacer(Modifier.weight(1f))
            val label = t(
                when {
                    step == Coach.Step.MANUAL -> "coach.gotIt"
                    step.tapThrough -> "coach.showMe"
                    else -> "common.next"
                }
            )
            Box(
                Modifier
                    .height(44.dp)
                    .pressable(0.95f) { onNext(step) }
                    .background(Palette.pink, CircleShape)
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = style(Face.bold, 16f), color = Color.White)
            }
        }
    }
}
