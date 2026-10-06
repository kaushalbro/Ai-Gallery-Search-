package com.example.core.scanner

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.data.model.GalleryAsset
import com.example.data.model.IndexingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object MediaStoreScanner {

    fun hasGalleryPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 34) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED") == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    suspend fun scanDeviceImages(context: Context, limit: Int = 2000): List<GalleryAsset> = withContext(Dispatchers.IO) {
        val assets = mutableListOf<GalleryAsset>()

        val collection: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED
        )

        // Note: Do not put LIMIT in sortOrder string as it causes IllegalArgumentException on Android 10+
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)

                while (cursor.moveToNext() && assets.size < limit) {
                    if (idCol < 0) continue
                    val id = cursor.getLong(idCol)
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "image_$id.jpg" else "image_$id.jpg"
                    val bucket = if (bucketCol >= 0) cursor.getString(bucketCol) ?: "Gallery" else "Gallery"
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "image/jpeg" else "image/jpeg"
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val dateAddedSec = if (dateAddedCol >= 0) cursor.getLong(dateAddedCol) else System.currentTimeMillis() / 1000
                    val dateModSec = if (dateModCol >= 0) cursor.getLong(dateModCol) else dateAddedSec

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val isScreenshot = isScreenshotAsset(name, bucket)

                    assets.add(
                        GalleryAsset(
                            platformAssetId = "mediastore_$id",
                            uri = contentUri.toString(),
                            filename = name,
                            albumName = bucket,
                            mimeType = mime,
                            width = width,
                            height = height,
                            fileSize = size,
                            createdAt = dateAddedSec * 1000L,
                            modifiedAt = dateModSec * 1000L,
                            isScreenshot = isScreenshot,
                            indexingStatus = IndexingStatus.DISCOVERED
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        assets
    }

    private fun isScreenshotAsset(filename: String, albumName: String): Boolean {
        val lowerName = filename.lowercase(Locale.ROOT)
        val lowerAlbum = albumName.lowercase(Locale.ROOT)
        return lowerAlbum.contains("screenshot") ||
                lowerAlbum.contains("screencapture") ||
                lowerName.startsWith("screenshot") ||
                lowerName.contains("screenshot_") ||
                lowerName.startsWith("screen_")
    }
}
