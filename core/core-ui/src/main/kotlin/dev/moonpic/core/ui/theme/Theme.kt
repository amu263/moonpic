package dev.moonpic.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = MoonVioletDeep,
    onPrimary = Cloud,
    primaryContainer = MoonViolet,
    onPrimaryContainer = MidnightBlue,
    secondary = MoonAmber,
    onSecondary = MidnightBlue,
    background = Cloud,
    onBackground = MidnightBlue,
    surface = Cloud,
    onSurface = MidnightBlue,
    surfaceVariant = MoonMist,
    onSurfaceVariant = MidnightBlue,
    error = ErrorRed,
    onError = Cloud,
)

private val DarkColors = darkColorScheme(
    primary = MoonViolet,
    onPrimary = MidnightBlue,
    primaryContainer = MoonVioletDeep,
    onPrimaryContainer = Cloud,
    secondary = MoonAmber,
    onSecondary = MidnightBlue,
    background = MidnightBlue,
    onBackground = Cloud,
    surface = MidnightBlue,
    onSurface = Cloud,
    surfaceVariant = MidnightBlueLight,
    onSurfaceVariant = MoonMist,
    error = ErrorRed,
    onError = Cloud,
)

/**
 * App-wide Material 3 theme. Uses Material You (dynamic) colors on Android 12+
 * when [dynamicColor] is true, falling back to the curated moonlit palette.
 */
@Composable
fun MoonPicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MoonPicTypography,
        content = content,
    )
}
