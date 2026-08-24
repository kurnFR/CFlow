package com.cashflow.ai.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cashflow.ai.domain.model.Currency

@Composable
fun AmountInputField(
    amountText: String,
    onAmountChange: (String) -> Unit,
    currency: Currency,
    onCurrencyChange: (Currency) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    isError: Boolean = false,
    errorMessage: String? = null
) {
    var isCurrencyMenuExpanded by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = amountText,
        onValueChange = { input ->
            // Allow only digits and decimal separators
            val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
            onAmountChange(filtered)
        },
        label = { Text(label) },
        isError = isError,
        supportingText = {
            if (isError && errorMessage != null) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        ),
        leadingIcon = {
            Box {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable { isCurrencyMenuExpanded = true }
                        .padding(start = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (currency == Currency.IDR) "Rp" else "$",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Currency",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                DropdownMenu(
                    expanded = isCurrencyMenuExpanded,
                    onDismissRequest = { isCurrencyMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("IDR (Rp - Indonesian Rupiah)") },
                        onClick = {
                            onCurrencyChange(Currency.IDR)
                            isCurrencyMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("USD ($ - US Dollar)") },
                        onClick = {
                            onCurrencyChange(Currency.USD)
                            isCurrencyMenuExpanded = false
                        }
                    )
                }
            }
        },
        trailingIcon = {
            if (amountText.isNotEmpty()) {
                IconButton(onClick = { onAmountChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear"
                    )
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}
