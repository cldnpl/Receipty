package com.cldnpl.receipty.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.cldnpl.receipty.models.Currency
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Dalla foto alle voci: legge il testo sul telefono (ML Kit con il modello incluso
 * nell'app, niente rete) e passa tutto al parser.
 */
object ReceiptReader {
    class UnreadableImage : Exception()

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun read(image: Bitmap, currency: Currency = Currency.current): ParsedReceipt {
        val digits = currency.minorDigits
        return withContext(Dispatchers.Default) {
            val upright = normalized(image) ?: throw UnreadableImage()
            ReceiptParser.parse(recognize(upright), digits)
        }
    }

    /** Foto non più grande del necessario. */
    fun normalized(image: Bitmap, maxDimension: Int = 2800): Bitmap? {
        val longest = max(image.width, image.height)
        if (longest <= 0) return null
        val scale = min(1f, maxDimension.toFloat() / longest)
        if (scale >= 1f) return image
        return Bitmap.createScaledBitmap(image, (image.width * scale).roundToInt(), (image.height * scale).roundToInt(), true)
    }

    suspend fun recognize(image: Bitmap): List<OCRFragment> {
        val text: Text = suspendCancellableCoroutine { cont ->
            recognizer.process(InputImage.fromBitmap(image, 0))
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        return text.textBlocks.flatMap { block ->
            block.lines.mapNotNull { line ->
                val r = line.boundingBox ?: return@mapNotNull null
                OCRFragment(
                    text = line.text,
                    confidence = line.confidence,
                    box = Box(r.left.toFloat(), r.top.toFloat(), r.width().toFloat(), r.height().toFloat()),
                    angle = Math.toRadians(line.angle.toDouble()).toFloat(),
                )
            }
        }
    }

    /** Una foto scelta dalla galleria, dritta (l'orientamento EXIF applicato). */
    suspend fun decode(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            // Prima solo le dimensioni, per non caricare in memoria foto da 50 megapixel.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 2800) sample *= 2
            val bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null
            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
            rotate(bitmap, degrees)
        }.getOrNull()
    }

    fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
