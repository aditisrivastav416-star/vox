package com.example.vox.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.example.vox.app.VoxApplication
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import androidx.compose.ui.window.DialogProperties
import com.example.vox.core.ui.voxViewModel
import com.example.vox.feature.activity.ActivityRoute
import com.example.vox.feature.activity.ActivityViewModel
import com.example.vox.feature.auth.AuthAction
import com.example.vox.feature.auth.AuthEvent
import com.example.vox.feature.auth.AuthViewModel
import com.example.vox.feature.auth.ProfileSetupRoute
import com.example.vox.feature.auth.SplashScreen
import com.example.vox.feature.auth.WelcomeScreen
import com.example.vox.core.ui.ObserveEvents
import com.example.vox.feature.groups.CreateGroupRoute
import com.example.vox.feature.groups.CreateGroupViewModel
import com.example.vox.feature.groups.GroupDetailRoute
import com.example.vox.feature.groups.GroupDetailViewModel
import com.example.vox.feature.groups.GroupsRoute
import com.example.vox.feature.groups.GroupsViewModel
import com.example.vox.feature.home.HomeRoute
import com.example.vox.feature.home.HomeViewModel
import com.example.vox.feature.receipt.ReceiptResultRoute
import com.example.vox.feature.receipt.ReceiptResultViewModel
import com.example.vox.feature.receipt.ReceiptScannerRoute
import com.example.vox.feature.receipt.ReceiptScannerViewModel
import com.example.vox.feature.recurring.CreateRecurringRoute
import com.example.vox.feature.recurring.CreateRecurringViewModel
import com.example.vox.feature.recurring.RecurringRoute
import com.example.vox.feature.recurring.RecurringViewModel
import com.example.vox.feature.settings.SettingsRoute
import com.example.vox.feature.settings.SettingsViewModel
import com.example.vox.feature.split.SplitRoute
import com.example.vox.feature.split.SplitViewModel
import com.example.vox.feature.transaction.TransactionDetailRoute
import com.example.vox.feature.transaction.TransactionDetailViewModel
import com.example.vox.feature.transaction.TransactionFormRoute
import com.example.vox.feature.transaction.TransactionFormViewModel
import com.example.vox.feature.vox.VoxRoute
import com.example.vox.feature.vox.VoxViewModel
import com.example.vox.navigation.VoxDestination as D
import kotlinx.coroutines.delay

private fun optionalArgs(vararg names: String) = names.map {
    navArgument(it) {
        type = NavType.StringType
        nullable = true
        defaultValue = null
    }
}

@Composable
fun VoxNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as VoxApplication
    val back: () -> Unit = { navController.popBackStack() }
    val goHome: () -> Unit = { navController.popBackStack(D.HOME, inclusive = false) }

    /** Replaces the current screen, so "back" skips it (e.g. Vox → form, scanner → result). */
    fun replace(route: String) {
        val current = navController.currentDestination?.route ?: return navController.navigate(route)
        navController.navigate(route) { popUpTo(current) { inclusive = true } }
    }

    NavHost(navController, startDestination = D.SPLASH, modifier = modifier) {
        composable(D.SPLASH) {
            LaunchedEffect(Unit) {
                delay(1_200)
                navController.navigate(if (app.container.restoreSession()) D.HOME else D.PROFILE_SETUP) { popUpTo(D.SPLASH) { inclusive = true } }
            }
            SplashScreen()
        }
        composable(D.WELCOME) {
            val vm = voxViewModel { AuthViewModel(userRepository) }
            val authState by vm.state.collectAsStateWithLifecycle()
            val enterApp = { navController.navigate(D.HOME) { popUpTo(D.WELCOME) { inclusive = true } } }
            ObserveEvents(vm.events) {
                when (it) {
                    AuthEvent.SignedIn -> enterApp()
                    AuthEvent.Registered -> navController.navigate(D.PROFILE_SETUP)
                }
            }
            WelcomeScreen(authState, vm::onAction)
        }
        composable(D.PROFILE_SETUP) {
            ProfileSetupRoute(
                viewModel = voxViewModel { AuthViewModel(userRepository) },
                onBack = back,
                onDone = { navController.navigate(D.HOME) { popUpTo(D.WELCOME) { inclusive = true } } },
            )
        }
        composable(D.HOME) {
            HomeRoute(
                viewModel = voxViewModel {
                    HomeViewModel(transactionRepository, groupRepository, recurringPaymentRepository, userRepository, budgetRepository)
                },
                onOpenRecurring = { navController.navigate(D.RECURRING) },
                onCreateRecurring = { navController.navigate(D.RECURRING_CREATE) },
                onOpenGroups = { navController.navigate(D.GROUPS) },
                onCreateGroup = { navController.navigate(D.GROUP_CREATE) },
                onOpenGroup = { navController.navigate(D.groupDetail(it)) },
                onOpenTransaction = { navController.navigate(D.transactionDetail(it)) },
            )
        }
        composable(D.ACTIVITY) {
            ActivityRoute(
                viewModel = voxViewModel { ActivityViewModel(transactionRepository, groupRepository) },
                onOpenTransaction = { navController.navigate(D.transactionDetail(it)) },
                onAddTransaction = { navController.navigate(D.transactionForm()) },
            )
        }
        dialog(D.VOX, dialogProperties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            VoxRoute(
                viewModel = voxViewModel { VoxViewModel(voiceAssistant, transactionRepository) },
                onClose = back,
                onTypeInstead = { replace(D.transactionForm(title = it.ifBlank { null })) },
                onScanReceipt = { replace(D.RECEIPT_SCAN) },
                onSplitBill = { replace(D.split()) },
                onEditDetails = { result ->
                    replace(
                        D.transactionForm(
                            amountMinor = result.amount.amountMinor,
                            title = result.title,
                            category = result.category.name,
                            type = result.type.name,
                        ),
                    )
                },
            )
        }
        composable(D.TRANSACTION_DETAIL, arguments = listOf(navArgument(D.ARG_TRANSACTION_ID) { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString(D.ARG_TRANSACTION_ID).orEmpty()
            TransactionDetailRoute(
                viewModel = voxViewModel { TransactionDetailViewModel(it, transactionRepository, groupRepository) },
                onBack = back,
                onEdit = { navController.navigate(D.transactionForm(transactionId = id)) },
            )
        }
        composable(
            D.TRANSACTION_FORM,
            arguments = optionalArgs(D.ARG_TRANSACTION_ID, D.ARG_AMOUNT, D.ARG_TITLE, D.ARG_CATEGORY, D.ARG_TYPE),
        ) {
            TransactionFormRoute(
                viewModel = voxViewModel { TransactionFormViewModel(it, transactionRepository, userRepository) },
                onBack = back,
            )
        }
        composable(D.RECEIPT_SCAN) {
            ReceiptScannerRoute(
                viewModel = voxViewModel { ReceiptScannerViewModel(receiptRepository) },
                onBack = back,
                onScanned = { replace(D.receiptResult(it)) },
            )
        }
        composable(D.RECEIPT_RESULT, arguments = listOf(navArgument(D.ARG_RECEIPT_ID) { type = NavType.StringType })) {
            ReceiptResultRoute(
                viewModel = voxViewModel { ReceiptResultViewModel(it, receiptRepository, transactionRepository) },
                onBack = back,
                onSaved = goHome,
                onSplit = { receiptId -> navController.navigate(D.split(receiptId = receiptId)) },
            )
        }
        composable(D.SPLIT, arguments = optionalArgs(D.ARG_RECEIPT_ID, D.ARG_GROUP_ID)) {
            SplitRoute(
                viewModel = voxViewModel { SplitViewModel(it, groupRepository, receiptRepository, transactionRepository, userRepository) },
                onExit = back,
                // Group splits return to the group; everything else lands back on Home.
                onDone = { if (!navController.popBackStack(D.GROUP_DETAIL, inclusive = false)) goHome() },
            )
        }
        composable(D.GROUPS) {
            GroupsRoute(
                viewModel = voxViewModel { GroupsViewModel(groupRepository, userRepository) },
                onBack = back,
                onCreate = { navController.navigate(D.GROUP_CREATE) },
                onOpenGroup = { navController.navigate(D.groupDetail(it)) },
            )
        }
        composable(D.GROUP_CREATE) {
            CreateGroupRoute(
                viewModel = voxViewModel { CreateGroupViewModel(groupRepository) },
                onBack = back,
                onCreated = { replace(D.groupDetail(it)) },
            )
        }
        composable(D.GROUP_DETAIL, arguments = listOf(navArgument(D.ARG_GROUP_ID) { type = NavType.StringType })) {
            GroupDetailRoute(
                viewModel = voxViewModel { GroupDetailViewModel(it, groupRepository) },
                onBack = back,
                onAddExpense = { groupId -> navController.navigate(D.split(groupId = groupId)) },
            )
        }
        composable(D.RECURRING) {
            RecurringRoute(
                viewModel = voxViewModel { RecurringViewModel(recurringPaymentRepository) },
                onBack = back,
                onCreate = { navController.navigate(D.RECURRING_CREATE) },
            )
        }
        composable(D.RECURRING_CREATE) {
            CreateRecurringRoute(
                viewModel = voxViewModel { CreateRecurringViewModel(recurringPaymentRepository, userRepository) },
                onBack = back,
            )
        }
        dialog(D.SETTINGS, dialogProperties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            SettingsRoute(
                viewModel = voxViewModel { SettingsViewModel(userRepository, groupRepository, recurringPaymentRepository) },
                onBack = back,
                onOpenGroups = { navController.navigate(D.GROUPS) },
                onOpenRecurring = { navController.navigate(D.RECURRING) },
                onSignedOut = {
                    navController.navigate(D.PROFILE_SETUP) { popUpTo(navController.graph.id) { inclusive = true } }
                },
            )
        }
    }
}
