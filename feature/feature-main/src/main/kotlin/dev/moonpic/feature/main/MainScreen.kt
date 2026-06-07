package dev.moonpic.feature.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewComfy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.moonpic.core.ui.components.FeatureCard

const val MAIN_ROUTE = "main"

data class Feature(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun mainScreen(
    onOpenEditor: (uri: String) -> Unit,
    onOpenLutImport: () -> Unit,
) {
    val features = remember(onOpenEditor, onOpenLutImport) {
        buildFeatures(onOpenEditor, onOpenLutImport)
    }
    MainScreenContent(features = features)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContent(features: List<Feature>) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "🌙 MoonPic", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { /* TODO: open settings */ }) {
                        Icon(imageVector = Icons.Outlined.Tune, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(features, key = { it.id }) { f ->
                FeatureCard(
                    title = f.title,
                    subtitle = f.subtitle,
                    icon = f.icon,
                    onClick = f.onClick,
                    enabled = f.enabled,
                )
            }
        }
    }
}

private fun buildFeatures(
    onOpenEditor: (String) -> Unit,
    onOpenLutImport: () -> Unit,
): List<Feature> = listOf(
    Feature("edit", "Edit", "Pick → crop, rotate, resize", Icons.Outlined.Crop, true) { onOpenEditor("") },
    Feature("filters", "Filters & LUT", "310+ filters, import .cube", Icons.Outlined.Palette, true, onOpenLutImport),
    Feature("camera", "Camera", "Coming soon — live LUT", Icons.Outlined.CameraAlt, false) {},
    Feature("scan", "Document scan", "Edge detect + perspective", Icons.Outlined.Description, false) {},
    Feature("ocr", "OCR", "120+ languages, offline", Icons.Outlined.LibraryBooks, false) {},
    Feature("pdf", "PDF tools", "Convert, search, extract", Icons.Outlined.PictureAsPdf, false) {},
    Feature("gif", "GIF", "Make / extract frames", Icons.Outlined.AutoAwesome, false) {},
    Feature("collage", "Collage", "Grid, freeform, mosaic", Icons.Outlined.GridView, false) {},
    Feature("batch", "Batch", "Process many at once", Icons.Outlined.ViewComfy, false) {},
    Feature("ai", "AI", "Background, upscale, denoise", Icons.Outlined.Brush, false) {},
    Feature("watermark", "Watermark", "Text / image, batch", Icons.Outlined.PhotoLibrary, false) {},
    Feature("qr", "QR & barcode", "Scan + generate", Icons.Outlined.QrCodeScanner, false) {},
)
