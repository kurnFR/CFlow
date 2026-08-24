package com.cashflow.ai.presentation.ui.screens.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cashflow.ai.domain.model.ReceiptConfidence
import com.cashflow.ai.domain.model.TransactionSource
import com.cashflow.ai.domain.model.TransactionType
import com.cashflow.ai.presentation.theme.ExpenseRed
import com.cashflow.ai.presentation.theme.IncomeGreen
import com.cashflow.ai.presentation.ui.components.AmountInputField
import com.cashflow.ai.presentation.ui.components.CategorySelector
import com.cashflow.ai.presentation.ui.components.ConfidenceBadge
import com.cashflow.ai.presentation.ui.components.DatePickerModal
import com.cashflow.ai.presentation.viewmodel.AddEditTransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: AddEditTransactionViewModel,
    receiptJson: String? = null,
    transactionId: Long? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var isDatePickerOpen by remember { mutableStateOf(false) }
    var isMoreDetailsExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(receiptJson) {
        if (!receiptJson.isNullOrBlank()) {
            viewModel.populateFromReceiptJson(receiptJson)
        }
    }

    LaunchedEffect(transactionId) {
        if (transactionId != null && transactionId > 0) {
            viewModel.loadTransactionForEdit(transactionId)
        }
    }

    LaunchedEffect(uiState.isSavedSuccessfully) {
        if (uiState.isSavedSuccessfully) {
            onNavigateBack()
        }
    }

    val title = when {
        uiState.isEditMode -> "Edit Transaction"
        uiState.source == TransactionSource.PHOTO -> "Review AI Receipt"
        else -> "Add Transaction"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // AI Scanned Badge Header
            if (uiState.source == TransactionSource.PHOTO && uiState.aiConfidence != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Extracted from Receipt",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Review and edit the fields below before saving.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        ConfidenceBadge(
                            confidence = ReceiptConfidence(overall = uiState.aiConfidence ?: 0.9)
                        )
                    }
                }
            }

            // Income / Expense Type Selector
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                SegmentedButton(
                    selected = uiState.type == TransactionType.EXPENSE,
                    onClick = { viewModel.onTypeChanged(TransactionType.EXPENSE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = ExpenseRed.copy(alpha = 0.15f),
                        activeContentColor = ExpenseRed
                    )
                ) {
                    Text(
                        text = "Expense",
                        fontWeight = if (uiState.type == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal
                    )
                }

                SegmentedButton(
                    selected = uiState.type == TransactionType.INCOME,
                    onClick = { viewModel.onTypeChanged(TransactionType.INCOME) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = IncomeGreen.copy(alpha = 0.15f),
                        activeContentColor = IncomeGreen
                    )
                ) {
                    Text(
                        text = "Income",
                        fontWeight = if (uiState.type == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // Amount Input Field
            AmountInputField(
                amountText = uiState.amountText,
                onAmountChange = { viewModel.onAmountChanged(it) },
                currency = uiState.currency,
                onCurrencyChange = { viewModel.onCurrencyChanged(it) },
                isError = uiState.errorMessage != null && uiState.amountText.isBlank(),
                errorMessage = "Amount is required",
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Description Input Field
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.onDescriptionChanged(it) },
                label = { Text("Description / Merchant") },
                placeholder = { Text("e.g. Starbucks, Indomaret, Lunch") },
                singleLine = true,
                isError = uiState.errorMessage != null && uiState.description.length < 2,
                supportingText = {
                    if (uiState.errorMessage != null && uiState.description.length < 2) {
                        Text(text = "Must be at least 2 characters", color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Category Selector
            CategorySelector(
                categories = uiState.categories.filter { it.type == uiState.type },
                selectedCategoryName = uiState.category,
                suggestedCategoryName = uiState.suggestedCategory,
                onCategorySelected = { category ->
                    viewModel.onCategoryChanged(category.name)
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Date Selector Button
            OutlinedTextField(
                value = uiState.date,
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = {
                    IconButton(onClick = { isDatePickerOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Pick Date"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isDatePickerOpen = true }
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Expandable More Details (Tax, Discount, Notes)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isMoreDetailsExpanded = !isMoreDetailsExpanded }
                    ) {
                        Text(
                            text = "Additional Details (Tax, Discount, Notes)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (isMoreDetailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = isMoreDetailsExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = uiState.taxText,
                                    onValueChange = { viewModel.onTaxChanged(it) },
                                    label = { Text("Tax / PPN") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = uiState.discountText,
                                    onValueChange = { viewModel.onDiscountChanged(it) },
                                    label = { Text("Discount") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = uiState.notes,
                                onValueChange = { viewModel.onNotesChanged(it) },
                                label = { Text("Notes (optional)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Notes,
                                        contentDescription = null
                                    )
                                },
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Error Message Display
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Action Buttons
            Button(
                onClick = { viewModel.saveTransaction() },
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isEditMode) "Update Transaction" else "Save Transaction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Date Picker Modal
        if (isDatePickerOpen) {
            DatePickerModal(
                initialDateString = uiState.date,
                onDateSelected = { selectedDate ->
                    viewModel.onDateChanged(selectedDate)
                },
                onDismiss = { isDatePickerOpen = false }
            )
        }
    }
}
