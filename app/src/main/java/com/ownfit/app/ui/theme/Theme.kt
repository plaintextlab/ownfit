package com.ownfit.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ownfit.app.data.ThemeMode

private val Light = lightColorScheme(
    primary = Color(0xFF1F6B4C), onPrimary = Color.White,
    primaryContainer = Color(0xFFA8F2CA), onPrimaryContainer = Color(0xFF002113),
    secondary = Color(0xFF4D6357), secondaryContainer = Color(0xFFCFE9D8),
    tertiary = Color(0xFF3D6373), tertiaryContainer = Color(0xFFC1E8FB),
    background = Color(0xFFF6FBF4), surface = Color(0xFFF6FBF4),
    surfaceVariant = Color(0xFFDCE5DC), surfaceContainer = Color(0xFFEAEFE8),
    surfaceContainerHigh = Color(0xFFE4EAE3), surfaceContainerLow = Color(0xFFF0F5EE),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8CD5AF), onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF005236), onPrimaryContainer = Color(0xFFA8F2CA),
    secondary = Color(0xFFB4CCBD), secondaryContainer = Color(0xFF364B3F),
    tertiary = Color(0xFFA5CCDF), tertiaryContainer = Color(0xFF244C5B),
    background = Color(0xFF0F1511), surface = Color(0xFF0F1511),
    surfaceVariant = Color(0xFF404943), surfaceContainer = Color(0xFF1B211D),
    surfaceContainerHigh = Color(0xFF252B27), surfaceContainerLow = Color(0xFF171D19),
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
fun OwnFitTheme(mode: ThemeMode, dynamic: Boolean, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val ctx = LocalContext.current
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> Dark
        else -> Light
    }
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = scheme, typography = Type, content = content)
}
