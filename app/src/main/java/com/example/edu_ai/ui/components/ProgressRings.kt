
package com.example.edu_ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class RingProgress(
    val percentage: Float,
    val color: Color,
    val label: String
)

@Composable
fun ProgressRings(
    rings: List<RingProgress>,
    learningObjectives: List<String>,
    modifier: Modifier = Modifier
) {
    // Animation state for the rings
    var animationStarted by remember { mutableStateOf(false) }
    
    val animatedPercentages = rings.map { ring ->
        animateFloatAsState(
            targetValue = if (animationStarted) ring.percentage else 0f,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            label = "RingAnimation_${ring.label}"
        )
    }

    LaunchedEffect(Unit) {
        animationStarted = true
    }

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw the concentric rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val spacing = 18.dp.toPx()
            
            rings.forEachIndexed { index, ring ->
                val radius = (size.minDimension / 2) - (index * spacing) - (strokeWidth / 2)
                
                // Background Track
                drawCircle(
                    color = ring.color.copy(alpha = 0.08f),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )
                
                // Progress Arc
                val sweepAngle = (animatedPercentages[index].value / 100f) * 360f
                drawArc(
                    color = ring.color,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = center.copy(x = center.x - radius, y = center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center Content: Scrollable Learning Objectives
        Column(
            modifier = Modifier
                .size(110.dp)
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "FOCUS",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(learningObjectives) { objective ->
                    Text(
                        text = objective,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}


 