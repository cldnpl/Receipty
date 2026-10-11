package com.cldnpl.receipty.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** La fotocamera dentro la card "Anteprima": si inquadra lo scontrino senza lasciare la schermata. */
class CameraController(private val context: Context) {
    enum class Status { STARTING, READY, UNAVAILABLE, DENIED }

    var status by mutableStateOf(Status.STARTING)

    val previewView: PreviewView = PreviewView(context).apply {
        // TextureView: si lascia ritagliare dagli angoli arrotondati della card.
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        scaleType = PreviewView.ScaleType.FILL_CENTER
    }

    private var provider: ProcessCameraProvider? = null
    private val preview = Preview.Builder().build()
    private val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    val hasCamera: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    val permissionGranted: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    suspend fun start(owner: LifecycleOwner) {
        if (!hasCamera) {
            status = Status.UNAVAILABLE
            return
        }
        if (!permissionGranted) {
            status = Status.DENIED
            return
        }
        val provider = runCatching { awaitProvider() }.getOrNull()
        if (provider == null) {
            status = Status.UNAVAILABLE
            return
        }
        this.provider = provider
        status = runCatching {
            provider.unbindAll()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            Status.READY
        }.getOrDefault(Status.UNAVAILABLE)
    }

    fun stop() {
        provider?.unbindAll()
    }

    private suspend fun awaitProvider(): ProcessCameraProvider = suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching { future.get() }
                .onSuccess { cont.resume(it) }
                .onFailure { cont.resumeWithException(it) }
        }, ContextCompat.getMainExecutor(context))
    }

    suspend fun capture(): Bitmap = suspendCancellableCoroutine { cont ->
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val result = runCatching {
                        val bitmap = image.toBitmap()
                        ReceiptReader.rotate(bitmap, image.imageInfo.rotationDegrees)
                    }
                    image.close()
                    result.onSuccess { cont.resume(it) }.onFailure { cont.resumeWithException(it) }
                }

                override fun onError(exception: ImageCaptureException) {
                    cont.resumeWithException(exception)
                }
            },
        )
    }
}
