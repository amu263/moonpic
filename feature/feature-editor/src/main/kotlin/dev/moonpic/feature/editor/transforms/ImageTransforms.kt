package dev.moonpic.feature.editor.transforms

import android.graphics.Bitmap
import android.graphics.Matrix
import kotlin.math.max

enum class RotationDeg(val deg: Float) {
    D0(0f), D90(90f), D180(180f), D270(270f);

    fun combine(other: RotationDeg): RotationDeg {
        val sum = (deg + other.deg).mod(360f)
        return entries.first { it.deg == sum }
    }
}

/**
 * Crop rectangle in source-bitmap pixel coordinates. All values are integers
 * in the inclusive range [0, source.width-1] × [0, source.height-1].
 */
data class CropRect(val x: Int, val y: Int, val width: Int, val height: Int) {
    init {
        require(width > 0 && height > 0) { "non-positive crop size" }
    }
}

object ImageTransforms {

    fun rotate(src: Bitmap, by: RotationDeg): Bitmap {
        if (by == RotationDeg.D0) return src
        val matrix = Matrix().apply { postRotate(by.deg) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    fun rotate(src: Bitmap, deg: Float): Bitmap {
        if (deg.mod(360f) == 0f) return src
        val matrix = Matrix().apply { postRotate(deg) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    fun crop(src: Bitmap, rect: CropRect): Bitmap {
        val safe = CropRect(
            x = rect.x.coerceIn(0, src.width - 1),
            y = rect.y.coerceIn(0, src.height - 1),
            width = rect.width.coerceAtMost(src.width - rect.x).coerceAtLeast(1),
            height = rect.height.coerceAtMost(src.height - rect.y).coerceAtLeast(1),
        )
        return Bitmap.createBitmap(src, safe.x, safe.y, safe.width, safe.height)
    }

    /**
     * Resize so the longer edge equals [maxEdge], preserving aspect ratio.
     */
    fun resize(src: Bitmap, maxEdge: Int): Bitmap {
        val longer = max(src.width, src.height)
        if (longer <= maxEdge) return src
        val scale = maxEdge.toFloat() / longer
        val nw = (src.width * scale).toInt().coerceAtLeast(1)
        val nh = (src.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, nw, nh, true)
    }
}
