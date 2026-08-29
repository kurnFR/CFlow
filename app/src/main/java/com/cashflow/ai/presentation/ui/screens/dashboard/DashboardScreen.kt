package com.cashflow.ai.presentation.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashflow.ai.core.util.DateUtils
import com.cashflow.ai.domain.model.DateRange
import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.presentation.ui.components.AiChatInputBar
import com.cashflow.ai.presentation.ui.components.BatchTransactionReviewSheet
import com.cashflow.ai.presentation.ui.components.CustomDateRangePickerDialog
import com.cashflow.ai.presentation.ui.components.DateRangeFilterChips
import com.cashflow.ai.presentation.ui.components.SummaryCards
import com.cashflow.ai.presentation.ui.components.TransactionCard
import com.cashflow.ai.presentation.ui.components.charts.CategoryPieChart
import com.cashflow.ai.presentation.ui.components.charts.MonthlyTrendsLineChart
import com.cashflow.ai.presentation.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onGenerateInsight: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onAddAttachmentClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val batchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCustomDateRangePicker by remember { mutableStateOf(false) }

    val categoryIconMap = remember(uiState.categories) {
        uiState.categories.associate { it.name to it.icon }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(20.dp)
                            )
                        }
                        Text(
                            text = "CashFlow AI",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onGenerateInsight,
                        enabled = !uiState.isGeneratingInsight
                    ) {
                        if (uiState.isGeneratingInsight) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh AI insight"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Date Range Filter Chips
                    item {
                        DateRangeFilterChips(
                            selectedRange = uiState.selectedDateRange,
                            onRangeSelected = { range ->
                                if (range == DateRange.CUSTOM) {
                                    showCustomDateRangePicker = true
                                } else {
                                    viewModel.onDateRangeChanged(range)
                                }
                            },
                            customRangeLabel = if (uiState.customStartDate != null && uiState.customEndDate != null) {
                                "${DateUtils.formatDisplayDate(uiState.customStartDate!!)} - ${DateUtils.formatDisplayDate(uiState.customEndDate!!)}"
                            } else {
                                "Custom Range"
                            }
                        )
                    }

                    // Metric Summary Cards
                    item {
                        SummaryCards(summary = uiState.summary)
                    }

                    // Financial Insights Card
                    item {
                        val insight = uiState.insight
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = insight?.headline ?: "AI insight",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 10.dp)
                                    )
                                }

                                val recommendationList = insight?.recommendations ?: listOf(
                                    "Insights refresh weekly and can be generated anytime from the AI button."
                                )
                                recommendationList.forEachIndexed { index, recommendation ->
                                    Text(
                                        text = "• ${recommendation.trim()}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = if (index == 0) 8.dp else 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Expense by Category (Pie Chart)
                    item {
                        CategoryPieChart(
                            categoryExpenses = uiState.categoryExpenses,
                            currency = uiState.summary.currency
                        )
                    }

                    // Monthly Trends (Line Chart)
                    item {
                        MonthlyTrendsLineChart(
                            monthlyTotals = uiState.monthlyTrends,
                            currency = uiState.summary.currency
                        )
                    }

                    // Recent Transactions Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            TextButton(onClick = onNavigateToTransactions) {
                                Text("See All")
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Recent Transactions Items
                    if (uiState.recentTransactions.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No transactions yet. Type below e.g. 'bensin 30k' or scan receipt!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.recentTransactions, key = { it.id }) { transaction ->
                            TransactionCard(
                                transaction = transaction,
                                categoryIcon = categoryIconMap[transaction.category] ?: "🏷️",
                                onClick = { onTransactionClick(transaction) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Conversational ChatGPT / Gemini Style Natural Language Input Bar
            AiChatInputBar(
                text = uiState.chatInputText,
                onTextChanged = { viewModel.onChatInputChanged(it) },
                onSendClicked = { viewModel.parseChatInput() },
                onAddAttachmentClicked = onAddAttachmentClicked,
                isProcessing = uiState.isParsingChatInput,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }

    // Batch Review Sheet for Parsed Transactions
    if (uiState.isBatchReviewSheetOpen && uiState.parsedBatchTransactions.isNotEmpty()) {
        BatchTransactionReviewSheet(
            transactions = uiState.parsedBatchTransactions,
            categories = uiState.categories,
            onSaveAll = { items -> viewModel.saveBatchTransactions(items) },
            onDismiss = { viewModel.dismissBatchReviewSheet() },
            sheetState = batchSheetState
        )
    }

    if (showCustomDateRangePicker) {
        CustomDateRangePickerDialog(
            initialStartDate = uiState.customStartDate ?: DateUtils.getCurrentDateString(),
            initialEndDate = uiState.customEndDate ?: DateUtils.getCurrentDateString(),
            onConfirm = { startDate, endDate ->
                showCustomDateRangePicker = false
                viewModel.onCustomDateRangeSelected(startDate, endDate)
            },
            onDismiss = { showCustomDateRangePicker = false }
        )
    }
}
