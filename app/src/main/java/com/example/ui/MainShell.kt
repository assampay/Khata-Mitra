package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ThemePreference
import com.example.data.model.Customer
import com.example.data.model.LedgerTransaction
import com.example.data.model.PartyType
import com.example.data.model.PaymentMode
import com.example.data.model.ReminderStatus
import com.example.data.model.Supplier
import com.example.data.model.TxnType
import com.example.ui.auth.PinLockScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.AddEditCustomerDialog
import com.example.ui.screens.AddEditSupplierDialog
import com.example.ui.screens.AddExpenseDialog
import com.example.ui.screens.AddSaleDialog
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.BackupSyncScreen
import com.example.ui.screens.BusinessBookScreen
import com.example.ui.screens.BusinessProfileScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.KhataListScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.MoreRoute
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PartyLedgerScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.ReportsAnalyticsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubscriptionScreen
import com.example.ui.screens.SupportScreen
import com.example.ui.screens.TransactionDetailDialog
import com.example.ui.screens.TransactionEntryDialog
import com.example.ui.screens.UpiQrScreen
import com.example.ui.viewmodel.KhataViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.LocalAppStrings
import com.example.util.ShareAndIntents
import com.example.util.stringsFor

private enum class MainTab {
    HOME, KHATA, BOOK, MORE
}

private data class TxnDialogConfig(
    val partyType: PartyType,
    val txnType: TxnType,
    val partyId: String? = null,
    val partyName: String? = null
)

@Composable
fun MainShell(viewModel: KhataViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val localPrefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val businessesState by viewModel.businessesState.collectAsStateWithLifecycle()
    val activeBusiness by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val customersState by viewModel.customersState.collectAsStateWithLifecycle()
    val filteredCustomers by viewModel.filteredCustomers.collectAsStateWithLifecycle()
    val suppliersState by viewModel.suppliersState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val expensesState by viewModel.expensesState.collectAsStateWithLifecycle()
    val salesState by viewModel.salesState.collectAsStateWithLifecycle()
    val remindersState by viewModel.remindersState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val supportTicketsState by viewModel.supportTicketsState.collectAsStateWithLifecycle()
    val announcementsState by viewModel.announcementsState.collectAsStateWithLifecycle()
    val adminUsersState by viewModel.adminUsersState.collectAsStateWithLifecycle()
    val adminTicketsState by viewModel.adminTicketsState.collectAsStateWithLifecycle()

    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val searchQuery by viewModel.customerSearch.collectAsStateWithLifecycle()
    val activeFilter by viewModel.customerFilter.collectAsStateWithLifecycle()
    val activeSort by viewModel.partySort.collectAsStateWithLifecycle()

    val userProfile = (userProfileState as? UiState.Success)?.data
    val businesses = (businessesState as? UiState.Success)?.data.orEmpty()
    val allCustomers = (customersState as? UiState.Success)?.data.orEmpty()
    val allSuppliers = (suppliersState as? UiState.Success)?.data.orEmpty()
    val allTransactions = (transactionsState as? UiState.Success)?.data.orEmpty()
    val allExpenses = (expensesState as? UiState.Success)?.data.orEmpty()
    val allSales = (salesState as? UiState.Success)?.data.orEmpty()
    val allReminders = (remindersState as? UiState.Success)?.data.orEmpty()
    val allNotifications = (notificationsState as? UiState.Success)?.data.orEmpty()
    val myTickets = (supportTicketsState as? UiState.Success)?.data.orEmpty()
    val announcements = (announcementsState as? UiState.Success)?.data.orEmpty()
    val adminUsers = (adminUsersState as? UiState.Success)?.data.orEmpty()
    val adminTickets = (adminTicketsState as? UiState.Success)?.data.orEmpty()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // Surface any Firestore stream errors safely in Snackbar
    val streamError = listOf(customersState, suppliersState, transactionsState)
        .filterIsInstance<UiState.Error>()
        .firstOrNull()?.message
    LaunchedEffect(streamError) {
        if (!streamError.isNullOrBlank()) {
            snackbarHostState.showSnackbar(streamError)
        }
    }

    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var khataInitialTab by remember { mutableIntStateOf(0) }
    var bookInitialTab by remember { mutableIntStateOf(0) }

    // Sub-screen navigation states
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    var selectedSupplierId by remember { mutableStateOf<String?>(null) }
    var activeMoreRoute by remember { mutableStateOf<MoreRoute?>(null) }
    var upiPrefillAmount by remember { mutableStateOf<Double?>(null) }
    var showPinSetup by remember { mutableStateOf(false) }

    // Dialog states
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<Supplier?>(null) }
    var txnDialogConfig by remember { mutableStateOf<TxnDialogConfig?>(null) }
    var detailTxn by remember { mutableStateOf<LedgerTransaction?>(null) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddSaleDialog by remember { mutableStateOf(false) }

    val strings = remember(localPrefs.language) { stringsFor(localPrefs.language) }

    CompositionLocalProvider(LocalAppStrings provides strings) {
        if (showPinSetup) {
            BackHandler { showPinSetup = false }
            PinLockScreen(
                isSetupMode = true,
                biometricEnabled = false,
                verifyPin = { false },
                onPinSetupComplete = { newPin ->
                    viewModel.savePin(newPin)
                    showPinSetup = false
                },
                onUnlocked = {},
                onForgotPinSignOut = {},
                onCancelSetup = { showPinSetup = false }
            )
            return@CompositionLocalProvider
        }

        // Handle full-screen pushed routes cleanly
        val selectedCustomer = selectedCustomerId?.let { id -> allCustomers.firstOrNull { it.id == id } }
        val selectedSupplier = selectedSupplierId?.let { id -> allSuppliers.firstOrNull { it.id == id } }

        when {
            selectedCustomer != null -> {
                PartyLedgerScreen(
                    partyType = PartyType.CUSTOMER,
                    customer = selectedCustomer,
                    supplier = null,
                    business = activeBusiness,
                    transactions = allTransactions,
                    onBack = { selectedCustomerId = null },
                    onGiveCreditOrPurchase = {
                        txnDialogConfig = TxnDialogConfig(
                            PartyType.CUSTOMER, TxnType.CREDIT_GIVEN, selectedCustomer.id, selectedCustomer.name
                        )
                    },
                    onReceiveOrPay = {
                        txnDialogConfig = TxnDialogConfig(
                            PartyType.CUSTOMER, TxnType.PAYMENT_RECEIVED, selectedCustomer.id, selectedCustomer.name
                        )
                    },
                    onEditParty = { editingCustomer = selectedCustomer },
                    onDeleteParty = {
                        viewModel.deleteCustomer(selectedCustomer.id) { selectedCustomerId = null }
                    },
                    onTransactionClick = { detailTxn = it },
                    onOpenUpiCollect = { amt ->
                        upiPrefillAmount = amt
                        selectedCustomerId = null
                        activeMoreRoute = MoreRoute.UPI_QR
                    },
                    onLogReminder = { channel, msg ->
                        viewModel.logReminder(
                            selectedCustomer.id, selectedCustomer.name, selectedCustomer.phone,
                            kotlin.math.abs(selectedCustomer.currentBalance), channel, msg
                        )
                    }
                )
            }
            selectedSupplier != null -> {
                PartyLedgerScreen(
                    partyType = PartyType.SUPPLIER,
                    customer = null,
                    supplier = selectedSupplier,
                    business = activeBusiness,
                    transactions = allTransactions,
                    onBack = { selectedSupplierId = null },
                    onGiveCreditOrPurchase = {
                        txnDialogConfig = TxnDialogConfig(
                            PartyType.SUPPLIER, TxnType.PURCHASE_PAYABLE, selectedSupplier.id, selectedSupplier.name
                        )
                    },
                    onReceiveOrPay = {
                        txnDialogConfig = TxnDialogConfig(
                            PartyType.SUPPLIER, TxnType.SUPPLIER_PAYMENT, selectedSupplier.id, selectedSupplier.name
                        )
                    },
                    onEditParty = { editingSupplier = selectedSupplier },
                    onDeleteParty = {
                        viewModel.deleteSupplier(selectedSupplier.id) { selectedSupplierId = null }
                    },
                    onTransactionClick = { detailTxn = it },
                    onOpenUpiCollect = {},
                    onLogReminder = { channel, msg ->
                        viewModel.logReminder(
                            selectedSupplier.id, selectedSupplier.name, selectedSupplier.phone,
                            kotlin.math.abs(selectedSupplier.currentBalance), channel, msg
                        )
                    }
                )
            }
            activeMoreRoute != null -> {
                when (activeMoreRoute!!) {
                    MoreRoute.REPORTS -> ReportsAnalyticsScreen(
                        business = activeBusiness,
                        customers = allCustomers,
                        suppliers = allSuppliers,
                        transactions = allTransactions,
                        expenses = allExpenses,
                        sales = allSales,
                        onBack = { activeMoreRoute = null }
                    )
                    MoreRoute.REMINDERS -> RemindersScreen(
                        business = activeBusiness,
                        customers = allCustomers,
                        reminders = allReminders,
                        onBack = { activeMoreRoute = null },
                        onLogReminder = { cust, ch, msg, st ->
                            viewModel.logReminder(cust.id, cust.name, cust.phone, cust.currentBalance, ch, msg, status = st)
                        }
                    )
                    MoreRoute.UPI_QR -> UpiQrScreen(
                        business = activeBusiness,
                        initialAmount = upiPrefillAmount,
                        onSaveUpiId = { updated -> viewModel.saveBusiness(updated) },
                        onBack = {
                            upiPrefillAmount = null
                            activeMoreRoute = null
                        }
                    )
                    MoreRoute.NOTIFICATIONS -> NotificationsScreen(
                        notifications = allNotifications,
                        onMarkRead = { viewModel.markNotificationRead(it) },
                        onBack = { activeMoreRoute = null }
                    )
                    MoreRoute.BUSINESS_PROFILE -> BusinessProfileScreen(
                        activeBusiness = activeBusiness,
                        businesses = businesses,
                        onSelectBusiness = { viewModel.selectBusiness(it) },
                        onSaveBusiness = { viewModel.saveBusiness(it) },
                        onBack = { activeMoreRoute = null }
                    )
                    MoreRoute.SUBSCRIPTION -> SubscriptionScreen(
                        userProfile = userProfile,
                        customerCount = allCustomers.size,
                        onSelectPlan = { viewModel.updatePlanTier(it) },
                        onBack = { activeMoreRoute = null }
                    )
                    MoreRoute.BACKUP_SYNC -> BackupSyncScreen(
                        lastSyncMillis = localPrefs.lastSyncMillis,
                        customers = allCustomers,
                        suppliers = allSuppliers,
                        transactions = allTransactions,
                        expenses = allExpenses,
                        sales = allSales,
                        onManualSync = { viewModel.triggerManualSync() },
                        onBack = { activeMoreRoute = null }
                    )
                    MoreRoute.SETTINGS -> SettingsScreen(
                        prefs = localPrefs,
                        onSetLanguage = { viewModel.setLanguage(it) },
                        onSetTheme = { viewModel.setThemePreference(it) },
                        onSetupPin = { showPinSetup = true },
                        onDisablePin = { viewModel.disablePin() },
                        onToggleBiometric = { viewModel.setBiometricEnabled(it) },
                        onToggleNotifications = { rem, sum -> viewModel.setNotificationPreferences(rem, sum) },
                        onSignOut = { signOutUser(credentialManager, scope) },
                        onDeleteAccount = {
                            viewModel.deleteAccountProfile { signOutUser(credentialManager, scope) }
                        },
                        onBack = { activeMoreRoute = null }
                    )
                    MoreRoute.SUPPORT -> SupportScreen(
                        tickets = myTickets,
                        onSubmitTicket = { sub, desc, cat -> viewModel.submitSupportTicket(sub, desc, cat) },
                        onBack = { activeMoreRoute = null }
                    )
                    MoreRoute.ADMIN -> AdminDashboardScreen(
                        users = adminUsers,
                        tickets = adminTickets,
                        onUpdateTicket = { id, status, reply -> viewModel.adminUpdateTicket(id, status, reply) },
                        onPublishAnnouncement = { title, msg -> viewModel.adminPublishAnnouncement(title, msg) },
                        onUpdateUser = { uid, st, plan -> viewModel.adminUpdateUser(uid, st, plan) },
                        onBack = { activeMoreRoute = null }
                    )
                }
            }
            else -> {
                // Main Adaptive 4-Tab Shell
                val navItems = listOf(
                    Triple(MainTab.HOME, strings.navHome, Icons.Filled.Home to Icons.Outlined.Home),
                    Triple(MainTab.KHATA, strings.navKhata, Icons.Filled.AccountBalanceWallet to Icons.Outlined.AccountBalanceWallet),
                    Triple(MainTab.BOOK, strings.navBook, Icons.Filled.Book to Icons.Outlined.Book),
                    Triple(MainTab.MORE, strings.navMore, Icons.Filled.GridView to Icons.Outlined.GridView)
                )

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 600.dp
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets.safeDrawing,
                        bottomBar = {
                            if (!useRail) {
                                NavigationBar(
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                                ) {
                                    navItems.forEach { (tab, label, icons) ->
                                        val selected = currentTab == tab
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { currentTab = tab },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) icons.first else icons.second,
                                                    contentDescription = label
                                                )
                                            },
                                            label = { Text(label) },
                                            modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (useRail) {
                                NavigationRail {
                                    navItems.forEach { (tab, label, icons) ->
                                        val selected = currentTab == tab
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { currentTab = tab },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) icons.first else icons.second,
                                                    contentDescription = label
                                                )
                                            },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                when (currentTab) {
                                    MainTab.HOME -> HomeDashboardScreen(
                                        activeBusiness = activeBusiness,
                                        businesses = businesses,
                                        summary = summary,
                                        recentTransactions = allTransactions,
                                        announcements = announcements,
                                        unreadNotificationCount = allNotifications.count { !it.readFlag },
                                        isSyncing = isSyncing,
                                        lastSyncMillis = localPrefs.lastSyncMillis,
                                        onSelectBusiness = { viewModel.selectBusiness(it) },
                                        onManualSync = { viewModel.triggerManualSync() },
                                        onOpenNotifications = { activeMoreRoute = MoreRoute.NOTIFICATIONS },
                                        onAddCustomer = { showAddCustomerDialog = true },
                                        onAddSupplier = { showAddSupplierDialog = true },
                                        onGiveCredit = {
                                            if (allCustomers.isEmpty()) showAddCustomerDialog = true
                                            else txnDialogConfig = TxnDialogConfig(PartyType.CUSTOMER, TxnType.CREDIT_GIVEN)
                                        },
                                        onReceivePayment = {
                                            if (allCustomers.isEmpty()) showAddCustomerDialog = true
                                            else txnDialogConfig = TxnDialogConfig(PartyType.CUSTOMER, TxnType.PAYMENT_RECEIVED)
                                        },
                                        onAddExpense = { showAddExpenseDialog = true },
                                        onAddSale = { showAddSaleDialog = true },
                                        onOpenReports = { activeMoreRoute = MoreRoute.REPORTS },
                                        onOpenUpiQr = { activeMoreRoute = MoreRoute.UPI_QR },
                                        onTransactionClick = { detailTxn = it },
                                        onViewAllTransactions = {
                                            khataInitialTab = 0
                                            currentTab = MainTab.KHATA
                                        }
                                    )
                                    MainTab.KHATA -> KhataListScreen(
                                        customers = filteredCustomers,
                                        suppliers = allSuppliers,
                                        searchQuery = searchQuery,
                                        activeFilter = activeFilter,
                                        activeSort = activeSort,
                                        initialTab = khataInitialTab,
                                        onSearchChange = { viewModel.setCustomerSearch(it) },
                                        onFilterChange = { viewModel.setCustomerFilter(it) },
                                        onSortChange = { viewModel.setPartySort(it) },
                                        onCustomerClick = { selectedCustomerId = it.id },
                                        onSupplierClick = { selectedSupplierId = it.id },
                                        onOpenAddCustomer = { showAddCustomerDialog = true },
                                        onOpenAddSupplier = { showAddSupplierDialog = true }
                                    )
                                    MainTab.BOOK -> BusinessBookScreen(
                                        business = activeBusiness,
                                        sales = allSales,
                                        expenses = allExpenses,
                                        customers = allCustomers,
                                        initialTab = bookInitialTab,
                                        onOpenAddSale = { showAddSaleDialog = true },
                                        onOpenAddExpense = { showAddExpenseDialog = true },
                                        onDeleteExpense = { viewModel.deleteExpense(it) }
                                    )
                                    MainTab.MORE -> MoreHubScreen(
                                        userProfile = userProfile,
                                        activeBusiness = activeBusiness,
                                        isAdmin = isAdmin,
                                        overdueCount = summary.overdueCount,
                                        onNavigate = { activeMoreRoute = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Modal Dialogs ---
        if (showAddCustomerDialog || editingCustomer != null) {
            val editing = editingCustomer
            AddEditCustomerDialog(
                existing = editing,
                onDismiss = {
                    showAddCustomerDialog = false
                    editingCustomer = null
                },
                onSave = { name, phone, address, category, notes, photoUri, openBal ->
                    if (editing == null) {
                        viewModel.addCustomer(name, phone, address, category, notes, photoUri, openBal, null) {
                            showAddCustomerDialog = false
                        }
                    } else {
                        viewModel.updateCustomer(editing.id, name, phone, address, category, notes, photoUri, editing.dueDateMillis) {
                            editingCustomer = null
                        }
                    }
                }
            )
        }

        if (showAddSupplierDialog || editingSupplier != null) {
            val editing = editingSupplier
            AddEditSupplierDialog(
                existing = editing,
                onDismiss = {
                    showAddSupplierDialog = false
                    editingSupplier = null
                },
                onSave = { name, company, phone, address, gst, openBal ->
                    if (editing == null) {
                        viewModel.addSupplier(name, company, phone, address, gst, openBal, null) {
                            showAddSupplierDialog = false
                        }
                    } else {
                        viewModel.updateSupplier(editing.id, name, company, phone, address, gst, editing.dueDateMillis) {
                            editingSupplier = null
                        }
                    }
                }
            )
        }

        txnDialogConfig?.let { cfg ->
            TransactionEntryDialog(
                initialPartyType = cfg.partyType,
                initialTxnType = cfg.txnType,
                preselectedPartyId = cfg.partyId,
                preselectedPartyName = cfg.partyName,
                customers = allCustomers,
                suppliers = allSuppliers,
                onDismiss = { txnDialogConfig = null },
                onSave = { partyId, partyName, pType, tType, amount, mode, desc, items, ref, attach ->
                    viewModel.recordTransaction(
                        partyId, partyName, pType, tType, amount, mode, desc, items, ref, attach, System.currentTimeMillis()
                    ) {
                        if (tType == TxnType.PAYMENT_RECEIVED && localPrefs.reminderNotificationsEnabled) {
                            ShareAndIntents.showLocalNotification(
                                context,
                                "Payment Received",
                                "Received ₹${amount.toLong()} from $partyName"
                            )
                        }
                        txnDialogConfig = null
                    }
                }
            )
        }

        detailTxn?.let { txn ->
            TransactionDetailDialog(
                txn = txn,
                business = activeBusiness,
                onDismiss = { detailTxn = null },
                onEdit = { newAmt, mode, notes ->
                    viewModel.editTransaction(
                        txn, newAmt, mode, notes, txn.itemDetails, txn.referenceNumber, txn.txnTimeMillis
                    ) { detailTxn = null }
                },
                onReverse = { reason ->
                    viewModel.reverseTransaction(txn, reason) { detailTxn = null }
                }
            )
        }

        if (showAddExpenseDialog) {
            AddExpenseDialog(
                onDismiss = { showAddExpenseDialog = false },
                onSave = { amt, cat, mode, notes, receipt ->
                    viewModel.addExpense(amt, cat, mode, notes, receipt, System.currentTimeMillis()) {
                        showAddExpenseDialog = false
                    }
                }
            )
        }

        if (showAddSaleDialog) {
            AddSaleDialog(
                customers = allCustomers,
                onDismiss = { showAddSaleDialog = false },
                onSave = { custId, custName, prod, qty, price, disc, tax, paid, mode ->
                    viewModel.addSale(
                        custId, custName, prod, qty, price, disc, tax, paid, mode, System.currentTimeMillis()
                    ) { showAddSaleDialog = false }
                }
            )
        }
    }
}
