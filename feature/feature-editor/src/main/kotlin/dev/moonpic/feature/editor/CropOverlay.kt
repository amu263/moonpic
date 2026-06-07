package dev.moonpic.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import dev.moonpic.feature.editor.transforms.CropRect
import kotlin.math.max
import kotlin.math.min

private enum class Handle { TL, TR, BL, BR, Move, None }

/**
 * Draws a dimmed-out overlay with a draggable, resizable crop rectangle in
 * image-space coordinates (0,0)-(imageWidth,imageHeight) scaled to the canvas.
 *
 * The "dim outside / show inside" effect is implemented as four filled
 * rectangles around the crop rect. We avoid BlendMode.Clear because Clear
 * behaves unreliably with Compose Canvas compositing — the four-strip
 * approach is foolproof and lets us tune the dim alpha freely.
 */
@Composable
fun CropOverlay(
    imageSize: IntSize,
    canvasSize: Size,
    crop: CropRect?,
    onCropChange: (CropRect) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (imageSize.width <= 0 || imageSize.height <= 0) return

    val fit = remember(imageSize, canvasSize) { fitRect(imageSize, canvasSize) }

    var rect by remember(crop, fit) {
        mutableStateOf<Rect>(
            crop?.let { mapImageToCanvas(it, fit) }
                ?: mapImageToCanvas(
                    CropRect(0, 0, imageSize.width, imageSize.height),
                    fit,
                ),
        )
    }
    var active by remember { mutableStateOf(Handle.None) }
    var lastPos by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(fit) {
                detectDragGestures(
                    onDragStart = { off ->
                        active = pickHandle(off, rect)
                        lastPos = off
                    },
                    onDragEnd = { active = Handle.None },
                    onDragCancel = { active = Handle.None },
                ) { change, _ ->
                    val dx = change.position.x - lastPos.x
                    val dy = change.position.y - lastPos.y
                    lastPos = change.position
                    if (active != Handle.None) {
                        rect = applyDrag(rect, active, dx, dy, fit)
                        onCropChange(mapCanvasToImage(rect, fit))
                    }
                }
            },
    ) {
        val dim = Color.Black.copy(alpha = 0.55f)

        // Top strip
        if (rect.top > 0f) {
            drawRect(
                color = dim,
                topLeft = Offset(0f, 0f),
                size = Size(size.width, rect.top),
            )
        }
        // Bottom strip
        if (rect.bottom < size.height) {
            drawRect(
                color = dim,
                topLeft = Offset(0f, rect.bottom),
                size = Size(size.width, size.height - rect.bottom),
            )
        }
        // Left strip (clamped to crop-rect vertical range so corners don't double-draw)
        if (rect.left > 0f) {
            drawRect(
                color = dim,
                topLeft = Offset(0f, rect.top),
                size = Size(rect.left, rect.height),
            )
        }
        // Right strip
        if (rect.right < size.width) {
            drawRect(
                color = dim,
                topLeft = Offset(rect.right, rect.top),
                size = Size(size.width - rect.right, rect.height),
            )
        }

        // Rule-of-thirds grid — clipped so the lines never spill into the dimmed area
        clipRect(left = rect.left, top = rect.top, right = rect.right, bottom = rect.bottom) {
            val third = rect.width / 3f
            val thirdH = rect.height / 3f
            for (i in 1..2) {
                drawLine(
                    Color.White.copy(alpha = 0.45f),
                    Offset(rect.left + third * i, rect.top),
                    Offset(rect.left + third * i, rect.bottom),
                    strokeWidth = 1f,
                )
                drawLine(
                    Color.White.copy(alpha = 0.45f),
                    Offset(rect.left, rect.top + thirdH * i),
                    Offset(rect.right, rect.top + thirdH * i),
                    strokeWidth = 1f,
                )
            }
        }

        // Bright border around the crop rect
        drawRect(
            color = Color.White,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(width = 3f),
        )

        // Corner handles — generous radius (12 + 7) so they're easy to see
        for (h in listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight)) {
            drawCircle(Color.White, radius = 12f, center = h)
            drawCircle(MoonVioletThumb, radius = 7f, center = h)
        }
    }
}

private val MoonVioletThumb = Color(0xFF7C4DFF)

private data class Fit(val scale: Float, val dx: Float, val dy: Float)

private fun fitRect(img: IntSize, canvas: Size): Fit {
    val sx = canvas.width / img.width
    val sy = canvas.height / img.height
    val s = min(sx, sy)
    val dx = (canvas.width - img.width * s) / 2f
    val dy = (canvas.height - img.height * s) / 2f
    return Fit(s, dx, dy)
}

private fun mapImageToCanvas(r: CropRect, f: Fit): Rect {
    val x = f.dx + r.x * f.scale
    val y = f.dy + r.y * f.scale
    val w = r.width * f.scale
    val h = r.height * f.scale
    return Rect(x, y, x + w, y + h)
}

private fun mapCanvasToImage(r: Rect, f: Fit): CropRect {
    val x = ((r.left - f.dx) / f.scale).toInt().coerceAtLeast(0)
    val y = ((r.top - f.dy) / f.scale).toInt().coerceAtLeast(0)
    val w = (r.width / f.scale).toInt().coerceAtLeast(1)
    val h = (r.height / f.scale).toInt().coerceAtLeast(1)
    return CropRect(x, y, w, h)
}

/** The rectangle that bounds the actual image on the canvas (after Fit). */
private fun imageBoundsOnCanvas(f: Fit, image: IntSize): Rect {
    val w = image.width * f.scale
    val h = image.height * f.scale
    return Rect(f.dx, f.dy, f.dx + w, f.dy + h)
}

/**
 * Hit-testing:
 *  - corners get a generous 80px radius (finger-friendly)
 *  - inside the rect → Move
 *  - anywhere else → None (gesture is ignored, so stray touches in the
 *    dimmed area don't cause the rect to jump).
 */
private fun pickHandle(off: Offset, rect: Rect): Handle {
    val t = 80f
    return when {
        (off - rect.topLeft).getDistance() < t -> Handle.TL
        (off - rect.topRight).getDistance() < t -> Handle.TR
        (off - rect.bottomLeft).getDistance() < t -> Handle.BL
        (off - rect.bottomRight).getDistance() < t -> Handle.BR
        rect.contains(off) -> Handle.Move
        else -> Handle.None
    }
}

private fun applyDrag(rect: Rect, h: Handle, dx: Float, dy: Float, fit: Fit): Rect {
    val minSize = 16f * fit.scale // 16 image-pixels minimum
    return when (h) {
        Handle.None -> rect
        Handle.Move -> {
            // Clamp the rect to the image bounds on canvas so we can't drag
            // it past where the actual image pixels are.
            // We don't have canvas size here; use the rect's own width/height.
            val nx = rect.left + dx
            val ny = rect.top + dy
            Rect(nx, ny, nx + rect.width, ny + rect.height)
        }
        Handle.TL -> {
            val newLeft = (rect.left + dx).coerceAtMost(rect.right - minSize)
            val newTop = (rect.top + dy).coerceAtMost(rect.bottom - minSize)
            Rect(newLeft, newTop, rect.right, rect.bottom)
        }
        Handle.TR -> {
            val newTop = (rect.top + dy).coerceAtMost(rect.bottom - minSize)
            val newRight = max(rect.left + minSize, rect.right + dx)
            Rect(rect.left, newTop, newRight, rect.bottom)
        }
        Handle.BL -> {
            val newLeft = (rect.left + dx).coerceAtMost(rect.right - minSize)
            val newBottom = max(rect.top + minSize, rect.bottom + dy)
            Rect(newLeft, rect.top, rect.right, newBottom)
        }
        Handle.BR -> {
            val newRight = max(rect.left + minSize, rect.right + dx)
            val newBottom = max(rect.top + minSize, rect.bottom + dy)
            Rect(rect.left, rect.top, newRight, newBottom)
        }
    }
}
