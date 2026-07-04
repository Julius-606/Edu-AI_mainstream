package com.example.edu_ai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    Box(
        modifier = modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw the concentric rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val spacing = 20.dp.toPx()
            
            rings.forEachIndexed { index, ring ->
                val radius = (size.minDimension / 2) - (index * spacing) - (strokeWidth / 2)
                
                // Background Track
                drawCircle(
                    color = ring.color.copy(alpha = 0.1f),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )
                
                // Progress Arc
                drawArc(
                    color = ring.color,
                    startAngle = -90f,
                    sweepAngle = (ring.percentage / 100f) * 360f,
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
                .size(140.dp)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Objectives",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(learningObjectives) { objective ->
                    Text(
                        text = "• $objective",
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
