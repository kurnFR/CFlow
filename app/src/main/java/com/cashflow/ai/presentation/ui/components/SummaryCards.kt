package com.cashflow.ai.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cashflow.ai.core.util.CurrencyFormatter
import com.cashflow.ai.domain.model.TransactionSummary
import com.cashflow.ai.presentation.theme.ExpenseRed
import com.cashflow.ai.presentation.theme.ExpenseRedLight
import com.cashflow.ai.presentation.theme.IncomeGreen
import com.cashflow.ai.presentation.theme.IncomeGreenLight

@Composable
fun SummaryCards(
    summary: TransactionSummary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Income Card
        MetricCard(
            title = "Income",
            amountFormatted = CurrencyFormatter.formatCompact(summary.totalIncome, summary.currency),
            growthPercent = summary.incomeGrowthPercent,
            accentColor = IncomeGreen,
            accentBgColor = IncomeGreenLight,
            icon = Icons.Default.ArrowUpward,
            modifier = Modifier.width(160.dp)
        )

        // Expense Card
        MetricCard(
            title = "Expense",
            amountFormatted = CurrencyFormatter.formatCompact(summary.totalExpense, summary.currency),
            growthPercent = summary.expenseGrowthPercent,
            accentColor = ExpenseRed,
            accentBgColor = ExpenseRedLight,
            icon = Icons.Default.ArrowDownward,
            modifier = Modifier.width(160.dp)
        )

        // Net Balance Card
        val isNetPositive = summary.netBalance >= 0
        val netPrefix = if (isNetPositive) "+" else ""
        MetricCard(
            title = "Net Balance",
            amountFormatted = "$netPrefix${CurrencyFormatter.formatCompact(summary.netBalance, summary.currency)}",
            growthPercent = summary.netGrowthPercent,
            accentColor = if (isNetPositive) MaterialTheme.colorScheme.primary else ExpenseRed,
            accentBgColor = if (isNetPositive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else ExpenseRedLight,
            icon = Icons.Default.AccountBalanceWallet,
            modifier = Modifier.width(160.dp)
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    amountFormatted: String,
    growthPercent: Float?,
    accentColor: Color,
    accentBgColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentBgColor)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = amountFormatted,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (growthPercent != null) {
                val isUp = growthPercent >= 0
                val arrow = if (isUp) "▲" else "▼"
                val growthColor = if (isUp) IncomeGreen else ExpenseRed
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$arrow ${String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(growthPercent))}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = growthColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " vs last period",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                Text(
                    text = "Current period",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
