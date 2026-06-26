package com.budging.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = AppPrimary,
    onPrimary = Color.White,
    primaryContainer = AppPrimarySoft,
    onPrimaryContainer = AppPrimary,
    secondary = AppSecondary,
    onSecondary = Color.White,
    secondaryContainer = AppSecondarySoft,
    onSecondaryContainer = AppPrimary,
    tertiary = AppWarm,
    onTertiary = Color.White,
    tertiaryContainer = AppWarmSoft,
    onTertiaryContainer = AppWarm,
    background = AppBackground,
    onBackground = AppText,
    surface = AppSurface,
    onSurface = AppText,
    surfaceVariant = AppSurfaceLow,
    onSurfaceVariant = AppMutedText,
    outline = AppOutline,
    error = AppDanger,
    errorContainer = AppDangerSoft,
)

private val DarkColors = darkColorScheme(
    primary = AppPrimarySoft,
    onPrimary = AppPrimary,
    secondary = AppSecondarySoft,
    onSecondary = AppPrimary,
    background = Color(0xFF131317),
    onBackground = Color(0xFFF4F1F3),
    surface = Color(0xFF1A1A1F),
    onSurface = Color(0xFFF4F1F3),
    surfaceVariant = Color(0xFF2A2A31),
    onSurfaceVariant = Color(0xFFC9C4CB),
    outline = Color(0xFF4B4850),
    error = Color(0xFFFFB4AB),
)

val BudgingShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

@Immutable
data class BudgingSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

val LocalBudgingSpacing = staticCompositionLocalOf { BudgingSpacing() }

@Composable
fun BudgingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors: ColorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalBudgingSpacing provides BudgingSpacing()) {
        MaterialTheme(
            colorScheme = colors,
            typography = BudgingTypography,
            shapes = BudgingShapes,
            content = content,
        )
    }
}

object BudgingTheme {
    val spacing: BudgingSpacing
        @Composable
        get() = LocalBudgingSpacing.current
}
