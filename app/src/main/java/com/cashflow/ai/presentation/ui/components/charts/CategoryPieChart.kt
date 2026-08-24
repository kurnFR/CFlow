package com.cashflow.ai.presentation.ui.components.charts

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.cashflow.ai.core.util.CurrencyFormatter
import com.cashflow.ai.domain.model.CategoryExpense
import com.cashflow.ai.domain.model.Currency
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPieChart(
    categoryExpenses: List<CategoryExpense>,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Expense by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val totalSum = categoryExpenses.sumOf { it.total }
                Text(
                    text = CurrencyFormatter.format(totalSum, currency),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (categoryExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No expenses recorded in this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val chartColors = remember(categoryExpenses) {
                    categoryExpenses.map { item ->
                        try {
                            AndroidColor.parseColor(item.colorHex)
                        } catch (e: Exception) {
                            getDefaultColorForIndex(categoryExpenses.indexOf(item))
                        }
                    }
                }

                // MPAndroidChart PieChart View
                AndroidView(
                    factory = { context ->
                        PieChart(context).apply {
                            description.isEnabled = false
                            isDrawHoleEnabled = true
                            setHoleColor(AndroidColor.TRANSPARENT)
                            setTransparentCircleRadius(58f)
                            holeRadius = 52f
                            setDrawCenterText(true)
                            centerText = "Expenses"
                            setCenterTextSize(14f)
                            setCenterTextColor(AndroidColor.GRAY)
                            setDrawEntryLabels(false)
                            legend.isEnabled = false
                            setUsePercentValues(true)
                            setExtraOffsets(0f, 0f, 0f, 0f)
                            animateY(1200, Easing.EaseInOutQuad)
                        }
                    },
                    update = { chart ->
                        val entries = categoryExpenses.map { exp ->
                            PieEntry(exp.total.toFloat(), exp.category)
                        }

                        val dataSet = PieDataSet(entries, "Expense Categories").apply {
                            colors = chartColors
                            sliceSpace = 3f
                            setDrawValues(false)
                        }

                        chart.data = PieData(dataSet)
                        chart.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Breakdown Legend Cards
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryExpenses.forEachIndexed { index, exp ->
                        val color = Color(chartColors[index])
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${exp.icon} ${exp.category}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", exp.percentage)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getDefaultColorForIndex(index: Int): Int {
    val fallbackColors = intArrayOf(
        AndroidColor.rgb(255, 87, 34),
        AndroidColor.rgb(76, 175, 80),
        AndroidColor.rgb(33, 150, 243),
        AndroidColor.rgb(233, 30, 99),
        AndroidColor.rgb(255, 152, 0),
        AndroidColor.rgb(156, 39, 176),
        AndroidColor.rgb(0, 188, 212),
        AndroidColor.rgb(121, 85, 72),
        AndroidColor.rgb(96, 125, 139)
    )
    return fallbackColors[index % fallbackColors.size]
}
