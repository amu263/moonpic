package dev.moonpic.feature.editor.transforms

/**
 * Viewport transform applied to the image bitmap. Independent of the crop
 * rect: while the crop rect selects which part of the image to keep, the
 * viewport decides how the image is *displayed* in the editor canvas.
 *
 * - [scale] is a uniform scale factor; 1f = fit-to-canvas, 2f = 2× zoom.
 * - [offsetX]/[offsetY] are pixel offsets applied after the scale, in the
 *   composable's local coordinate space (the canvas). They let the user
 *   pan the zoomed image around.
 *
 * The transform is applied via `Modifier.graphicsLayer` on the underlying
 * `Image` composable. The crop rect is drawn in canvas space and is
 * unaffected by this transform — so the user can pinch-zoom to inspect
 * detail while still seeing the crop overlay sit on top.
 */
data class ImageTransform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    companion object {
        val Identity = ImageTransform(1f, 0f, 0f)
    }
}
