package com.cashflow.ai.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashflow.ai.R
import com.cashflow.ai.domain.model.ReceiptConfidence
import com.cashflow.ai.presentation.theme.ConfidenceHighColor
import com.cashflow.ai.presentation.theme.ConfidenceLowColor
import com.cashflow.ai.presentation.theme.ConfidenceMediumColor

@Composable
fun ConfidenceBadge(
    confidence: ReceiptConfidence,
    modifier: Modifier = Modifier
) {
    val config = when {
        confidence.isHighConfidence -> {
            ConfidenceBadgeConfig(
                bgColor = ConfidenceHighColor.copy(alpha = 0.15f),
                textColor = ConfidenceHighColor,
                icon = Icons.Default.CheckCircle,
                label = stringResource(R.string.high_confidence)
            )
        }
        confidence.isMediumConfidence -> {
            ConfidenceBadgeConfig(
                bgColor = ConfidenceMediumColor.copy(alpha = 0.15f),
                textColor = ConfidenceMediumColor,
                icon = Icons.Default.Info,
                label = stringResource(R.string.medium_confidence)
            )
        }
        else -> {
            ConfidenceBadgeConfig(
                bgColor = ConfidenceLowColor.copy(alpha = 0.15f),
                textColor = ConfidenceLowColor,
                icon = Icons.Default.Warning,
                label = stringResource(R.string.low_confidence)
            )
        }
    }

    val percentage = (confidence.overall * 100).toInt()

    Row(
        modifier = modifier
            .background(config.bgColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = config.icon,
            contentDescription = null,
            tint = config.textColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "AI $percentage% • ${config.label}",
            style = MaterialTheme.typography.labelSmall,
            color = config.textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class ConfidenceBadgeConfig(
    val bgColor: Color,
    val textColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)
