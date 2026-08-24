package com.cashflow.ai.core.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraManager(
    private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var currentLensFacing = CameraSelector.LENS_FACING_BACK
    private var currentFlashMode = ImageCapture.FLASH_MODE_OFF
    private var isTorchOn = false

    suspend fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) = withContext(Dispatchers.Main) {
        val provider = getCameraProvider()
        cameraProvider = provider

        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(currentFlashMode)
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(currentLensFacing)
            .build()

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun capturePhoto(outputFile: File): Result<File> = suspendCancellableCoroutine { cont ->
        val capture = imageCapture
        if (capture == null) {
            cont.resume(Result.failure(IllegalStateException("ImageCapture not initialized")))
            return@suspendCancellableCoroutine
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        val executor = ContextCompat.getMainExecutor(context)

        capture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    if (cont.isActive) {
                        cont.resume(Result.success(outputFile))
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    if (cont.isActive) {
                        cont.resume(Result.failure(exception))
                    }
                }
            }
        )
    }

    fun setFlashMode(flashMode: Int) {
        currentFlashMode = flashMode
        imageCapture?.flashMode = flashMode
    }

    fun toggleTorch(): Boolean {
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                isTorchOn = !isTorchOn
                cam.cameraControl.enableTorch(isTorchOn)
                return isTorchOn
            }
        }
        return false
    }

    fun toggleCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        currentLensFacing = if (currentLensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        val provider = cameraProvider ?: return
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val selector = CameraSelector.Builder().requireLensFacing(currentLensFacing).build()

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider = suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                try {
                    cont.resume(future.get())
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }
}
