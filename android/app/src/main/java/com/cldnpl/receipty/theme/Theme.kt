package com.cldnpl.receipty.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.cldnpl.receipty.R
import kotlin.math.PI

fun hex(rgb: Long, alpha: Float = 1f): Color = Color(0xFF000000 or rgb).copy(alpha = alpha)

/** Tema scuro: lo decide RootView (impostazioni → Aspetto). */
val LocalDark = compositionLocalOf { false }

/**
 * Colori campionati dal mockup (Figma "Chi paga?"), con il loro gemello per il tema scuro:
 * al posto del rosa pieno, un prugna scurissimo che in basso si accende di lampone.
 */
object Palette {
    @Composable @ReadOnlyComposable
    private fun c(light: Long, dark: Long, lightAlpha: Float = 1f, darkAlpha: Float = 1f): Color =
        if (LocalDark.current) hex(dark, darkAlpha) else hex(light, lightAlpha)

    val ink @Composable @ReadOnlyComposable get() = c(0x1A1A1A, 0xF7EEF2)
    val inkSoft @Composable @ReadOnlyComposable get() = c(0x4C4346, 0xCDB9C1)
    val gray @Composable @ReadOnlyComposable get() = c(0x58585C, 0xAE9FA6)
    val muted @Composable @ReadOnlyComposable get() = c(0x867A7E, 0x8F7F87)

    val raspberry @Composable @ReadOnlyComposable get() = c(0x822F4B, 0xF59BBD)
    val raspberryDeep @Composable @ReadOnlyComposable get() = c(0x6F2441, 0xF7A8C6)
    val warning @Composable @ReadOnlyComposable get() = c(0xA04262, 0xFF8FB3)

    val pink = hex(0xF57FA7)
    val pinkIcon @Composable @ReadOnlyComposable get() = c(0xF083A6, 0xF58DB1)
    val pinkText @Composable @ReadOnlyComposable get() = c(0xE388A7, 0xF58DB1)
    val pinkLight = hex(0xFA9ABA)
    val pinkSoft @Composable @ReadOnlyComposable get() = c(0xFEE2E9, 0x3A2230)
    val pinkMist @Composable @ReadOnlyComposable get() = c(0xFFF2F5, 0x2E1D26)
    val flag @Composable @ReadOnlyComposable get() = c(0xFFEAEF, 0x3D1F2D)
    val hairline @Composable @ReadOnlyComposable get() = c(0xF1E5E9, 0x3A2A31)
    val dashed @Composable @ReadOnlyComposable get() = c(0xFDBAD0, 0x6B3A50)
    val dashedSoft @Composable @ReadOnlyComposable get() = c(0xE6CED6, 0x5A3A48)
    val placeholder @Composable @ReadOnlyComposable get() = c(0xD9A2B5, 0x8C6B79)
    val chipText @Composable @ReadOnlyComposable get() = c(0x652B41, 0xF7B6CD)
    val chipFill @Composable @ReadOnlyComposable get() = c(0xFFDFEA, 0x3A2230)

    val fieldFill @Composable @ReadOnlyComposable get() = c(0xFFF6F9, 0x2A1B22)
    val fieldBorder @Composable @ReadOnlyComposable get() = c(0xF4DEE5, 0x4A2F3C)
    val fieldPlaceholder @Composable @ReadOnlyComposable get() = c(0xB28D9A, 0x8C6F7B)

    val disabledFill @Composable @ReadOnlyComposable get() = c(0xFA93B6, 0x5A2E42)
    val disabledText @Composable @ReadOnlyComposable get() = c(0xFFCDDE, 0xA07488)

    val check @Composable @ReadOnlyComposable get() = c(0x2EB889, 0x3CCB98)
    val success = hex(0x22C090)
    val successText @Composable @ReadOnlyComposable get() = c(0x2B8969, 0x4ED6A4)
    val arrow @Composable @ReadOnlyComposable get() = c(0xE686A7, 0xF58DB1)

    /** Superfici: card piene, righe semitrasparenti, pillole chiare. */
    val card @Composable @ReadOnlyComposable get() = c(0xFFFFFF, 0x251A21)
    val cardSoft @Composable @ReadOnlyComposable get() = c(0xFFFFFF, 0x251A21, 0.8f, 0.85f)
    val cardGhost @Composable @ReadOnlyComposable get() = c(0xFFFFFF, 0x251A21, 0.55f, 0.5f)
    val pillFill @Composable @ReadOnlyComposable get() = c(0xFFF8FA, 0x2C1E26)

    /** Il nero dei bottoni "Aggiungi" e "Fatto": nel tema scuro diventa chiaro. */
    val inverseFill @Composable @ReadOnlyComposable get() = c(0x1A1A1A, 0xF7EEF2)
    val inverseText @Composable @ReadOnlyComposable get() = c(0xFFFFFF, 0x1A1A1A)

    val sheetTop @Composable @ReadOnlyComposable get() = c(0xFFFAFC, 0x241920)
    val sheetBottom @Composable @ReadOnlyComposable get() = c(0xFEF5F8, 0x1C141A)
    val handle @Composable @ReadOnlyComposable get() = c(0xF2C6D5, 0x5A3A48)
    val scrim = hex(0x27020E)
    val shadow @Composable @ReadOnlyComposable get() = c(0xB0325F, 0x000000)

    /**
     * La carta dello scontrino disegnato (splash): bianca, e nel tema scuro un prugna
     * schiarito, perché sul fondo quasi nero il foglio si legga lo stesso.
     */
    val receiptPaper @Composable @ReadOnlyComposable get() = c(0xFFFFFF, 0x362430)
    val receiptEdge @Composable @ReadOnlyComposable get() = c(0xFFFFFF, 0x54394B)

    val previewDark @Composable @ReadOnlyComposable get() = c(0x2A2227, 0x120C0F)
    val bracket = hex(0xF998BB)
    val paper = hex(0xF7F1EA)
}

/**
 * Il gradiente verticale di tutte le schermate, dal rosa quasi bianco al rosa pieno
 * (nel tema scuro: dal prugna quasi nero al lampone).
 */
object Backdrop {
    private val light = listOf(
        0.00f to 0xFEF6F9, 0.10f to 0xFEF2F6, 0.20f to 0xFFEEF3, 0.30f to 0xFEE9F0,
        0.40f to 0xFCE0EA, 0.50f to 0xFCD3E2, 0.60f to 0xFDC8DA, 0.70f to 0xFFBCD3,
        0.80f to 0xFFB2CC, 0.90f to 0xFEA2C1, 1.00f to 0xFE93B8,
    )
    private val dark = listOf(
        0.00f to 0x150F12, 0.20f to 0x181114, 0.40f to 0x1D1318, 0.55f to 0x23151C,
        0.70f to 0x2D1722, 0.85f to 0x3A1A2A, 1.00f to 0x4A1E34,
    )

    /** Stesse posizioni in entrambi i temi: si campiona il gradiente scuro dove cadono quelle chiare. */
    fun stops(isDark: Boolean): Array<Pair<Float, Color>> =
        light.map { (location, _) -> location to color(location, isDark) }.toTypedArray()

    /** Il colore del gradiente a una frazione dell'altezza dello schermo (0 = in alto). */
    fun color(at: Float, isDark: Boolean): Color {
        val stops = if (isDark) dark else light
        val t = at.coerceIn(0f, 1f)
        val i = stops.indexOfLast { it.first <= t }
        if (i < 0 || i >= stops.size - 1) return hex(stops.last().second.toLong())
        val (la, ha) = stops[i]
        val (lb, hb) = stops[i + 1]
        val u = (t - la) / (lb - la)
        fun ch(h: Int, shift: Int) = ((h shr shift) and 0xFF) / 255f
        fun mix(shift: Int) = ch(ha, shift) + (ch(hb, shift) - ch(ha, shift)) * u
        return Color(mix(16), mix(8), mix(0))
    }

    /**
     * Il gradiente a tutto schermo, spostato in modo che chi lo disegna a `top` pixel
     * dall'alto dello schermo veda la fetta giusta.
     */
    fun brush(isDark: Boolean, screenHeight: Float, top: Float = 0f): Brush =
        Brush.verticalGradient(*stops(isDark), startY = -top, endY = screenHeight - top)
}

@Composable
fun BackdropView(modifier: Modifier = Modifier) {
    val dark = LocalDark.current
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(*Backdrop.stops(dark))))
}

// Tipografia (Inter, come nel Figma)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)

/**
 * Fraunces (SIL OFL), istanza statica opsz 144 / wght 800 / SOFT 100 / WONK 1: le grazie
 * morbide e la coda tonda della y stanno bene accanto allo scontrino disegnato a pennarello.
 * Serve solo al marchio dello splash, l'interfaccia resta tutta in Inter.
 */
val Fraunces = FontFamily(Font(R.font.fraunces_soft_bold, FontWeight.Bold))

enum class Face(val weight: FontWeight) {
    regular(FontWeight.Normal),
    medium(FontWeight.Medium),
    semibold(FontWeight.SemiBold),
    bold(FontWeight.Bold),
    extraBold(FontWeight.ExtraBold),
}

/**
 * Uno stile di testo come nel Figma: `tracking` in em (-0.03 = -3%), `lineHeight` la distanza
 * tra le linee di base se diversa da quella naturale del font (1,21 em).
 */
fun style(face: Face, size: Float, tracking: Float = 0f, lineHeight: Float? = null): TextStyle =
    TextStyle(
        fontFamily = Inter,
        fontWeight = face.weight,
        fontSize = size.sp,
        letterSpacing = tracking.em,
        lineHeight = (lineHeight ?: (size * 1.21f)).sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )

object Type {
    val hero = style(Face.extraBold, 46f, -0.03f)
    val scanTitle = style(Face.extraBold, 39f, -0.02f, 42.5f)
    val flowTitle = style(Face.extraBold, 31f, -0.02f, 34f)
    val resultTitle = style(Face.extraBold, 42.5f, -0.02f)

    val lead = style(Face.regular, 17f, lineHeight = 24.5f)
    val lead16 = style(Face.regular, 16f, lineHeight = 23f)
    val section = style(Face.semibold, 14.5f, -0.02f)

    val cardTitle = style(Face.semibold, 17.5f, 0.01f)
    val cardSub = style(Face.medium, 13.5f, 0.01f)
    val cardMeta = style(Face.medium, 13f, 0.01f)

    val button = style(Face.bold, 18.5f, -0.02f)
    val sheetTitle = style(Face.bold, 23.5f, -0.01f)
    val optionTitle = style(Face.bold, 18f)
    val optionSub = style(Face.regular, 14f)

    val rowName = style(Face.medium, 16f)
    val rowPrice = style(Face.semibold, 16f)
    val rowWarning = style(Face.medium, 14f)
    val totalLabel = style(Face.regular, 14.5f)
    val totalValue = style(Face.extraBold, 22f, -0.02f)

    val pillLabel = style(Face.semibold, 15.5f)
    val personName = style(Face.medium, 17f)
    val personNameStrong = style(Face.semibold, 17f)
    val chip = style(Face.medium, 15f)
    val caption = style(Face.semibold, 14.5f, -0.01f)
    val micro = style(Face.medium, 12f)
    val itemTitle = style(Face.semibold, 16f)
    val perHead = style(Face.semibold, 12f)

    val bigAmount = style(Face.extraBold, 46f, -0.03f)
    val transferName = style(Face.semibold, 17.5f)
    val transferSub = style(Face.regular, 13.5f)
    val transferAmount = style(Face.extraBold, 24.5f, -0.02f)
    val link = style(Face.semibold, 15.5f, -0.01f)
    val smallLink = style(Face.semibold, 14f)
    val fieldValue = style(Face.bold, 19f, -0.01f)
}

/** Una misura in sp che non segue la dimensione del testo di sistema (lo splash è un disegno). */
@Composable
fun fixedSp(value: Float): TextUnit = with(LocalDensity.current) { (value / fontScale).sp }

/** Uno stile che non cresce col testo di sistema: per le lettere dentro cerchi di misura fissa. */
@Composable
fun fixedStyle(face: Face, size: Float): TextStyle =
    style(face, size).copy(fontSize = fixedSp(size), lineHeight = fixedSp(size * 1.21f))

/**
 * Titolo su più righe con interlinea più stretta di quella naturale del font.
 * In Compose basta l'interlinea dello stile: niente righe impilate a mano come su iOS.
 */
@Composable
fun TightTitle(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
) {
    Text(
        text = text,
        style = style,
        color = Palette.ink,
        textAlign = textAlign,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * Il marchio: sempre lo stesso lettering, sullo splash e in cima alla home.
 * Fraunces lampone invece dell'Inter nero del resto dei titoli — è un nome proprio,
 * non un titolo di schermata.
 */
@Composable
fun Wordmark(modifier: Modifier = Modifier, size: Float = 46f, scales: Boolean = true) {
    Text(
        text = "Receipty",
        modifier = modifier,
        color = Palette.raspberry,
        style = TextStyle(
            fontFamily = Fraunces,
            fontWeight = FontWeight.Bold,
            fontSize = if (scales) size.sp else fixedSp(size),
            letterSpacing = (-0.017f).em,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
    )
}

// Geometria comune

object Metrics {
    val cardRadius = 28.dp
    val buttonHeight = 60.dp
    val backSize = 43.dp
}

/** L'ombra morbida di SwiftUI: `radius` è la sfocatura, `y` lo spostamento verso il basso. */
fun Modifier.softShadow(shape: Shape, color: Color, alpha: Float, radius: Dp, y: Dp = 0.dp): Modifier =
    dropShadow(shape, Shadow(radius = radius, color = color, offset = DpOffset(0.dp, y), alpha = alpha))

/** Card bianca con l'ombra rosata del mockup. */
@Composable
fun Modifier.card(radius: Dp = Metrics.cardRadius, fill: Color = Palette.card): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .softShadow(shape, Palette.shadow, 0.08f, 18.dp, 8.dp)
        .background(fill, shape)
}

/** La molla di SwiftUI (`response`, `dampingFraction`) tradotta in quella di Compose. */
fun <T> swiftSpring(response: Float, damping: Float, visibilityThreshold: T? = null): SpringSpec<T> {
    val omega = 2 * PI.toFloat() / response
    return spring(dampingRatio = damping, stiffness = omega * omega, visibilityThreshold = visibilityThreshold)
}
