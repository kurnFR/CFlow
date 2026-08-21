package com.cashflow.ai.presentation.ui.components.charts

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cashflow.ai.core.util.CurrencyFormatter
import com.cashflow.ai.domain.model.Currency
import com.cashflow.ai.domain.model.MonthlyTotal
import com.cashflow.ai.presentation.theme.ExpenseRed
import com.cashflow.ai.presentation.theme.IncomeGreen
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

@Composable
fun MonthlyTrendsLineChart(
    monthlyTotals: List<MonthlyTotal>,
    currency: Currency = Currency.IDR,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Monthly Trends",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Chart Legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendItem(color = IncomeGreen, label = "Income")
                ChartLegendItem(color = ExpenseRed, label = "Expense")
                ChartLegendItem(color = MaterialTheme.colorScheme.primary, label = "Net")
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (monthlyTotals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Not enough transaction history for trends",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val monthsLabels = monthlyTotals.map { formatMonthLabel(it.month) }

                AndroidView(
                    factory = { context ->
                        LineChart(context).apply {
                            description.isEnabled = false
                            setTouchEnabled(true)
                            isDragEnabled = true
                            setScaleEnabled(false)
                            setPinchZoom(false)
                            setDrawGridBackground(false)
                            legend.isEnabled = false

                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                setDrawGridLines(false)
                                granularity = 1f
                                textColor = AndroidColor.GRAY
                                textSize = 11f
                            }

                            axisLeft.apply {
                                setDrawGridLines(true)
                                gridColor = AndroidColor.argb(30, 128, 128, 128)
                                textColor = AndroidColor.GRAY
                                textSize = 10f
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        return CurrencyFormatter.formatCompact(value.toDouble(), currency)
                                    }
                                }
                            }

                            axisRight.isEnabled = false
                            animateX(1000)
                        }
                    },
                    update = { chart ->
                        chart.xAxis.valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                val index = value.toInt()
                                return if (index in monthsLabels.indices) monthsLabels[index] else ""
                            }
                        }

                        val incomeEntries = monthlyTotals.mapIndexed { idx, total ->
                            Entry(idx.toFloat(), total.incomeTotal.toFloat())
                        }
                        val expenseEntries = monthlyTotals.mapIndexed { idx, total ->
                            Entry(idx.toFloat(), total.expenseTotal.toFloat())
                        }
                        val netEntries = monthlyTotals.mapIndexed { idx, total ->
                            Entry(idx.toFloat(), total.netTotal.toFloat())
                        }

                        val incomeDataSet = LineDataSet(incomeEntries, "Income").apply {
                            color = AndroidColor.rgb(46, 125, 50)
                            setCircleColor(AndroidColor.rgb(46, 125, 50))
                            lineWidth = 2.5f
                            circleRadius = 4f
                            mode = LineDataSet.Mode.CUBIC_BEZIER
                            setDrawValues(false)
                        }

                        val expenseDataSet = LineDataSet(expenseEntries, "Expense").apply {
                            color = AndroidColor.rgb(198, 40, 40)
                            setCircleColor(AndroidColor.rgb(198, 40, 40))
                            lineWidth = 2.5f
                            circleRadius = 4f
                            mode = LineDataSet.Mode.CUBIC_BEZIER
                            setDrawValues(false)
                        }

                        val netDataSet = LineDataSet(netEntries, "Net").apply {
                            color = AndroidColor.rgb(0, 106, 106)
                            setCircleColor(AndroidColor.rgb(0, 106, 106))
                            lineWidth = 2f
                            circleRadius = 3.5f
                            enableDashedLine(10f, 5f, 0f)
                            mode = LineDataSet.Mode.CUBIC_BEZIER
                            setDrawValues(false)
                        }

                        chart.data = LineData(incomeDataSet, expenseDataSet, netDataSet)
                        chart.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }
        }
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatMonthLabel(monthStr: String): String {
    // "2026-08" -> "Aug"
    val parts = monthStr.split("-")
    if (parts.size == 2) {
        return when (parts[1]) {
            "01" -> "Jan"
            "02" -> "Feb"
            "03" -> "Mar"
            "04" -> "Apr"
            "05" -> "May"
            "06" -> "Jun"
            "07" -> "Jul"
            "08" -> "Aug"
            "09" -> "Sep"
            "10" -> "Oct"
            "11" -> "Nov"
            "12" -> "Dec"
            else -> monthStr
        }
    }
    return monthStr
}
