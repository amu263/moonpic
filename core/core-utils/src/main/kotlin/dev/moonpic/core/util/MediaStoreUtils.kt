package dev.moonpic.core.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream

/**
 * Save a [Bitmap] into the public Pictures/MoonPic collection via MediaStore so
 * the result shows up in the system gallery without requiring WRITE_EXTERNAL_STORAGE.
 *
 * @return the content:// Uri of the saved image, or null on failure.
 */
fun Context.saveBitmapToPictures(
    bitmap: Bitmap,
    displayName: String,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    quality: Int = 95,
): Uri? {
    val mime = when (format) {
        Bitmap.CompressFormat.PNG -> "image/png"
        Bitmap.CompressFormat.JPEG -> "image/jpeg"
        Bitmap.CompressFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "image/webp" else "image/png"
        else -> "image/png"
    }

    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, mime)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/MoonPic",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    val resolver = contentResolver
    val uri = resolver.insert(collection, values) ?: return null

    return runCatching {
        resolver.openOutputStream(uri)?.use { out: OutputStream ->
            bitmap.compress(format, quality, out)
        } ?: error("openOutputStream returned null")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        uri
    }.getOrElse {
        resolver.delete(uri, null, null)
        null
    }
}
