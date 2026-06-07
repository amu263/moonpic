package dev.moonpic.feature.editor

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.moonpic.core.util.decodeSampledBitmap
import dev.moonpic.core.util.saveBitmapToPictures
import dev.moonpic.feature.editor.transforms.CropRect
import dev.moonpic.feature.editor.transforms.ImageTransforms
import dev.moonpic.feature.editor.transforms.RotationDeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max

data class EditorState(
    val sourceUri: Uri? = null,
    val sourceBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val rotation: RotationDeg = RotationDeg.D0,
    val crop: CropRect? = null,
    val maxEdge: Int = 4096,
    val isWorking: Boolean = false,
    val message: String? = null,
    val savedUri: Uri? = null,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    application: Application,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val resolver: ContentResolver
        get() = getApplication<Application>().contentResolver

    fun loadSource(rawUri: String?) {
        val uri = rawUri?.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: return
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, message = null) }
            val bmp = withContext(Dispatchers.IO) {
                ctx.decodeSampledBitmap(uri, maxEdge = 4096)
            }
            if (bmp == null) {
                _state.update { it.copy(isWorking = false, message = "Could not decode image") }
                return@launch
            }
            _state.update {
                it.copy(
                    sourceUri = uri,
                    sourceBitmap = bmp,
                    previewBitmap = bmp,
                    rotation = RotationDeg.D0,
                    crop = null,
                    isWorking = false,
                )
            }
        }
    }

    fun pickImageFromSystem() {
        // Photo Picker (Android 13+) or ACTION_OPEN_DOCUMENT fallback handled by
        // the host Activity via ActivityResultContracts. The ViewModel just
        // receives the resulting Uri via loadSource().
        // (UI code in EditorScreen owns the launcher.)
    }

    fun rotate(by: RotationDeg) {
        val src = _state.value.sourceBitmap ?: return
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            val next = withContext(Dispatchers.IO) { ImageTransforms.rotate(src, by) }
            _state.update {
                it.copy(
                    sourceBitmap = next,
                    previewBitmap = next,
                    rotation = it.rotation.combine(by),
                    crop = null,
                    isWorking = false,
                )
            }
        }
    }

    fun setCrop(rect: CropRect?) {
        _state.update { it.copy(crop = rect) }
        applyPreview()
    }

    fun setMaxEdge(edge: Int) {
        _state.update { it.copy(maxEdge = edge.coerceIn(64, 8192)) }
        applyPreview()
    }

    private fun applyPreview() {
        val src = _state.value.sourceBitmap ?: return
        val crop = _state.value.crop
        val maxEdge = _state.value.maxEdge
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            val preview = withContext(Dispatchers.IO) {
                var bmp = src
                if (crop != null) bmp = bmp.applyCrop(crop)
                if (bmp.width > maxEdge || bmp.height > maxEdge) bmp = bmp.applyResize(maxEdge = maxEdge)
                bmp
            }
            _state.update { it.copy(previewBitmap = preview, isWorking = false) }
        }
    }

    fun save() {
        val src = _state.value.sourceBitmap ?: return
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, message = null) }
            val uri = withContext(Dispatchers.IO) {
                var bmp = src
                _state.value.crop?.let { bmp = bmp.applyCrop(it) }
                if (bmp.width > _state.value.maxEdge || bmp.height > _state.value.maxEdge) {
                    bmp = bmp.applyResize(maxEdge = _state.value.maxEdge)
                }
                getApplication<Application>().saveBitmapToPictures(
                    bitmap = bmp,
                    displayName = "moonpic_${System.currentTimeMillis()}.png",
                )
            }
            _state.update {
                it.copy(
                    isWorking = false,
                    savedUri = uri,
                    message = if (uri != null) "Saved to Pictures/MoonPic" else "Save failed",
                )
            }
        }
    }
}

private fun Bitmap.applyCrop(c: CropRect): Bitmap = ImageTransforms.crop(this, c)
private fun Bitmap.applyResize(maxEdge: Int): Bitmap = ImageTransforms.resize(this, maxEdge = maxEdge)
private fun Bitmap.applyRotation(deg: Float): Bitmap = ImageTransforms.rotate(this, deg)
