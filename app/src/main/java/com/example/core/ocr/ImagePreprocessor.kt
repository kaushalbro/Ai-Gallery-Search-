package com.example.core.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import kotlin.math.max

data class ProcessedImage(
    val bitmap: Bitmap,
    val originalWidth: Int,
    val originalHeight: Int,
    val processedWidth: Int,
    val processedHeight: Int
)

object ImagePreprocessor {

    fun loadAndPreprocess(
        context: Context,
        uriString: String,
        maxDimension: Int = 3200
    ): ProcessedImage? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = if (uriString.startsWith("file://") || uriString.startsWith("/")) {
                val path = if (uriString.startsWith("file://")) uri.path ?: uriString.removePrefix("file://") else uriString
                File(path).inputStream()
            } else {
                context.contentResolver.openInputStream(uri)
            }

            if (inputStream == null) return null

            // 1. Decode bounds
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            val tempBytes = inputStream.readBytes()
            BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, options)

            val origWidth = options.outWidth
            val origHeight = options.outHeight

            if (origWidth <= 0 || origHeight <= 0) return null

            // 2. Calculate sample size (safe power of 2 for sharp desktop/form clarity)
            var sampleSize = 1
            val maxSide = max(origWidth, origHeight)
            while ((maxSide / (sampleSize * 2)) >= maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var decodedBitmap = BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, decodeOptions) ?: return null

            // 3. EXIF orientation handling
            val exifStream = tempBytes.inputStream()
            val exif = ExifInterface(exifStream)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                val rotated = Bitmap.createBitmap(decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, true)
                if (rotated != decodedBitmap) {
                    decodedBitmap.recycle()
                    decodedBitmap = rotated
                }
            }

            ProcessedImage(
                bitmap = decodedBitmap,
                originalWidth = origWidth,
                originalHeight = origHeight,
                processedWidth = decodedBitmap.width,
                processedHeight = decodedBitmap.height
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
