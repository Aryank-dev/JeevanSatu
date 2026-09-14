package com.offgridrescue.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offgridrescue.app.domain.HeatmapNode
import com.offgridrescue.app.domain.ProximityLevel
import com.offgridrescue.app.domain.SignalTrend
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HeatmapView(
    nodes: List<HeatmapNode>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2 * 0.8f

            // Draw Background Rings
            drawCircle(color = Color.Gray.copy(alpha = 0.1f), radius = maxRadius * 0.3f, style = Stroke(1f))
            drawCircle(color = Color.Gray.copy(alpha = 0.1f), radius = maxRadius * 0.6f, style = Stroke(1f))
            drawCircle(color = Color.Gray.copy(alpha = 0.1f), radius = maxRadius, style = Stroke(1f))

            // Center: YOU
            drawCircle(color = Color.Blue, radius = 8.dp.toPx())

            nodes.forEach { node ->
                val radius = when (node.proximity) {
                    ProximityLevel.VERY_CLOSE -> maxRadius * 0.3f
                    ProximityLevel.NEARBY -> maxRadius * 0.6f
                    ProximityLevel.FAR -> maxRadius
                    ProximityLevel.UNKNOWN -> maxRadius * 0.9f
                }

                // Deterministic angle based on deviceId to keep node stable
                val angle = (node.deviceId.hashCode() % 360).toDouble() * (Math.PI / 180)
                val x = center.x + radius * cos(angle).toFloat()
                val y = center.y + radius * sin(angle).toFloat()
                
                val nodeColor = when (node.proximity) {
                    ProximityLevel.VERY_CLOSE -> Color.Red
                    ProximityLevel.NEARBY -> Color(0xFFF57C00) // Orange
                    ProximityLevel.FAR -> Color(0xFF388E3C) // Green
                    else -> Color.Gray
                }

                val baseNodeRadius = when (node.proximity) {
                    ProximityLevel.VERY_CLOSE -> 12.dp.toPx()
                    ProximityLevel.NEARBY -> 8.dp.toPx()
                    ProximityLevel.FAR -> 6.dp.toPx()
                    else -> 4.dp.toPx()
                }

                // Adjust pulsing based on signal trend
                val nodePulse = if (node.signalTrend == SignalTrend.STRONGER) pulseScale else 1.0f
                
                if (node.isDirect) {
                    drawCircle(
                        color = nodeColor.copy(alpha = 0.4f),
                        radius = baseNodeRadius * nodePulse,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = nodeColor,
                        radius = baseNodeRadius * 0.6f,
                        center = Offset(x, y)
                    )
                } else {
                    // Relayed node visual: Outlined
                    drawCircle(
                        color = nodeColor,
                        radius = baseNodeRadius * 0.8f,
                        center = Offset(x, y),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )
                }
            }
        }
        
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Mesh Proximity Map (Relative)",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Text(
            text = "YOU",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = Color.Blue,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}
