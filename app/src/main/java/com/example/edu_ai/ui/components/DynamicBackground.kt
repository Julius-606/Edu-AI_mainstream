package com.example.edu_ai.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun DynamicBackground(modifier: Modifier = Modifier) {
    val icons = listOf(
        Icons.Default.School,
        Icons.Default.Book,
        Icons.Default.Settings
    )
    
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            currentIndex = (currentIndex + 1) % icons.size
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = icons[currentIndex],
            animationSpec = tween(durationMillis = 2000),
            label = "BackgroundFade"
        ) { icon ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(300.dp)
                        .alpha(0.05f)
                )
            }
        }
    }
}
