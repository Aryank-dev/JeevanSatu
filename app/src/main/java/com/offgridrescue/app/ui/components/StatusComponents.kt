package com.offgridrescue.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SemanticStatusType {
    HEALTHY,   // Green + Check
    WARNING,   // Amber + Clock/Wait
    ERROR,     // Red + !
    INFO       // Blue + Dot/Info
}

@Composable
fun SemanticStatusIndicator(
    type: SemanticStatusType,
    label: String,
    modifier: Modifier = Modifier
) {
    val (color, icon) = when (type) {
        SemanticStatusType.HEALTHY -> Color(0xFF388E3C) to Icons.Default.CheckCircle
        SemanticStatusType.WARNING -> Color(0xFFF57C00) to Icons.Default.Refresh
        SemanticStatusType.ERROR -> Color(0xFFD32F2F) to Icons.Default.Warning
        SemanticStatusType.INFO -> MaterialTheme.colorScheme.primary to Icons.Default.Info
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatusTile(
    title: String,
    statusText: String,
    statusType: SemanticStatusType,
    modifier: Modifier = Modifier,
    subText: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SemanticStatusIndicator(type = statusType, label = statusText)
            if (subText != null) {
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}
