
package com.example.edu_ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.example.edu_ai.utils.PreferenceManager

@Composable
fun TraceTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val savedTheme = PreferenceManager.getAppTheme(context)
    val savedAccent = PreferenceManager.getThemeAccent(context)
    val savedFont = PreferenceManager.getAppFont(context)

    val isDark = when (savedTheme) {
        "light" -> false
        "oled", "dark" -> true
        "system" -> isSystemInDarkTheme()
        else -> true
    }

    val (primaryColor, secondaryColor) = when (savedAccent) {
        "emerald" -> Pair(Color(0xFF10B981), Color(0xFF06B6D4))
        "violet" -> Pair(Color(0xFF8B5CF6), Color(0xFFD946EF))
        "coral" -> Pair(Color(0xFFF43F5E), Color(0xFFFB923C))
        "amber" -> Pair(Color(0xFFF59E0B), Color(0xFFEAB308))
        else -> Pair(NeonIndigo, NeonBlue) // default indigo/blue
    }

    val isOled = savedTheme == "oled"

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            tertiary = Color(0xFFEC4899),
            background = if (isOled) Color.Black else DarkSlateBg,
            surface = if (isOled) Color(0xFF09090B) else SlateCard,
            onPrimary = Color.Black,
            onSecondary = Color.White,
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = if (isOled) Color(0xFF18181B) else SlateCardLight,
            onSurfaceVariant = Color(0xFFCBD5E1),
            primaryContainer = primaryColor.copy(alpha = 0.35f),
            secondaryContainer = secondaryColor.copy(alpha = 0.25f)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            tertiary = Color(0xFFEC4899),
            background = Color(0xFFF8FAFC),
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color.Black,
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFF1F5F9),
            onSurfaceVariant = Color(0xFF475569)
        )
    }

    val selectedFontFamily = when (savedFont) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    val dynamicTypography = Typography(
        bodyLarge = Typography.bodyLarge.copy(fontFamily = selectedFontFamily),
        bodyMedium = Typography.bodyMedium.copy(fontFamily = selectedFontFamily),
        bodySmall = Typography.bodySmall.copy(fontFamily = selectedFontFamily),
        titleLarge = Typography.titleLarge.copy(fontFamily = selectedFontFamily),
        titleMedium = Typography.titleMedium.copy(fontFamily = selectedFontFamily),
        titleSmall = Typography.titleSmall.copy(fontFamily = selectedFontFamily),
        labelLarge = Typography.labelLarge.copy(fontFamily = selectedFontFamily)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = dynamicTypography,
        content = content
    )
}

 