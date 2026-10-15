package com.ownfit.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.ownfit.app.data.ThemeMode

/** Gruvbox accent colours for the current mode (bright on dark, faded on light). */
@Immutable
data class Gruvbox(
    val red: Color, val green: Color, val yellow: Color, val blue: Color,
    val purple: Color, val aqua: Color, val orange: Color, val gray: Color,
)

private val GruvboxDark = Gruvbox(
    red = Color(0xFFFB4934), green = Color(0xFFB8BB26), yellow = Color(0xFFFABD2F), blue = Color(0xFF83A598),
    purple = Color(0xFFD3869B), aqua = Color(0xFF8EC07C), orange = Color(0xFFFE8019), gray = Color(0xFF928374),
)

private val GruvboxLight = Gruvbox(
    red = Color(0xFF9D0006), green = Color(0xFF79740E), yellow = Color(0xFFB57614), blue = Color(0xFF076678),
    purple = Color(0xFF8F3F71), aqua = Color(0xFF427B58), orange = Color(0xFFAF3A03), gray = Color(0xFF7C6F64),
)

val LocalGruvbox = staticCompositionLocalOf { GruvboxDark }

/** Shorthand: `gruvbox().aqua` inside composables. */
@Composable
fun gruvbox(): Gruvbox = LocalGruvbox.current

private val Dark = darkColorScheme(
    primary = Color(0xFFFABD2F), onPrimary = Color(0xFF282828),
    primaryContainer = Color(0xFF79621C), onPrimaryContainer = Color(0xFFFBF1C7),
    inversePrimary = Color(0xFFB57614),
    secondary = Color(0xFF8EC07C), onSecondary = Color(0xFF282828),
    secondaryContainer = Color(0xFF3F5A3B), onSecondaryContainer = Color(0xFFEBDBB2),
    tertiary = Color(0xFFFE8019), onTertiary = Color(0xFF282828),
    tertiaryContainer = Color(0xFF7A3B10), onTertiaryContainer = Color(0xFFFBF1C7),
    error = Color(0xFFFB4934), onError = Color(0xFF282828),
    errorContainer = Color(0xFF9D0006), onErrorContainer = Color(0xFFFBF1C7),
    background = Color(0xFF282828), onBackground = Color(0xFFEBDBB2),
    surface = Color(0xFF282828), onSurface = Color(0xFFEBDBB2),
    surfaceVariant = Color(0xFF3C3836), onSurfaceVariant = Color(0xFFBDAE93),
    surfaceTint = Color(0xFFFABD2F),
    inverseSurface = Color(0xFFEBDBB2), inverseOnSurface = Color(0xFF282828),
    outline = Color(0xFF928374), outlineVariant = Color(0xFF504945),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF504945), surfaceDim = Color(0xFF1D2021),
    surfaceContainerLowest = Color(0xFF1D2021), surfaceContainerLow = Color(0xFF32302F),
    surfaceContainer = Color(0xFF3C3836), surfaceContainerHigh = Color(0xFF45403D),
    surfaceContainerHighest = Color(0xFF504945),
)

private val Light = lightColorScheme(
    primary = Color(0xFFB57614), onPrimary = Color(0xFFFBF1C7),
    primaryContainer = Color(0xFFF5D88F), onPrimaryContainer = Color(0xFF3C3836),
    inversePrimary = Color(0xFFFABD2F),
    secondary = Color(0xFF427B58), onSecondary = Color(0xFFFBF1C7),
    secondaryContainer = Color(0xFFC9DDB2), onSecondaryContainer = Color(0xFF282828),
    tertiary = Color(0xFFAF3A03), onTertiary = Color(0xFFFBF1C7),
    tertiaryContainer = Color(0xFFF7C79A), onTertiaryContainer = Color(0xFF282828),
    error = Color(0xFF9D0006), onError = Color(0xFFFBF1C7),
    errorContainer = Color(0xFFF2B8AE), onErrorContainer = Color(0xFF3C3836),
    background = Color(0xFFFBF1C7), onBackground = Color(0xFF3C3836),
    surface = Color(0xFFFBF1C7), onSurface = Color(0xFF3C3836),
    surfaceVariant = Color(0xFFEBDBB2), onSurfaceVariant = Color(0xFF665C54),
    surfaceTint = Color(0xFFB57614),
    inverseSurface = Color(0xFF3C3836), inverseOnSurface = Color(0xFFFBF1C7),
    outline = Color(0xFF928374), outlineVariant = Color(0xFFD5C4A1),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF9F5D7), surfaceDim = Color(0xFFEBDBB2),
    surfaceContainerLowest = Color(0xFFF9F5D7), surfaceContainerLow = Color(0xFFF2E5BC),
    surfaceContainer = Color(0xFFEBDBB2), surfaceContainerHigh = Color(0xFFE5D3A8),
    surfaceContainerHighest = Color(0xFFD5C4A1),
)

private val Type = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontWeight = FontWeight.Bold),
        displayMedium = t.displayMedium.copy(fontWeight = FontWeight.Bold),
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    )
}

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun OwnFitTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = isDark(mode)
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(LocalGruvbox provides if (dark) GruvboxDark else GruvboxLight) {
        MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Type, content = content)
    }
}
