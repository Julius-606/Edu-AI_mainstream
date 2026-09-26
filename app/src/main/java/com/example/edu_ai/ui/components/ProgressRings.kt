package com.example.edu_ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
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
    learntProgress: Float,
    quizProgress: Float,
    modifier: Modifier = Modifier
) {
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
        modifier = modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw exactly two concentric rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val spacing = 16.dp.toPx()
            
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

        // Center Content showing Learnt and Quiz percentage numbers side-by-side or stacked cleanly
        Column(
            modifier = Modifier
                .size(100.dp)
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Learnt Progress
            Text(
                text = "${learntProgress.toInt()}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            Text(
                text = "Learnt",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 10.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Quiz Progress
            Text(
                text = "${quizProgress.toInt()}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            Text(
                text = "Quiz",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 10.sp
            )
        }
    }
}
