package com.cashflow.ai.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

object ImagePreprocessor {

    private const val MAX_DIMENSION = 2048
    private const val JPEG_QUALITY = 85

    /**
     * Decodes, corrects rotation from EXIF, and resizes bitmap to max 2048px from a Uri.
     */
    suspend fun loadAndOptimizeBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_DIMENSION
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // First decode bounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null

            // Calculate sample size
            options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, maxDimension)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            } ?: return@withContext null

            // Handle orientation
            val orientation = getExifOrientation(context, uri)
            val rotatedBitmap = rotateBitmapIfRequired(sampledBitmap, orientation)

            // Scale to exact max dimension if still larger
            scaleBitmapDown(rotatedBitmap, maxDimension)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes and optimizes bitmap from a File.
     */
    suspend fun loadAndOptimizeBitmap(
        file: File,
        maxDimension: Int = MAX_DIMENSION
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext null

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)

            if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null

            options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, maxDimension)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            val sampledBitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return@withContext null

            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val rotatedBitmap = rotateBitmapIfRequired(sampledBitmap, orientation)

            scaleBitmapDown(rotatedBitmap, maxDimension)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Enhances contrast and converts to grayscale for higher ML Kit OCR accuracy.
     */
    fun enhanceForOcr(src: Bitmap, contrast: Float = 1.3f, brightness: Float = -10f): Bitmap {
        val width = src.width
        val height = src.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Grayscale matrix
        val grayMatrix = ColorMatrix().apply {
            setSaturation(0f)
        }

        // 2. Contrast & Brightness adjustment matrix
        // scale = contrast, translate = brightness
        val scale = contrast
        val translate = (1f - scale) * 128f + brightness
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        grayMatrix.postConcat(contrastMatrix)
        paint.colorFilter = ColorMatrixColorFilter(grayMatrix)
        canvas.drawBitmap(src, 0f, 0f, paint)

        return outputBitmap
    }

    /**
     * Saves a bitmap to the application's cache directory and returns the File.
     */
    suspend fun saveBitmapToCache(
        context: Context,
        bitmap: Bitmap,
        filenamePrefix: String = "receipt_"
    ): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "receipt_images").apply {
            if (!exists()) mkdirs()
        }
        val file = File(cacheDir, "${filenamePrefix}${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        file
    }

    fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        val maxDim = max(width, height)
        while (maxDim / inSampleSize > maxDimension) {
            inSampleSize *= 2
        }
        return max(1, inSampleSize)
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxCurrent = max(width, height)

        if (maxCurrent <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / maxCurrent.toFloat()
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    fun rotateBitmapIfRequired(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        val rotated = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
        if (rotated != bitmap && !bitmap.isRecycled) {
            bitmap.recycle()
        }
        return rotated
    }
}
