
package com.example.edu_ai.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

import com.example.edu_ai.ui.theme.NeonBlue
import com.example.edu_ai.ui.theme.NeonIndigo
import com.example.edu_ai.ui.theme.DarkSlateBg

@Composable
fun DynamicBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundTransition")
    
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GradientPhase"
    )

    val color1 = NeonBlue.copy(alpha = 0.15f)
    val color2 = DarkSlateBg
    val color3 = NeonIndigo.copy(alpha = 0.18f)

    val animatedBrush = Brush.linearGradient(
        colors = listOf(color1, color2, color3),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(1000f * phase, 1000f * (1f - phase))
    )

    Box(modifier = modifier.fillMaxSize().background(animatedBrush)) {
        val icons = listOf(
            Icons.Default.School,
            Icons.Default.Book,
            Icons.Default.AutoAwesome
        )
        
        var currentIndex by remember { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            while (true) {
                delay(12000)
                currentIndex = (currentIndex + 1) % icons.size
            }
        }

        Crossfade(
            targetState = icons[currentIndex],
            animationSpec = tween(durationMillis = 3000),
            modifier = Modifier.align(Alignment.Center),
            label = "IconFade"
        ) { icon ->
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(400.dp)
                    .alpha(0.03f)
            )
        }
    }
}


 