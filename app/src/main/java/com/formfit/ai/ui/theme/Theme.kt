package com.formfit.ai.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FitFormDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryVariant,
    onPrimaryContainer = TextPrimary,

    secondary = PrimaryVariant,
    onSecondary = OnPrimary,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = TextPrimary,

    tertiary = WarningYellow,
    onTertiary = BackgroundDark,

    background = BackgroundDark,
    onBackground = TextPrimary,

    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,

    error = ErrorRed,
    onError = TextPrimary,

    outline = DividerColor,
    outlineVariant = SurfaceVariantDark,

    inverseSurface = TextPrimary,
    inverseOnSurface = BackgroundDark,
    inversePrimary = PrimaryVariant,

    scrim = BackgroundDark
)

@Composable
fun FitFormAITheme(content: @Composable () -> Unit) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar arka planını uygulama arka planıyla eşleştir
            window.statusBarColor = BackgroundDark.toArgb()
            // Status bar ikonlarını açık renk yap (koyu arka plan üzerinde)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = FitFormDarkColorScheme,
        typography = Typography,
        content = content
    )
}
