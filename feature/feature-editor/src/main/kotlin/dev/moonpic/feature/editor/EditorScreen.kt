package dev.moonpic.feature.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.moonpic.feature.editor.transforms.CropRect
import dev.moonpic.feature.editor.transforms.RotationDeg

/**
 * Public entry point used by app's NavHost.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun editorScreen(
    sourceUri: String,
    onBack: () -> Unit,
) {
    val vm: EditorViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(sourceUri) {
        if (sourceUri.isNotBlank()) vm.loadSource(sourceUri)
    }
    LaunchedEffect(state.message) {
        state.message?.let { snack.showSnackbar(it) }
    }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        uri?.let { vm.loadSource(it.toString()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vm.save() },
                        enabled = state.sourceBitmap != null && !state.isWorking,
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            CanvasArea(
                state = state,
                onCropChange = vm::setCrop,
                onPick = {
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
            Toolbar(
                state = state,
                onRotateLeft = { vm.rotate(RotationDeg.D270) },
                onRotateRight = { vm.rotate(RotationDeg.D90) },
                onMaxEdgeChange = vm::setMaxEdge,
            )
        }
    }
}

@Composable
private fun CanvasArea(
    state: EditorState,
    onCropChange: (CropRect) -> Unit,
    onPick: () -> Unit,
) {
    val bmp = state.sourceBitmap
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .onSizeChanged { canvasSize = it },
        contentAlignment = Alignment.Center,
    ) {
        if (bmp == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "No image loaded",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                AssistChip(
                    onClick = onPick,
                    label = { Text("Pick image") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        labelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        } else {
            val display = state.previewBitmap ?: bmp
            Image(
                bitmap = display.asImageBitmap(),
                contentDescription = "Editing preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )
            val canvasSizeF = androidx.compose.ui.geometry.Size(
                canvasSize.width.toFloat(),
                canvasSize.height.toFloat(),
            )
            if (canvasSize.width > 0) {
                CropOverlay(
                    imageSize = IntSize(display.width, display.height),
                    canvasSize = canvasSizeF,
                    crop = state.crop,
                    onCropChange = onCropChange,
                )
            }
        }
        if (state.isWorking) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun Toolbar(
    state: EditorState,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onMaxEdgeChange: (Int) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Tune, contentDescription = null)
                Text("Transform", style = MaterialTheme.typography.titleMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onRotateLeft, label = { Text("⟲ 90°") })
                AssistChip(onClick = onRotateRight, label = { Text("⟳ 90°") })
                AssistChip(
                    onClick = {},
                    label = { Text("Crop (drag the box)") },
                    leadingIcon = { Icon(Icons.Outlined.Crop, null) },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Text("  Max edge: ${state.maxEdge}px", style = MaterialTheme.typography.bodyMedium)
            }
            Slider(
                value = state.maxEdge.toFloat(),
                onValueChange = { onMaxEdgeChange(it.toInt()) },
                valueRange = 512f..8192f,
                steps = 14,
            )
        }
    }
}
