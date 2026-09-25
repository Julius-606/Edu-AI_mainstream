
package com.example.edu_ai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NeonBlue,
    secondary = NeonIndigo,
    tertiary = androidx.compose.ui.graphics.Color(0xFFEC4899),
    background = DarkSlateBg,
    surface = SlateCard,
    onPrimary = androidx.compose.ui.graphics.Color.Black,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onBackground = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
    surfaceVariant = SlateCardLight,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFCBD5E1),
    primaryContainer = NeonIndigo.copy(alpha = 0.35f),
    secondaryContainer = NeonBlue.copy(alpha = 0.25f)
)

private val LightColorScheme = lightColorScheme(
    primary = NeonIndigo,
    secondary = NeonBlue,
    tertiary = androidx.compose.ui.graphics.Color(0xFFEC4899),
    background = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
    surface = androidx.compose.ui.graphics.Color.White,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    onBackground = androidx.compose.ui.graphics.Color(0xFF0F172A),
    onSurface = androidx.compose.ui.graphics.Color(0xFF0F172A)
)

@Composable
fun TraceTheme(
    darkTheme: Boolean = true, // Force Dark theme for Trace slate aesthetics
    dynamicColor: Boolean = false, // Set to false to preserve brand blue/indigo neon gradients
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

 