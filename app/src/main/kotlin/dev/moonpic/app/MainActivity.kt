package dev.moonpic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.moonpic.core.ui.theme.MoonPicTheme
import dev.moonpic.feature.editor.editorScreen
import dev.moonpic.feature.filters.lutImportScreen
import dev.moonpic.feature.main.mainScreen

/**
 * Single-activity host. All navigation lives here; feature modules expose
 * NavGraphBuilder extension functions and we wire them in.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoonPicTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

private object Routes {
    const val MAIN = "main"
    const val EDITOR = "editor/{uri}"
    const val LUT_IMPORT = "lut/import"
    fun editor(uri: String) = "editor/${java.net.URLEncoder.encode(uri, "UTF-8")}"
}

@Composable
private fun AppRoot() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            mainScreen(
                onOpenEditor = { uri -> navController.navigate(Routes.editor(uri)) },
                onOpenLutImport = { navController.navigate(Routes.LUT_IMPORT) },
            )
        }
        composable(Routes.EDITOR) { backStackEntry ->
            val raw = backStackEntry.arguments?.getString("uri") ?: return@composable
            val uri = java.net.URLDecoder.decode(raw, "UTF-8")
            editorScreen(
                sourceUri = uri,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.LUT_IMPORT) {
            lutImportScreen(onBack = { navController.popBackStack() })
        }
    }
}
