package dev.moonpic.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max

/**
 * Load a [Bitmap] from a content [Uri] with a sample size chosen so neither
 * dimension exceeds [maxEdge]. EXIF orientation is applied so the result is
 * always upright.
 */
fun Context.decodeSampledBitmap(uri: Uri, maxEdge: Int = 4096): Bitmap? {
    // Pass 1 — bounds only.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val sample = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge)

    // Pass 2 — real decode.
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val raw = contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts)
    } ?: return null

    val orientation = readExifOrientation(uri)
    return if (orientation != ExifInterface.ORIENTATION_NORMAL) {
        applyExifOrientation(raw, orientation).also { if (it !== raw) raw.recycle() }
    } else raw
}

private fun sampleSize(w: Int, h: Int, maxEdge: Int): Int {
    var sample = 1
    var cw = w; var ch = h
    while (max(cw, ch) / 2 >= maxEdge) {
        sample *= 2
        cw /= 2
        ch /= 2
    }
    return sample
}

private fun Context.readExifOrientation(uri: Uri): Int = runCatching {
    contentResolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(
        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
    ) } ?: ExifInterface.ORIENTATION_NORMAL
}.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.preScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.preScale(-1f, 1f) }
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
