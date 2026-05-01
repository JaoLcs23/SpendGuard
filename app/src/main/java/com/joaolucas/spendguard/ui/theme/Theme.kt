package com.joaolucas.spendguard.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary              = Gold,
    onPrimary            = SpendGuardBlack,
    primaryContainer     = Color(0xFF3A3000),
    onPrimaryContainer   = GoldLight,
    secondary            = GoldDark,
    onSecondary          = SpendGuardBlack,
    secondaryContainer   = Color(0xFF2A2200),
    onSecondaryContainer = Gold,
    tertiary             = Color(0xFFE0C060),
    onTertiary           = SpendGuardBlack,
    tertiaryContainer    = Color(0xFF2E2800),
    onTertiaryContainer  = GoldLight,
    error                = Color(0xFFCF6679),
    errorContainer       = Color(0xFF4A1020),
    onError              = SpendGuardBlack,
    onErrorContainer     = Color(0xFFFFB3C0),
    background           = SpendGuardBlack,
    onBackground         = Color.White,
    surface              = SpendGuardSurface,
    onSurface            = Color.White,
    surfaceVariant       = SpendGuardCard,
    onSurfaceVariant     = Color(0xFFCCCCCC),
    outline              = Color(0xFF666666),
    inverseOnSurface     = SpendGuardBlack,
    inverseSurface       = Color.White,
    inversePrimary       = GoldDark,
)

private val LightColorScheme = lightColorScheme(
    primary              = GoldDark,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFFFF3C4),
    onPrimaryContainer   = Color(0xFF3A2800),
    secondary            = Color(0xFFB8860B),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFFFEDB0),
    onSecondaryContainer = Color(0xFF3A2800),
    tertiary             = Color(0xFFCC9900),
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFFFF0CC),
    onTertiaryContainer  = Color(0xFF3A2800),
    error                = Color(0xFFB00020),
    errorContainer       = Color(0xFFFFDAD6),
    onError              = Color.White,
    onErrorContainer     = Color(0xFF410002),
    background           = Color(0xFFFAF8F3),
    onBackground         = Color(0xFF1C1B00),
    surface              = Color(0xFFFFFBF0),
    onSurface            = Color(0xFF1C1B00),
    surfaceVariant       = Color(0xFFF2EDD8),
    onSurfaceVariant     = Color(0xFF4A4530),
    outline              = Color(0xFF9A9070),
    inverseOnSurface     = Color(0xFFFFFBF0),
    inverseSurface       = Color(0xFF31302A),
    inversePrimary       = Color(0xA3EAA901),
)

@Composable
fun SpendGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    forceDark: Boolean? = null,
    content: @Composable () -> Unit
) {
    val useDark = when (forceDark) {
        true  -> true
        false -> false
        null  -> darkTheme
    }

    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme
    val statusBarColor = if (useDark) SpendGuardBlack.toArgb() else Color(0xFFFAF8F3).toArgb()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = statusBarColor
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}