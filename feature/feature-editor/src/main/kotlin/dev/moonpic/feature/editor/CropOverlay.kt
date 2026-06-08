package dev.moonpic.feature.editor

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import dev.moonpic.feature.editor.transforms.CropRect
import dev.moonpic.feature.editor.transforms.ImageTransform
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 8 directional resize handles + Move + None.
 *
 * The hit-testing is built so the corner boxes and the edge strips together
 * cover an "L"-shaped region around the entire rect:
 *  - Corners are 200×200 boxes straddling the corner pixel (user can grab
 *    from inside OR outside the rect).
 *  - Edges are 160px-wide strips straddling the edge, clamped to not
 *    overlap with the corner boxes.
 *  - The deep interior is Move.
 *  - Outside everything is None.
 */
private enum class Handle { TL, T, TR, R, BR, B, BL, L, Move, None }

private val MoonViolet = Color(0xFF7C4DFF)
private val DimColor = Color.Black.copy(alpha = 0.55f)

/** Visible handle radius (px). On 3x device ≈ 6dp — visible but unobtrusive. */
private const val HANDLE_RADIUS_PX = 18f
private const val HANDLE_DOT_PX = 9f

/** Half-size of a corner hit box. 100f → 200×200 box. On 3x ≈ 66dp. */
private const val CORNER_HIT_HALF = 100f

/**
 * Half-width of an edge hit strip. 80f → 160px strip straddling the edge.
 * On 3x ≈ 53dp. Big enough that finger touches on (or near) the visible
 * border reliably land on the edge, not on the interior Move zone.
 */
private const val EDGE_HIT_HALF = 80f

/** Minimum crop size in image-pixels, prevents collapse. */
private const val MIN_CROP_IMG_PX = 16f

/**
 * Width of the dim-fade transition zone (px). Within this distance from
 * the rect edge the dim alpha ramps from 0 (at the edge) to 0.55. Beyond
 * this distance the dim stays at 0.55. Gives the crop a soft, photo-editor
 * "lens" feel rather than a hard punched-out look.
 */
private const val DIM_FADE_PX = 40f

private const val MIN_SCALE = 0.5f
private const val MAX_SCALE = 5f

/**
 * Crop overlay with two responsibilities:
 *
 * 1. **Crop UI** (8 handles + Move). Single-finger drag on a handle
 *    resizes the crop; single-finger drag inside the rect moves it.
 * 2. **Image viewport**. Two-finger pinch zooms; two-finger pan translates.
 *
 * Both gesture modes live in a single `awaitPointerEventScope` loop, so
 * switching between 1-finger and 2-finger is atomic (no event is dropped
 * during the transition). Per the v0.1.3 fix, crop changes are
 * committed to the parent only in `onDragEnd` to avoid recomposition
 * jitter; viewport changes are committed on every frame because they
 * don't suffer from the same roundtrip issue.
 */
@Composable
fun CropOverlay(
    imageSize: IntSize,
    canvasSize: Size,
    crop: CropRect?,
    imageTransform: ImageTransform,
    onCropChange: (CropRect) -> Unit,
    onImageTransformChange: (ImageTransform) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (imageSize.width <= 0 || imageSize.height <= 0) return

    val fit = remember(imageSize, canvasSize) { fitRect(imageSize, canvasSize) }
    val imageBounds = remember(fit, imageSize) { imageBoundsOnCanvas(fit, imageSize) }

    var rect by remember(fit) {
        mutableStateOf<Rect>(
            crop?.let { mapImageToCanvas(it, fit) }
                ?: mapImageToCanvas(
                    CropRect(0, 0, imageSize.width, imageSize.height),
                    fit,
                ),
        )
    }
    var localTransform by remember { mutableStateOf(imageTransform) }
    var active by remember { mutableStateOf(Handle.None) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(fit, imageBounds) {
                awaitPointerEventScope {
                    var lastPos: Offset? = null
                    var lastCentroid: Offset? = null
                    var lastDistance: Float? = null

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }

                        if (pressed.isEmpty()) {
                            // Drag end: commit any pending change.
                            if (active != Handle.None) {
                                onCropChange(mapCanvasToImage(rect, fit))
                            }
                            if (lastCentroid != null) {
                                onImageTransformChange(localTransform)
                            }
                            lastPos = null
                            lastCentroid = null
                            lastDistance = null
                            active = Handle.None
                        } else if (pressed.size >= 2) {
                            // Two-finger viewport gesture: centroid gives
                            // pan delta, distance ratio gives zoom.
                            val centroid = (pressed[0].position + pressed[1].position) / 2f
                            val distance = (pressed[0].position - pressed[1].position).getDistance()
                            if (lastCentroid != null && lastDistance != null &&
                                lastDistance!! > 0.1f
                            ) {
                                val panDelta = centroid - lastCentroid!!
                                val zoom = distance / lastDistance!!
                                localTransform = localTransform.copy(
                                    scale = (localTransform.scale * zoom)
                                        .coerceIn(MIN_SCALE, MAX_SCALE),
                                    offsetX = localTransform.offsetX + panDelta.x,
                                    offsetY = localTransform.offsetY + panDelta.y,
                                )
                                onImageTransformChange(localTransform)
                            }
                            lastCentroid = centroid
                            lastDistance = distance
                            // Switch out of any single-finger crop state.
                            lastPos = null
                            active = Handle.None
                        } else {
                            // Single-finger: crop handle.
                            val p = pressed[0]
                            if (lastPos == null) {
                                active = pickHandle(p.position, rect)
                                lastPos = p.position
                            } else {
                                val dx = p.position.x - lastPos!!.x
                                val dy = p.position.y - lastPos!!.y
                                lastPos = p.position
                                if (active != Handle.None) {
                                    rect = applyDrag(rect, active, dx, dy, fit, imageBounds)
                                }
                            }
                            // Switch out of any two-finger viewport state.
                            lastCentroid = null
                            lastDistance = null
                        }

                        // Consume so the events don't leak to siblings
                        // (in case we ever add a sibling gesture detector).
                        for (change in event.changes) {
                            if (change.pressed) change.consume()
                        }
                    }
                }
            },
    ) {
        drawDimAndCrop(rect, active)
    }
}

private fun DrawScope.drawDimAndCrop(rect: Rect, active: Handle) {
    // Gradient dim — top / bottom / left / right strips. Each strip fades
    // from Color.Transparent at the rect edge to DimColor over DIM_FADE_PX,
    // then stays at DimColor out to the canvas edge. This makes the crop
    // feel like a "soft lens" rather than a hard punched-out hole.

    if (rect.top > 0f) {
        drawRect(
            brush = verticalDimBrush(rect.top, 0f),
            topLeft = Offset(0f, 0f),
            size = Size(size.width, rect.top),
        )
    }
    if (rect.bottom < size.height) {
        drawRect(
            brush = verticalDimBrush(rect.bottom, size.height),
            topLeft = Offset(0f, rect.bottom),
            size = Size(size.width, size.height - rect.bottom),
        )
    }
    if (rect.left > 0f) {
        drawRect(
            brush = horizontalDimBrush(rect.left, 0f),
            topLeft = Offset(0f, rect.top),
            size = Size(rect.left, rect.height),
        )
    }
    if (rect.right < size.width) {
        drawRect(
            brush = horizontalDimBrush(rect.right, size.width),
            topLeft = Offset(rect.right, rect.top),
            size = Size(size.width - rect.right, rect.height),
        )
    }

    // Rule-of-thirds grid — clipped to the crop rect.
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

    // Corner handles. The active corner is drawn larger / fully opaque
    // as touch feedback.
    for (h in listOf(Handle.TL, Handle.TR, Handle.BL, Handle.BR)) {
        val center = when (h) {
            Handle.TL -> rect.topLeft
            Handle.TR -> rect.topRight
            Handle.BL -> rect.bottomLeft
            Handle.BR -> rect.bottomRight
            else -> Offset.Zero
        }
        val isActive = active == h
        drawCircle(
            Color.White.copy(alpha = if (isActive) 1f else 0.85f),
            radius = if (isActive) HANDLE_RADIUS_PX + 4f else HANDLE_RADIUS_PX,
            center = center,
        )
        drawCircle(
            MoonViolet.copy(alpha = if (isActive) 1f else 0.9f),
            radius = if (isActive) HANDLE_DOT_PX + 2f else HANDLE_DOT_PX,
            center = center,
        )
    }

    // Mid-edge pips. Active edge is drawn larger / violet as feedback.
    val anyEdgeActive = active in listOf(Handle.T, Handle.B, Handle.L, Handle.R)
    for ((edge, center) in listOf(
        Handle.T to Offset((rect.left + rect.right) / 2f, rect.top),
        Handle.B to Offset((rect.left + rect.right) / 2f, rect.bottom),
        Handle.L to Offset(rect.left, (rect.top + rect.bottom) / 2f),
        Handle.R to Offset(rect.right, (rect.top + rect.bottom) / 2f),
    )) {
        val isActive = active == edge
        drawCircle(
            if (isActive) MoonViolet else Color.White.copy(alpha = 0.85f),
            radius = if (isActive || anyEdgeActive) 11f else 7f,
            center = center,
        )
    }
}

private fun verticalDimBrush(edgeY: Float, endY: Float): Brush {
    val h = abs(endY - edgeY)
    val stops: Array<Pair<Float, Color>> = if (h > DIM_FADE_PX) {
        arrayOf(
            0f to Color.Transparent,            // at edgeY
            (DIM_FADE_PX / h) to DimColor,      // at edgeY ± DIM_FADE_PX
            1f to DimColor,                     // at endY
        )
    } else {
        arrayOf(
            0f to Color.Transparent,
            1f to DimColor,
        )
    }
    return Brush.verticalGradient(
        colorStops = stops,
        startY = edgeY,
        endY = endY,
    )
}

private fun horizontalDimBrush(edgeX: Float, endX: Float): Brush {
    val w = abs(endX - edgeX)
    val stops: Array<Pair<Float, Color>> = if (w > DIM_FADE_PX) {
        arrayOf(
            0f to Color.Transparent,
            (DIM_FADE_PX / w) to DimColor,
            1f to DimColor,
        )
    } else {
        arrayOf(
            0f to Color.Transparent,
            1f to DimColor,
        )
    }
    return Brush.horizontalGradient(
        colorStops = stops,
        startX = edgeX,
        endX = endX,
    )
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
 * Corners are 200×200 boxes straddling the corner pixel. Edge strips are
 * 160px wide, also straddling the edge, but clamped to NOT overlap the
 * corner boxes (the corner check has higher priority, so corners always
 * win in the overlap). The deep interior of the rect is Move. Outside
 * everything is None.
 */
private fun pickHandle(off: Offset, rect: Rect): Handle {
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
