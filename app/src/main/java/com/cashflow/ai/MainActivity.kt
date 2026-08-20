package com.cashflow.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cashflow.ai.presentation.theme.CashFlowTheme
import com.cashflow.ai.presentation.ui.MainAppScaffold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CashFlowTheme {
                MainAppScaffold()
            }
        }
    }
}
