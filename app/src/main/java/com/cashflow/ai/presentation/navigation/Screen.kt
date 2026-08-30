package com.cashflow.ai.presentation.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Transactions : Screen("transactions")
    data object AddTransaction : Screen("add_transaction?source={source}&receiptJson={receiptJson}") {
        fun createRoute(source: String = "MANUAL", receiptJson: String? = null): String {
            return if (receiptJson != null) {
                val encoded = java.net.URLEncoder.encode(receiptJson, "UTF-8")
                "add_transaction?source=$source&receiptJson=$encoded"
            } else {
                "add_transaction?source=$source"
            }
        }
    }
    data object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long): String = "edit_transaction/$transactionId"
    }
    data object CameraScan : Screen("camera_scan")
    data object Settings : Screen("settings")
    data object AiChat : Screen("ai_chat")
}
