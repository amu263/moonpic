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

/**
 * The 8 directional resize handles + Move + None.
 *
 * The hit-testing is built so the corner boxes and the edge strips together
 * cover a "L"-shaped region around the entire rect, so the user can grab any
 * side from inside OR outside the rect. The interior of the rect (after the
 * edge strips) is reserved for Move. Outside the corner/edge regions is
 * None — so stray touches on the dimmed area don't grab the rect.
 */
private enum class Handle { TL, T, TR, R, BR, B, BL, L, Move, None }

private val MoonViolet = Color(0xFF7C4DFF)

/**
 * 12 canvas-px ≈ 4dp on a 3x device, big enough to see but small enough not
 * to cover the image. The hit-testing below uses a much larger region.
 */
private const val HANDLE_RADIUS_PX = 12f

/**
 * Half-size of a corner hit box. 60f means a 120×120 box centred on the
 * corner pixel — finger-friendly (≈40dp on 3x) and straddling the rect edge
 * so touches from either side of the corner pick the corner handle.
 */
private const val CORNER_HIT_HALF = 60f

/**
 * Half-width of an edge hit strip. 30f means a 60px-wide strip straddling
 * the rect edge (≈20dp on 3x). Edge strips are clamped to NOT overlap
 * with the corner hit boxes.
 */
private const val EDGE_HIT_HALF = 30f

/**
 * Minimum crop size in image-pixels. Anything smaller becomes a 1×1 crop
 * when we round to int at mapCanvasToImage, which is useless.
 */
private const val MIN_CROP_IMG_PX = 16f

/**
 * Draws a dimmed-out overlay with a draggable, resizable crop rectangle in
 * image-space coordinates (0,0)-(imageWidth,imageHeight) scaled to the canvas.
 *
 * The "dim outside / show inside" effect is implemented as four filled
 * rectangles around the crop rect. We avoid BlendMode.Clear because Clear
 * behaves unreliably with Compose Canvas compositing — the four-strip
 * approach is foolproof.
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
    val imageBounds = remember(fit, imageSize) { imageBoundsOnCanvas(fit, imageSize) }

    var rect by remember(crop, fit) {
        mutableStateOf<Rect>(
            crop?.let { mapImageToCanvas(it, fit) }
                ?: Rect(0f, 0f, imageSize.width.toFloat(), imageSize.height.toFloat())
                    .let { mapImageToCanvas(CropRect(0, 0, imageSize.width, imageSize.height), fit) },
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
                    if (active == Handle.None) return@detectDragGestures
                    val dx = change.position.x - lastPos.x
                    val dy = change.position.y - lastPos.y
                    lastPos = change.position
                    rect = applyDrag(rect, active, dx, dy, fit, imageBounds)
                    onCropChange(mapCanvasToImage(rect, fit))
                }
            },
    ) {
        val dim = Color.Black.copy(alpha = 0.55f)

        // Four strips: top / bottom / left / right. Together they cover
        // exactly the area outside the crop rect, so the inside stays
        // fully visible.
        if (rect.top > 0f) {
            drawRect(
                color = dim,
                topLeft = Offset(0f, 0f),
                size = Size(size.width, rect.top),
            )
        }
        if (rect.bottom < size.height) {
            drawRect(
                color = dim,
                topLeft = Offset(0f, rect.bottom),
                size = Size(size.width, size.height - rect.bottom),
            )
        }
        if (rect.left > 0f) {
            drawRect(
                color = dim,
                topLeft = Offset(0f, rect.top),
                size = Size(rect.left, rect.height),
            )
        }
        if (rect.right < size.width) {
            drawRect(
                color = dim,
                topLeft = Offset(rect.right, rect.top),
                size = Size(size.width - rect.right, rect.height),
            )
        }

        // Rule-of-thirds grid — clipped to the crop rect so the lines
        // never spill into the dimmed area.
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

        // Bright border around the crop rect.
        drawRect(
            color = Color.White,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(width = 3f),
        )

        // Corner handles. The active corner is drawn larger and fully
        // opaque as visual feedback that the touch was registered.
        for (h in listOf(Handle.TL, Handle.TR, Handle.BL, Handle.BR)) {
            val center = when (h) {
                Handle.TL -> rect.topLeft
                Handle.TR -> rect.topRight
                Handle.BL -> rect.bottomLeft
                Handle.BR -> rect.bottomRight
                else -> Offset.Zero
            }
            val r = if (active == h) HANDLE_RADIUS_PX + 4f else HANDLE_RADIUS_PX
            val dotR = if (active == h) HANDLE_RADIUS_PX - 3f else HANDLE_RADIUS_PX - 5f
            drawCircle(
                Color.White.copy(alpha = if (active == h) 0.95f else 0.85f),
                radius = r,
                center = center,
            )
            drawCircle(
                MoonViolet.copy(alpha = if (active == h) 1f else 0.9f),
                radius = dotR,
                center = center,
            )
        }

        // Mid-edge pips — small visual hint that the edges are also
        // draggable. Non-interactive (the actual hit area is a 60px strip
        // around each edge).
        val edgePipColor = if (active in listOf(Handle.T, Handle.B, Handle.L, Handle.R)) {
            MoonViolet
        } else {
            Color.White.copy(alpha = 0.85f)
        }
        drawCircle(
            edgePipColor,
            radius = if (active in listOf(Handle.T, Handle.B, Handle.L, Handle.R)) 9f else 6f,
            center = Offset((rect.left + rect.right) / 2f, rect.top),
        )
        drawCircle(
            edgePipColor,
            radius = if (active in listOf(Handle.T, Handle.B, Handle.L, Handle.R)) 9f else 6f,
            center = Offset((rect.left + rect.right) / 2f, rect.bottom),
        )
        drawCircle(
            edgePipColor,
            radius = if (active in listOf(Handle.T, Handle.B, Handle.L, Handle.R)) 9f else 6f,
            center = Offset(rect.left, (rect.top + rect.bottom) / 2f),
        )
        drawCircle(
            edgePipColor,
            radius = if (active in listOf(Handle.T, Handle.B, Handle.L, Handle.R)) 9f else 6f,
            center = Offset(rect.right, (rect.top + rect.bottom) / 2f),
        )
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
 * Hit-testing for the 8 directional handles + Move.
 *
 * Corners are 120×120 boxes straddling the corner pixel. Edge strips are
 * 60px wide, also straddling the edge, but clamped to NOT overlap the
 * corner boxes. The rect interior (after subtracting the edge strips) is
 * Move. Anything else is None.
 */
private fun pickHandle(off: Offset, rect: Rect): Handle {
    // Corner hit zones (square, centred on the corner pixel).
    if (off.x in (rect.left - CORNER_HIT_HALF)..(rect.left + CORNER_HIT_HALF) &&
        off.y in (rect.top - CORNER_HIT_HALF)..(rect.top + CORNER_HIT_HALF)
    ) return Handle.TL
    if (off.x in (rect.right - CORNER_HIT_HALF)..(rect.right + CORNER_HIT_HALF) &&
        off.y in (rect.top - CORNER_HIT_HALF)..(rect.top + CORNER_HIT_HALF)
    ) return Handle.TR
    if (off.x in (rect.left - CORNER_HIT_HALF)..(rect.left + CORNER_HIT_HALF) &&
        off.y in (rect.bottom - CORNER_HIT_HALF)..(rect.bottom + CORNER_HIT_HALF)
    ) return Handle.BL
    if (off.x in (rect.right - CORNER_HIT_HALF)..(rect.right + CORNER_HIT_HALF) &&
        off.y in (rect.bottom - CORNER_HIT_HALF)..(rect.bottom + CORNER_HIT_HALF)
    ) return Handle.BR

    // Edge hit strips (not overlapping the corner boxes).
    if (off.y in (rect.top - EDGE_HIT_HALF)..(rect.top + EDGE_HIT_HALF) &&
        off.x in (rect.left + CORNER_HIT_HALF)..(rect.right - CORNER_HIT_HALF)
    ) return Handle.T
    if (off.y in (rect.bottom - EDGE_HIT_HALF)..(rect.bottom + EDGE_HIT_HALF) &&
        off.x in (rect.left + CORNER_HIT_HALF)..(rect.right - CORNER_HIT_HALF)
    ) return Handle.B
    if (off.x in (rect.left - EDGE_HIT_HALF)..(rect.left + EDGE_HIT_HALF) &&
        off.y in (rect.top + CORNER_HIT_HALF)..(rect.bottom - CORNER_HIT_HALF)
    ) return Handle.L
    if (off.x in (rect.right - EDGE_HIT_HALF)..(rect.right + EDGE_HIT_HALF) &&
        off.y in (rect.top + CORNER_HIT_HALF)..(rect.bottom - CORNER_HIT_HALF)
    ) return Handle.R

    // Interior of the rect is Move.
    if (rect.contains(off)) return Handle.Move

    return Handle.None
}

private fun applyDrag(
    rect: Rect,
    h: Handle,
    dx: Float,
    dy: Float,
    fit: Fit,
    imageBounds: Rect,
): Rect {
    val minSize = MIN_CROP_IMG_PX * fit.scale
    return when (h) {
        Handle.None -> rect

        // Whole-rect translation, clamped so the rect stays inside the
        // image bounds. Without this clamp, a Move could drag the crop
        // rect completely off the image, which is confusing.
        Handle.Move -> {
            val maxLeft = imageBounds.right - rect.width
            val maxTop = imageBounds.bottom - rect.height
            val newLeft = (rect.left + dx).coerceIn(imageBounds.left, maxLeft)
            val newTop = (rect.top + dy).coerceIn(imageBounds.top, maxTop)
            Rect(newLeft, newTop, newLeft + rect.width, newTop + rect.height)
        }

        // Corner handles: move two edges (one horizontal + one vertical).
        Handle.TL -> {
            val nl = (rect.left + dx).coerceAtMost(rect.right - minSize)
            val nt = (rect.top + dy).coerceAtMost(rect.bottom - minSize)
            Rect(nl, nt, rect.right, rect.bottom)
        }
        Handle.TR -> {
            val nt = (rect.top + dy).coerceAtMost(rect.bottom - minSize)
            val nr = max(rect.left + minSize, rect.right + dx)
            Rect(rect.left, nt, nr, rect.bottom)
        }
        Handle.BR -> {
            val nr = max(rect.left + minSize, rect.right + dx)
            val nb = max(rect.top + minSize, rect.bottom + dy)
            Rect(rect.left, rect.top, nr, nb)
        }
        Handle.BL -> {
            val nl = (rect.left + dx).coerceAtMost(rect.right - minSize)
            val nb = max(rect.top + minSize, rect.bottom + dy)
            Rect(nl, rect.top, rect.right, nb)
        }

        // Edge handles: move one edge, keep the opposite edge fixed.
        Handle.T -> {
            val nt = (rect.top + dy).coerceAtMost(rect.bottom - minSize)
            Rect(rect.left, nt, rect.right, rect.bottom)
        }
        Handle.B -> {
            val nb = max(rect.top + minSize, rect.bottom + dy)
            Rect(rect.left, rect.top, rect.right, nb)
        }
        Handle.L -> {
            val nl = (rect.left + dx).coerceAtMost(rect.right - minSize)
            Rect(nl, rect.top, rect.right, rect.bottom)
        }
        Handle.R -> {
            val nr = max(rect.left + minSize, rect.right + dx)
            Rect(rect.left, rect.top, nr, rect.bottom)
        }
    }
}
