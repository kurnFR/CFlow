package com.cashflow.ai.presentation.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cashflow.ai.CashFlowApp
import com.cashflow.ai.presentation.navigation.Screen
import com.cashflow.ai.presentation.ui.components.AppBottomNavigation
import com.cashflow.ai.presentation.ui.screens.camera.CameraScanScreen
import com.cashflow.ai.presentation.ui.screens.transaction.AddEditTransactionScreen
import com.cashflow.ai.presentation.ui.screens.transaction.TransactionListScreen
import com.cashflow.ai.presentation.viewmodel.AddEditTransactionViewModel
import com.cashflow.ai.presentation.viewmodel.CameraScanViewModel
import com.cashflow.ai.presentation.viewmodel.TransactionListViewModel
import kotlinx.coroutines.launch

import com.cashflow.ai.presentation.ui.screens.dashboard.DashboardScreen
import com.cashflow.ai.presentation.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold() {
    val navController = rememberNavController()
    val app = CashFlowApp.instance
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var isAddChoiceSheetOpen by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()

    val cameraScanViewModel: CameraScanViewModel = viewModel(
        factory = CameraScanViewModel.Factory(app.processReceiptUseCase)
    )

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isAddChoiceSheetOpen = false
            navController.navigate(Screen.CameraScan.route)
            cameraScanViewModel.onGalleryImageSelected(context, it)
        }
    }

    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Transactions.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigation(navController = navController)
            }
        },
        floatingActionButton = {
            if (currentRoute == Screen.Transactions.route) {
                FloatingActionButton(
                    onClick = { isAddChoiceSheetOpen = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Transaction",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val dashboardViewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.Factory(
                        app.transactionRepository,
                        app.parseNaturalLanguageTransactionsUseCase
                    )
                )
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onGenerateInsight = { dashboardViewModel.generateInsight() },
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onTransactionClick = { transaction ->
                        navController.navigate(Screen.EditTransaction.createRoute(transaction.id))
                    },
                    onAddAttachmentClicked = { isAddChoiceSheetOpen = true }
                )
            }

            composable(Screen.Transactions.route) {
                val listViewModel: TransactionListViewModel = viewModel(
                    factory = TransactionListViewModel.Factory(app.transactionRepository)
                )
                TransactionListScreen(
                    viewModel = listViewModel,
                    onTransactionClick = { transaction ->
                        navController.navigate(Screen.EditTransaction.createRoute(transaction.id))
                    },
                    onAddTransactionClick = { isAddChoiceSheetOpen = true }
                )
            }

            composable(
                route = Screen.AddTransaction.route,
                arguments = listOf(
                    navArgument("source") {
                        type = NavType.StringType
                        defaultValue = "MANUAL"
                    },
                    navArgument("receiptJson") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val addViewModel: AddEditTransactionViewModel = viewModel(
                    factory = AddEditTransactionViewModel.Factory(
                        app.transactionRepository,
                        app.suggestCategoryUseCase
                    )
                )
                val receiptJson = entry.arguments?.getString("receiptJson")
                val decodedJson = receiptJson?.let {
                    try {
                        java.net.URLDecoder.decode(it, "UTF-8")
                    } catch (e: Exception) {
                        it
                    }
                }

                AddEditTransactionScreen(
                    viewModel = addViewModel,
                    receiptJson = decodedJson,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditTransaction.route,
                arguments = listOf(
                    navArgument("transactionId") { type = NavType.LongType }
                )
            ) { entry ->
                val editViewModel: AddEditTransactionViewModel = viewModel(
                    factory = AddEditTransactionViewModel.Factory(
                        app.transactionRepository,
                        app.suggestCategoryUseCase
                    )
                )
                val transactionId = entry.arguments?.getLong("transactionId")

                AddEditTransactionScreen(
                    viewModel = editViewModel,
                    transactionId = transactionId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.CameraScan.route) {
                CameraScanScreen(
                    viewModel = cameraScanViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onReceiptScanned = { json ->
                        navController.popBackStack()
                        navController.navigate(Screen.AddTransaction.createRoute(source = "PHOTO", receiptJson = json))
                    },
                    onManualEntry = {
                        navController.popBackStack()
                        navController.navigate(Screen.AddTransaction.createRoute(source = "MANUAL"))
                    }
                )
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: com.cashflow.ai.presentation.viewmodel.SettingsViewModel = viewModel(
                    factory = com.cashflow.ai.presentation.viewmodel.SettingsViewModel.Factory(
                        app.googleAuthManager,
                        app.syncManager,
                        app.googleSheetsService
                    )
                )
                com.cashflow.ai.presentation.ui.screens.settings.SettingsScreen(
                    viewModel = settingsViewModel
                )
            }
        }

        // Add Transaction Mode Choice Sheet
        if (isAddChoiceSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isAddChoiceSheetOpen = false },
                sheetState = bottomSheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Add Transaction",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    AddOptionItem(
                        icon = Icons.Default.CameraAlt,
                        title = "Scan Receipt with AI",
                        subtitle = "Auto-extract total, items, tax and category",
                        onClick = {
                            isAddChoiceSheetOpen = false
                            navController.navigate(Screen.CameraScan.route)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AddOptionItem(
                        icon = Icons.Default.PhotoLibrary,
                        title = "Choose from Gallery",
                        subtitle = "Select a receipt photo from your photos",
                        onClick = {
                            galleryPickerLauncher.launch("image/*")
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AddOptionItem(
                        icon = Icons.Default.Edit,
                        title = "Manual Entry",
                        subtitle = "Type amount, date, and description manually",
                        onClick = {
                            isAddChoiceSheetOpen = false
                            navController.navigate(Screen.AddTransaction.createRoute(source = "MANUAL"))
                        }
                    )

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun AddOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
