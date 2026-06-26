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
    onPrimaryContainer = AppInkBlue,
    secondary = AppSecondary,
    onSecondary = Color.White,
    secondaryContainer = AppSecondarySoft,
    onSecondaryContainer = AppInkBlue,
    tertiary = AppSecondary,
    onTertiary = Color.White,
    tertiaryContainer = AppSecondarySoft,
    onTertiaryContainer = AppInkBlue,
    background = AppBackground,
    onBackground = AppText,
    surface = AppSurface,
    onSurface = AppText,
    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = AppMutedText,
    outline = AppOutline,
    error = AppDanger,
    errorContainer = AppDangerSoft,
)

private val DarkColors = darkColorScheme(
    primary = AppPrimarySoft,
    onPrimary = AppInkBlue,
    primaryContainer = Color(0xFF213149),
    onPrimaryContainer = Color(0xFFE3ECFF),
    secondary = AppSecondarySoft,
    onSecondary = AppInkBlue,
    secondaryContainer = Color(0xFF2B3747),
    onSecondaryContainer = Color(0xFFE3ECFF),
    background = Color(0xFF10141B),
    onBackground = Color(0xFFF2F5FA),
    surface = Color(0xFF171C24),
    onSurface = Color(0xFFF2F5FA),
    surfaceVariant = Color(0xFF232A35),
    onSurfaceVariant = Color(0xFFBEC6D3),
    outline = Color(0xFF445062),
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
