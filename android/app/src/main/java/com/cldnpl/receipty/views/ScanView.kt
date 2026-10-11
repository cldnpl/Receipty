package com.cldnpl.receipty.views

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cldnpl.receipty.localization.t
import com.cldnpl.receipty.models.AppModel
import com.cldnpl.receipty.models.BillDraft
import com.cldnpl.receipty.models.BillSource
import com.cldnpl.receipty.models.Route
import com.cldnpl.receipty.scanner.CameraController
import com.cldnpl.receipty.scanner.ReceiptReader
import com.cldnpl.receipty.theme.BackButton
import com.cldnpl.receipty.theme.BackdropView
import com.cldnpl.receipty.theme.Face
import com.cldnpl.receipty.theme.Haptics
import com.cldnpl.receipty.theme.Palette
import com.cldnpl.receipty.theme.PrimaryButton
import com.cldnpl.receipty.theme.TightTitle
import com.cldnpl.receipty.theme.Type
import com.cldnpl.receipty.theme.card
import com.cldnpl.receipty.theme.hex
import com.cldnpl.receipty.theme.pressable
import com.cldnpl.receipty.theme.style
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ScanView(app: AppModel, draft: BillDraft) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val camera = remember { CameraController(context) }
    /** La foto che si sta leggendo; null mentre si inquadra. */
    var reading by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }

    val isReading = reading != null
    val hasCamera = camera.status == CameraController.Status.READY

    suspend fun read(image: Bitmap) {
        reading = image
        camera.stop()
        val started = System.currentTimeMillis()
        val parsed = runCatching { ReceiptReader.read(image, draft.currency) }.getOrNull()
        // La lettura è velocissima: l'animazione deve restare il tempo di capire cosa succede.
        val elapsed = System.currentTimeMillis() - started
        if (elapsed < 1100) delay(1100 - elapsed)

        if (parsed == null || parsed.items.isEmpty()) {
            Haptics.warning()
            reading = null
            failed = true
            camera.start(owner)
            return
        }
        Haptics.success()
        draft.source = BillSource.scan
        draft.items.clear()
        draft.items.addAll(parsed.items)
        draft.printedTotal = parsed.total
        app.path.add(Route.Review)
        delay(600)
        reading = null
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val image = ReceiptReader.decode(context, uri)
            if (image != null) read(image) else failed = true
        }
    }
    fun choosePhoto() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scope.launch { camera.start(owner) } else camera.status = CameraController.Status.DENIED
    }

    LaunchedEffect(Unit) {
        when {
            !camera.hasCamera -> camera.status = CameraController.Status.UNAVAILABLE
            camera.permissionGranted -> camera.start(owner)
            else -> permission.launch(Manifest.permission.CAMERA)
        }
    }
    // Tornando dalle impostazioni col permesso appena concesso, la fotocamera parte da sola.
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && camera.status == CameraController.Status.DENIED && camera.permissionGranted) {
                scope.launch { camera.start(owner) }
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            camera.stop()
        }
    }

    fun shoot() {
        if (!hasCamera) {
            choosePhoto()
            return
        }
        Haptics.tap()
        scope.launch {
            val image = runCatching { camera.capture() }.getOrNull()
            if (image != null) read(image) else failed = true
        }
    }

    // Schermata fissa, senza scroll: le posizioni sono quelle del mockup. Su telefoni più bassi
    // si accorcia il mirino, non il resto.
    Box(Modifier.fillMaxSize()) {
        BackdropView()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BackButton(modifier = Modifier.padding(top = 3.5.dp))
            TightTitle(t("scan.title"), Type.scanTitle, Modifier.padding(top = 16.5.dp), textAlign = TextAlign.Center)
            Text(
                t("scan.subtitle"),
                style = Type.lead16,
                color = Palette.inkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 7.5.dp, start = 30.dp, end = 30.dp),
            )

            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 20.5.dp, start = 23.5.dp, end = 23.5.dp, bottom = 18.dp)
            ) {
                val viewfinderHeight = (maxHeight - 68.dp).coerceIn(170.dp, 345.dp)
                Column(Modifier.fillMaxWidth().card()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 9.dp)
                            .height(46.dp)
                            .padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(t("scan.preview"), style = style(Face.semibold, 16f), color = Palette.ink)
                        Spacer(Modifier.weight(1f))
                        val badge = when {
                            isReading -> t("scan.reading")
                            camera.status == CameraController.Status.STARTING -> t("scan.status.starting")
                            camera.status == CameraController.Status.READY -> t("scan.status.ready")
                            else -> t("scan.status.photo")
                        }
                        Box(
                            Modifier
                                .height(24.dp)
                                .background(Palette.pinkSoft, CircleShape)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedContent(badge, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "badge") {
                                Text(it, style = style(Face.semibold, 13.5f), color = Palette.raspberry, maxLines = 1)
                            }
                        }
                    }
                    Viewfinder(
                        camera = camera,
                        reading = reading,
                        onAllowCamera = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            )
                        },
                        onChoosePhoto = ::choosePhoto,
                        modifier = Modifier
                            .padding(start = 14.dp, end = 14.dp, bottom = 13.dp)
                            .fillMaxWidth()
                            .height(viewfinderHeight),
                    )
                }
            }

            Box(Modifier.padding(horizontal = 24.dp)) {
                val enabled = !isReading && camera.status != CameraController.Status.STARTING
                if (hasCamera || isReading) {
                    PrimaryButton(onClick = ::shoot, enabled = enabled) { color ->
                        Icon(Icons.Outlined.PhotoCamera, null, tint = color, modifier = Modifier.size(23.dp))
                        Spacer(Modifier.size(11.dp))
                        Text(if (isReading) t("scan.reading") else t("scan.capture"), style = Type.button, color = color)
                    }
                } else {
                    PrimaryButton(onClick = ::choosePhoto, enabled = enabled) { color ->
                        Icon(Icons.Outlined.Image, null, tint = color, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.size(11.dp))
                        Text(t("scan.choosePhoto"), style = Type.button, color = color)
                    }
                }
            }

            Text(
                t("scan.continueManually"),
                style = Type.link,
                color = Palette.raspberryDeep,
                modifier = Modifier
                    .padding(top = 21.dp, bottom = 3.dp)
                    .pressable(0.96f, enabled = !isReading) { app.typeItemsInstead() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }

    if (failed) {
        AlertDialog(
            onDismissRequest = { failed = false },
            title = { Text(t("scan.failed.title"), style = style(Face.bold, 18f)) },
            text = { Text(t("scan.failed.message"), style = Type.lead16) },
            confirmButton = {
                TextButton(onClick = { failed = false }) { Text(t("scan.failed.retry"), style = Type.pillLabel) }
            },
            dismissButton = {
                TextButton(onClick = {
                    failed = false
                    app.typeItemsInstead()
                }) { Text(t("scan.failed.manual"), style = Type.pillLabel) }
            },
            containerColor = Palette.card,
            titleContentColor = Palette.ink,
            textContentColor = Palette.inkSoft,
        )
    }
}

@Composable
private fun Viewfinder(
    camera: CameraController,
    reading: Bitmap?,
    onAllowCamera: () -> Unit,
    onChoosePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasCamera = camera.status == CameraController.Status.READY
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.previewDark)
    ) {
        AnimatedContent(
            reading,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label = "viewfinder",
        ) { image ->
            Box(Modifier.fillMaxSize()) {
                if (image != null) {
                    Image(image.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    ScanLine()
                } else if (hasCamera) {
                    AndroidView({ camera.previewView }, Modifier.fillMaxSize())
                } else {
                    SampleReceipt(Modifier.fillMaxSize().padding(horizontal = 54.dp, vertical = 31.dp))
                }
            }
        }

        CornerBrackets(Modifier.fillMaxSize().padding(horizontal = 18.5.dp, vertical = 17.5.dp))

        if (camera.status == CameraController.Status.DENIED && reading == null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .height(32.dp)
                    .pressable(0.95f, onClick = onAllowCamera)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(t("scan.allowCamera"), style = style(Face.semibold, 13.5f), color = Color.White)
            }
        }

        if (hasCamera && reading == null) {
            val label = t("scan.choosePhoto")
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .size(38.dp)
                    .pressable(0.92f, onClick = onChoosePhoto)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Gli angoli rosa del mirino. */
@Composable
private fun CornerBrackets(modifier: Modifier) {
    Canvas(modifier) {
        val arm = 36.dp.toPx()
        val r = 9.dp.toPx()
        val w = size.width
        val h = size.height
        val p = Path().apply {
            // alto a sinistra
            moveTo(0f, arm); lineTo(0f, r); quadraticTo(0f, 0f, r, 0f); lineTo(arm, 0f)
            // alto a destra
            moveTo(w - arm, 0f); lineTo(w - r, 0f); quadraticTo(w, 0f, w, r); lineTo(w, arm)
            // basso a destra
            moveTo(w, h - arm); lineTo(w, h - r); quadraticTo(w, h, w - r, h); lineTo(w - arm, h)
            // basso a sinistra
            moveTo(arm, h); lineTo(r, h); quadraticTo(0f, h, 0f, h - r); lineTo(0f, h - arm)
        }
        drawPath(p, Palette.bracket, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** La riga luminosa che scorre mentre si legge lo scontrino. */
@Composable
private fun ScanLine() {
    val transition = rememberInfiniteTransition(label = "scan")
    val t by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "t",
    )
    val pink = Palette.pink
    val bracket = Palette.bracket
    Canvas(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f))) {
        val band = 70.dp.toPx()
        val center = -0f + t * size.height
        val top = center - band / 2
        drawRect(
            Brush.verticalGradient(
                listOf(pink.copy(alpha = 0f), pink.copy(alpha = 0.55f), pink.copy(alpha = 0f)),
                startY = top,
                endY = top + band,
            ),
            topLeft = Offset(0f, top),
            size = Size(size.width, band),
        )
        drawRect(bracket, topLeft = Offset(0f, center - 1.dp.toPx()), size = Size(size.width, 2.dp.toPx()))
    }
}

/**
 * Lo scontrino di esempio del mockup, mostrato quando non c'è una fotocamera da usare.
 * Le due righe sbiadite ricordano che l'OCR a volte sbaglia (e che l'app poi chiede conferma).
 */
@Composable
private fun SampleReceipt(modifier: Modifier) {
    val rows = listOf(
        Triple("MARGHERITA", "8.00", false), Triple("CARBONARA", "12.00", false),
        Triple("DRAFT BEER", "5.00", false), Triple("STILL WATER", "2.00", false),
        Triple("TIRAMIS▒", "6.00", true), Triple("COVER", "7.▒0", false),
    )
    val ink = hex(0x3A3033)
    val mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ink)
    val dashColor = hex(0x9C9296)
    Box(modifier.clearAndSetSemantics { }) {
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = -1.5f }
                .background(Palette.paper)
                .padding(horizontal = 17.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("TRATTORIA DA NINO", style = mono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 20.dp))
            Dashes(dashColor, Modifier.padding(top = 11.dp))
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.5.dp)) {
                for ((name, price, faded) in rows) {
                    Row(Modifier.fillMaxWidth().graphicsLayer { alpha = if (faded) 0.45f else 1f }) {
                        Text(name, style = mono)
                        Spacer(Modifier.weight(1f))
                        Text(price, style = mono)
                    }
                }
            }
            Dashes(dashColor, Modifier.padding(top = 11.dp))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text("TOTAL", style = mono.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.weight(1f))
                Text("40.00", style = mono.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun Dashes(color: Color, modifier: Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                drawLine(
                    color,
                    Offset(0f, size.height / 2),
                    Offset(size.width, size.height / 2),
                    strokeWidth = 0.8.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())),
                )
            }
    )
}
