package dev.moonpic.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min

private enum class Handle { TL, TR, BL, BR, Move, None }

/**
 * Draws a dimmed-out overlay with a draggable, resizable crop rectangle in
 * image-space coordinates (0,0)-(imageWidth,imageHeight) scaled to the canvas.
 */
@Composable
fun CropOverlay(
    imageSize: IntSize,
    canvasSize: Size,
    crop: dev.moonpic.feature.editor.transforms.CropRect?,
    onCropChange: (dev.moonpic.feature.editor.transforms.CropRect) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (imageSize.width <= 0 || imageSize.height <= 0) return

    val fit = remember(imageSize, canvasSize) { fitRect(imageSize, canvasSize) }
    var rect by remember(crop, fit) {
        mutableStateOf<Rect>(
            crop?.let { mapImageToCanvas(it, fit) }
                ?: mapImageToCanvas(
                    dev.moonpic.feature.editor.transforms.CropRect(0, 0, imageSize.width, imageSize.height),
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
                detectTapGestures { off -> active = pickHandle(off, rect) }
            }
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
                    rect = applyDrag(rect, active, dx, dy, fit)
                    onCropChange(mapCanvasToImage(rect, fit))
                }
            },
    ) {
        // dim outside
        val outer = Path().apply { addRect(Rect(Offset.Zero, size)) }
        val inner = Path().apply { addRect(rect) }
        drawPath(outer, Color.Black.copy(alpha = 0.5f))
        drawPath(inner, Color.Transparent, blendMode = BlendMode.Clear)
        drawPath(outer, Color.Black.copy(alpha = 0.5f))
        // border + grid
        drawRect(color = Color.White, topLeft = rect.topLeft, size = rect.size, style = Stroke(width = 2f))
        val third = rect.width / 3f
        val tH = rect.height / 3f
        for (i in 1..2) {
            drawLine(Color.White.copy(alpha = 0.4f), Offset(rect.left + third * i, rect.top), Offset(rect.left + third * i, rect.bottom), 1f)
            drawLine(Color.White.copy(alpha = 0.4f), Offset(rect.left, rect.top + tH * i), Offset(rect.right, rect.top + tH * i), 1f)
        }
        // corner handles
        for (h in listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight)) {
            drawCircle(Color.White, radius = 8f, center = h)
            drawCircle(Color.Black, radius = 5f, center = h)
        }
    }
}

private data class Fit(val scale: Float, val dx: Float, val dy: Float)

private fun fitRect(img: IntSize, canvas: Size): Fit {
    val sx = canvas.width / img.width
    val sy = canvas.height / img.height
    val s = min(sx, sy)
    val dx = (canvas.width - img.width * s) / 2f
    val dy = (canvas.height - img.height * s) / 2f
    return Fit(s, dx, dy)
}

private fun mapImageToCanvas(r: dev.moonpic.feature.editor.transforms.CropRect, f: Fit): Rect {
    val x = f.dx + r.x * f.scale
    val y = f.dy + r.y * f.scale
    val w = r.width * f.scale
    val h = r.height * f.scale
    return Rect(x, y, x + w, y + h)
}

private fun mapCanvasToImage(r: Rect, f: Fit): dev.moonpic.feature.editor.transforms.CropRect {
    val x = ((r.left - f.dx) / f.scale).toInt().coerceAtLeast(0)
    val y = ((r.top - f.dy) / f.scale).toInt().coerceAtLeast(0)
    val w = (r.width / f.scale).toInt().coerceAtLeast(1)
    val h = (r.height / f.scale).toInt().coerceAtLeast(1)
    return dev.moonpic.feature.editor.transforms.CropRect(x, y, w, h)
}

private fun pickHandle(off: Offset, rect: Rect): Handle {
    val t = 28f
    return when {
        (off - rect.topLeft).getDistance() < t -> Handle.TL
        (off - rect.topRight).getDistance() < t -> Handle.TR
        (off - rect.bottomLeft).getDistance() < t -> Handle.BL
        (off - rect.bottomRight).getDistance() < t -> Handle.BR
        rect.contains(off) -> Handle.Move
        else -> Handle.None
    }
}

private fun applyDrag(rect: Rect, h: Handle, dx: Float, dy: Float, fit: Fit): Rect = when (h) {
    Handle.None -> rect
    Handle.Move -> Rect(rect.left + dx, rect.top + dy, rect.right + dx, rect.bottom + dy)
    Handle.TL -> Rect(
        (rect.left + dx).coerceAtMost(rect.right - 20f),
        (rect.top + dy).coerceAtMost(rect.bottom - 20f),
        rect.right,
        rect.bottom,
    )
    Handle.TR -> Rect(
        rect.left,
        (rect.top + dy).coerceAtMost(rect.bottom - 20f),
        (rect.right + dx).coerceAtLeast(rect.left + 20f),
        rect.bottom,
    )
    Handle.BL -> Rect(
        (rect.left + dx).coerceAtMost(rect.right - 20f),
        rect.top,
        rect.right,
        (rect.bottom + dy).coerceAtLeast(rect.top + 20f),
    )
    Handle.BR -> Rect(
        rect.left,
        rect.top,
        max(rect.left + 20f, rect.right + dx),
        max(rect.top + 20f, rect.bottom + dy),
    )
}
