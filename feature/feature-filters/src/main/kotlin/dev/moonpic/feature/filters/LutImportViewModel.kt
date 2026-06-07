package dev.moonpic.feature.filters

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.moonpic.core.data.LutRepository
import dev.moonpic.core.domain.LutSummary
import dev.moonpic.core.filters.lut.CubeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import javax.inject.Inject

data class LutImportState(
    val isImporting: Boolean = false,
    val lastMessage: String? = null,
)

@HiltViewModel
class LutImportViewModel @Inject constructor(
    application: Application,
    private val repo: LutRepository,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(LutImportState())
    val state: StateFlow<LutImportState> = _state.asStateFlow()

    val luts: StateFlow<List<LutSummary>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun importCube(uri: Uri) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true, lastMessage = null) }
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val name = uri.lastPathSegment?.substringAfterLast('/') ?: "lut.cube"
                    val displayName = name.substringBeforeLast('.', name)

                    val parsed = ctx.contentResolver.openInputStream(uri)?.use { input ->
                        CubeParser.parse(BufferedReader(InputStreamReader(input, Charsets.UTF_8)))
                    } ?: error("could not open .cube file")

                    val raw = parsed.getOrThrow()
                    val lutDir = File(ctx.filesDir, "luts").apply { mkdirs() }
                    val target = File(lutDir, "${System.currentTimeMillis()}_$displayName.cube")
                    ctx.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(target).use { out -> input.copyTo(out) }
                    }
                    repo.import(
                        name = displayName,
                        title = raw.title.ifBlank { displayName },
                        size = raw.size,
                        domainMin = raw.domainMin,
                        domainMax = raw.domainMax,
                        rawPath = target.absolutePath,
                    )
                    "Imported ${raw.title.ifBlank { displayName }} (${raw.size}³)"
                }
            }
            _state.update {
                it.copy(
                    isImporting = false,
                    lastMessage = outcome.getOrElse { e -> "Failed: ${e.message ?: "unknown"}" },
                )
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun clearMessage() {
        _state.update { it.copy(lastMessage = null) }
    }
}
