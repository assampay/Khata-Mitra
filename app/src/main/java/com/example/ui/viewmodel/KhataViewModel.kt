package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppPreferences
import com.example.data.local.LocalSecurityPrefs
import com.example.data.local.ThemePreference
import com.example.data.model.Announcement
import com.example.data.model.AppLanguage
import com.example.data.model.AppNotification
import com.example.data.model.BusinessProfile
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.LedgerTransaction
import com.example.data.model.NotificationCategory
import com.example.data.model.PartyType
import com.example.data.model.PaymentMode
import com.example.data.model.PlanTier
import com.example.data.model.ReminderChannel
import com.example.data.model.ReminderRecord
import com.example.data.model.ReminderStatus
import com.example.data.model.SaleRecord
import com.example.data.model.Supplier
import com.example.data.model.SupportTicket
import com.example.data.model.TicketStatus
import com.example.data.model.TxnStatus
import com.example.data.model.TxnType
import com.example.data.model.UserProfile
import com.example.data.remote.KhataRepository
import com.example.util.Formatters
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class ReportTimeframe(val label: String, val days: Int) {
    DAILY("Today", 1),
    WEEKLY("This Week", 7),
    MONTHLY("This Month", 30),
    YEARLY("This Year", 365)
}

enum class CustomerFilter {
    ALL, TO_RECEIVE, TO_PAY, OVERDUE
}

enum class PartySort {
    RECENT, BALANCE_HIGH, NAME_AZ
}

data class DashboardSummary(
    val totalToReceive: Double = 0.0,
    val totalToPay: Double = 0.0,
    val todayReceived: Double = 0.0,
    val todayGiven: Double = 0.0,
    val todaySales: Double = 0.0,
    val todayExpenses: Double = 0.0,
    val customerCount: Int = 0,
    val supplierCount: Int = 0,
    val overdueCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class KhataViewModel(
    private val repository: KhataRepository,
    private val prefs: AppPreferences,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String?
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _customerSearch = MutableStateFlow("")
    val customerSearch: StateFlow<String> = _customerSearch.asStateFlow()

    private val _customerFilter = MutableStateFlow(CustomerFilter.ALL)
    val customerFilter: StateFlow<CustomerFilter> = _customerFilter.asStateFlow()

    private val _partySort = MutableStateFlow(PartySort.RECENT)
    val partySort: StateFlow<PartySort> = _partySort.asStateFlow()

    val localPrefs: StateFlow<LocalSecurityPrefs> = prefs.prefsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = LocalSecurityPrefs()
    )

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .toUiState("Failed to load profile")

    val businessesState: StateFlow<UiState<List<BusinessProfile>>> = repository.observeBusinesses(currentUserId)
        .toUiState("Failed to load businesses")

    val activeBusiness: StateFlow<BusinessProfile?> = combine(
        userProfileState,
        businessesState
    ) { userState, bizState ->
        val businesses = (bizState as? UiState.Success)?.data.orEmpty()
        val activeId = (userState as? UiState.Success)?.data?.activeBusinessId
        businesses.firstOrNull { it.id == activeId } ?: businesses.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val customersState: StateFlow<UiState<List<Customer>>> = combine(
        repository.observeCustomers(currentUserId),
        activeBusiness
    ) { list, biz ->
        if (biz == null) list else list.filter { it.businessId == biz.id }
    }.toUiState("Failed to load customers")

    val suppliersState: StateFlow<UiState<List<Supplier>>> = combine(
        repository.observeSuppliers(currentUserId),
        activeBusiness
    ) { list, biz ->
        if (biz == null) list else list.filter { it.businessId == biz.id }
    }.toUiState("Failed to load suppliers")

    val transactionsState: StateFlow<UiState<List<LedgerTransaction>>> = combine(
        repository.observeTransactions(currentUserId),
        activeBusiness
    ) { list, biz ->
        if (biz == null) list else list.filter { it.businessId == biz.id }
    }.toUiState("Failed to load ledger")

    val expensesState: StateFlow<UiState<List<Expense>>> = combine(
        repository.observeExpenses(currentUserId),
        activeBusiness
    ) { list, biz ->
        if (biz == null) list else list.filter { it.businessId == biz.id }
    }.toUiState("Failed to load expenses")

    val salesState: StateFlow<UiState<List<SaleRecord>>> = combine(
        repository.observeSales(currentUserId),
        activeBusiness
    ) { list, biz ->
        if (biz == null) list else list.filter { it.businessId == biz.id }
    }.toUiState("Failed to load sales")

    val remindersState: StateFlow<UiState<List<ReminderRecord>>> = combine(
        repository.observeReminders(currentUserId),
        activeBusiness
    ) { list, biz ->
        if (biz == null) list else list.filter { it.businessId == biz.id }
    }.toUiState("Failed to load reminders")

    val notificationsState: StateFlow<UiState<List<AppNotification>>> =
        repository.observeNotifications(currentUserId)
            .toUiState("Failed to load notifications")

    val supportTicketsState: StateFlow<UiState<List<SupportTicket>>> =
        repository.observeMySupportTickets(currentUserId)
            .toUiState("Failed to load tickets")

    val announcementsState: StateFlow<UiState<List<Announcement>>> =
        repository.observeAnnouncements(currentUserId)
            .toUiState("Failed to load announcements")

    val adminUsersState: StateFlow<UiState<List<UserProfile>>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllUsersAdmin(currentUserId) else flowOf(emptyList())
        }
        .toUiState("Failed to load users")

    val adminTicketsState: StateFlow<UiState<List<SupportTicket>>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllSupportTicketsAdmin(currentUserId) else flowOf(emptyList())
        }
        .toUiState("Failed to load admin tickets")

    val filteredCustomers: StateFlow<List<Customer>> = combine(
        customersState,
        customerSearch,
        customerFilter,
        partySort
    ) { state, query, filter, sort ->
        val raw = (state as? UiState.Success)?.data.orEmpty()
        val now = System.currentTimeMillis()
        val searched = if (query.isBlank()) raw else raw.filter {
            it.name.contains(query, ignoreCase = true) ||
                (it.phone?.contains(query, ignoreCase = true) == true)
        }
        val filtered = when (filter) {
            CustomerFilter.ALL -> searched
            CustomerFilter.TO_RECEIVE -> searched.filter { it.currentBalance > 0 }
            CustomerFilter.TO_PAY -> searched.filter { it.currentBalance < 0 }
            CustomerFilter.OVERDUE -> searched.filter {
                it.currentBalance > 0 && it.dueDateMillis != null && it.dueDateMillis < now
            }
        }
        when (sort) {
            PartySort.RECENT -> filtered.sortedByDescending { it.lastTransactionAtMillis ?: 0L }
            PartySort.BALANCE_HIGH -> filtered.sortedByDescending { kotlin.math.abs(it.currentBalance) }
            PartySort.NAME_AZ -> filtered.sortedBy { it.name.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        customersState,
        suppliersState,
        transactionsState,
        salesState,
        expensesState
    ) { custState, supState, txnState, saleState, expState ->
        val customers = (custState as? UiState.Success)?.data.orEmpty()
        val suppliers = (supState as? UiState.Success)?.data.orEmpty()
        val txns = (txnState as? UiState.Success)?.data.orEmpty()
        val sales = (saleState as? UiState.Success)?.data.orEmpty()
        val expenses = (expState as? UiState.Success)?.data.orEmpty()

        val startOfToday = Formatters.startOfTodayMillis()
        val now = System.currentTimeMillis()

        val customerReceive = customers.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
        val customerAdvancePay = customers.filter { it.currentBalance < 0 }.sumOf { -it.currentBalance }
        val supplierPayable = suppliers.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }

        val activeTodayTxns = txns.filter {
            it.txnTimeMillis >= startOfToday && it.status != TxnStatus.REVERSED.name
        }
        val todayRec = activeTodayTxns.filter {
            it.txnType == TxnType.PAYMENT_RECEIVED.name
        }.sumOf { it.amount }
        val todayGiv = activeTodayTxns.filter {
            it.txnType == TxnType.CREDIT_GIVEN.name || it.txnType == TxnType.SUPPLIER_PAYMENT.name
        }.sumOf { it.amount }

        val todaySaleTotal = sales.filter { it.saleTimeMillis >= startOfToday }.sumOf { it.totalAmount }
        val todayExpTotal = expenses.filter { it.expenseTimeMillis >= startOfToday }.sumOf { it.amount }
        val overdue = customers.count {
            it.currentBalance > 0 && it.dueDateMillis != null && it.dueDateMillis < now
        }

        DashboardSummary(
            totalToReceive = customerReceive,
            totalToPay = customerAdvancePay + supplierPayable,
            todayReceived = todayRec,
            todayGiven = todayGiv,
            todaySales = todaySaleTotal,
            todayExpenses = todayExpTotal,
            customerCount = customers.size,
            supplierCount = suppliers.size,
            overdueCount = overdue
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DashboardSummary())

    init {
        bootstrapSession()
    }

    private fun bootstrapSession() {
        viewModelScope.launch {
            _isSyncing.value = true
            val lang = localPrefs.value.language
            repository.ensureUserAndDefaultBusiness(userDisplayName, userEmail, lang)
                .onSuccess { prefs.updateLastSyncMillis() }
                .onFailure { _statusMessage.value = it.localizedMessage }
            _isAdmin.value = repository.checkIsAdmin(currentUserId, userEmail)
            _isSyncing.value = false
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.ensureUserAndDefaultBusiness(userDisplayName, userEmail, localPrefs.value.language)
                .onSuccess {
                    prefs.updateLastSyncMillis()
                    _statusMessage.value = "Cloud khata synced successfully"
                }
                .onFailure { _statusMessage.value = "Offline mode: changes queued locally" }
            _isSyncing.value = false
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun setCustomerSearch(q: String) {
        _customerSearch.value = q
    }

    fun setCustomerFilter(f: CustomerFilter) {
        _customerFilter.value = f
    }

    fun setPartySort(s: PartySort) {
        _partySort.value = s
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            prefs.setLanguage(language)
            repository.updateUserLanguage(language)
        }
    }

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch { prefs.setThemePreference(theme) }
    }

    fun setNotificationPreferences(reminders: Boolean, summary: Boolean) {
        viewModelScope.launch { prefs.setNotificationSettings(reminders, summary) }
    }

    fun savePin(pin: String) {
        viewModelScope.launch {
            prefs.setPin(pin)
            _statusMessage.value = "App PIN lock enabled"
        }
    }

    fun disablePin() {
        viewModelScope.launch {
            prefs.disablePin()
            _statusMessage.value = "App PIN lock disabled"
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setBiometricEnabled(enabled) }
    }

    fun selectBusiness(businessId: String) {
        viewModelScope.launch {
            repository.setActiveBusiness(businessId)
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun saveBusiness(business: BusinessProfile, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveBusiness(business)
                .onSuccess { newId ->
                    repository.setActiveBusiness(newId)
                    prefs.updateLastSyncMillis()
                    _statusMessage.value = "Business profile saved"
                    onDone()
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save business" }
        }
    }

    fun addCustomer(
        name: String,
        phone: String?,
        address: String?,
        category: String,
        notes: String?,
        photoUri: String?,
        openingBalance: Double,
        dueDateMillis: Long?,
        onSuccess: (String) -> Unit = {}
    ) {
        val bizId = activeBusiness.value?.id ?: return
        viewModelScope.launch {
            repository.addCustomer(
                businessId = bizId,
                name = name,
                phone = phone,
                address = address,
                category = category,
                notes = notes,
                photoUri = photoUri,
                openingBalance = openingBalance,
                dueDateMillis = dueDateMillis
            ).onSuccess { id ->
                prefs.updateLastSyncMillis()
                _statusMessage.value = "$name added to Khata"
                onSuccess(id)
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to add customer"
            }
        }
    }

    fun updateCustomer(
        customerId: String,
        name: String,
        phone: String?,
        address: String?,
        category: String,
        notes: String?,
        photoUri: String?,
        dueDateMillis: Long?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateCustomer(customerId, name, phone, address, category, notes, photoUri, dueDateMillis)
                .onSuccess {
                    prefs.updateLastSyncMillis()
                    _statusMessage.value = "Customer updated"
                    onSuccess()
                }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun deleteCustomer(customerId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteCustomer(customerId)
                .onSuccess {
                    _statusMessage.value = "Customer removed"
                    onSuccess()
                }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun addSupplier(
        name: String,
        companyName: String?,
        phone: String?,
        address: String?,
        gstNumber: String?,
        openingBalance: Double,
        dueDateMillis: Long?,
        onSuccess: (String) -> Unit = {}
    ) {
        val bizId = activeBusiness.value?.id ?: return
        viewModelScope.launch {
            repository.addSupplier(bizId, name, companyName, phone, address, gstNumber, openingBalance, dueDateMillis)
                .onSuccess { id ->
                    prefs.updateLastSyncMillis()
                    _statusMessage.value = "$name added to Suppliers"
                    onSuccess(id)
                }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun updateSupplier(
        supplierId: String,
        name: String,
        companyName: String?,
        phone: String?,
        address: String?,
        gstNumber: String?,
        dueDateMillis: Long?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateSupplier(supplierId, name, companyName, phone, address, gstNumber, dueDateMillis)
                .onSuccess {
                    _statusMessage.value = "Supplier updated"
                    onSuccess()
                }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun deleteSupplier(supplierId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteSupplier(supplierId)
                .onSuccess {
                    _statusMessage.value = "Supplier removed"
                    onSuccess()
                }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun recordTransaction(
        partyId: String,
        partyName: String,
        partyType: PartyType,
        txnType: TxnType,
        amount: Double,
        paymentMode: PaymentMode,
        description: String?,
        itemDetails: String?,
        referenceNumber: String?,
        attachmentUri: String?,
        txnTimeMillis: Long,
        onSuccess: (String) -> Unit = {}
    ) {
        val bizId = activeBusiness.value?.id ?: return
        viewModelScope.launch {
            repository.recordLedgerTransaction(
                businessId = bizId,
                partyId = partyId,
                partyName = partyName,
                partyType = partyType,
                txnType = txnType,
                amount = amount,
                paymentMode = paymentMode,
                description = description,
                itemDetails = itemDetails,
                referenceNumber = referenceNumber,
                attachmentUri = attachmentUri,
                txnTimeMillis = txnTimeMillis
            ).onSuccess { id ->
                prefs.updateLastSyncMillis()
                _statusMessage.value = "Transaction saved (${Formatters.formatRupee(amount)})"
                onSuccess(id)
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to save transaction"
            }
        }
    }

    fun editTransaction(
        existing: LedgerTransaction,
        newAmount: Double,
        newPaymentMode: PaymentMode,
        newDescription: String?,
        newItemDetails: String?,
        newReferenceNumber: String?,
        newTxnTimeMillis: Long,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.editLedgerTransaction(
                existing, newAmount, newPaymentMode, newDescription, newItemDetails, newReferenceNumber, newTxnTimeMillis
            ).onSuccess {
                prefs.updateLastSyncMillis()
                _statusMessage.value = "Transaction updated with audit log"
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Cannot edit transaction"
            }
        }
    }

    fun reverseTransaction(existing: LedgerTransaction, reason: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.reverseLedgerTransaction(existing, reason)
                .onSuccess {
                    prefs.updateLastSyncMillis()
                    _statusMessage.value = "Transaction reversed & balance restored"
                    onSuccess()
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to reverse" }
        }
    }

    fun addExpense(
        amount: Double,
        category: String,
        paymentMode: PaymentMode,
        notes: String?,
        receiptUri: String?,
        expenseTimeMillis: Long,
        onSuccess: () -> Unit = {}
    ) {
        val bizId = activeBusiness.value?.id ?: return
        viewModelScope.launch {
            repository.addExpense(bizId, amount, category, paymentMode, notes, receiptUri, expenseTimeMillis)
                .onSuccess {
                    prefs.updateLastSyncMillis()
                    _statusMessage.value = "Expense recorded (${Formatters.formatRupee(amount)})"
                    onSuccess()
                }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
                .onSuccess { _statusMessage.value = "Expense deleted" }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun addSale(
        customerId: String?,
        customerName: String,
        productName: String,
        quantity: Double,
        unitPrice: Double,
        discount: Double,
        taxPercent: Double,
        paidAmount: Double,
        paymentMode: PaymentMode,
        saleTimeMillis: Long,
        onSuccess: () -> Unit = {}
    ) {
        val bizId = activeBusiness.value?.id ?: return
        viewModelScope.launch {
            repository.addSale(
                bizId, customerId, customerName, productName, quantity, unitPrice,
                discount, taxPercent, paidAmount, paymentMode, saleTimeMillis
            ).onSuccess {
                prefs.updateLastSyncMillis()
                _statusMessage.value = "Sale invoice saved"
                onSuccess()
            }.onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun logReminder(
        partyId: String,
        partyName: String,
        partyPhone: String?,
        amount: Double,
        channel: ReminderChannel,
        message: String,
        scheduledAtMillis: Long = System.currentTimeMillis(),
        status: ReminderStatus = ReminderStatus.SENT
    ) {
        val bizId = activeBusiness.value?.id ?: return
        viewModelScope.launch {
            repository.logReminder(
                bizId, partyId, partyName, partyPhone, amount, channel, message, scheduledAtMillis, status
            ).onSuccess {
                _statusMessage.value = if (status == ReminderStatus.SCHEDULED) "Reminder scheduled" else "Reminder logged"
            }.onFailure {
                _statusMessage.value = it.localizedMessage
            }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { repository.markNotificationRead(id) }
    }

    fun addSystemNotification(title: String, body: String, category: NotificationCategory) {
        viewModelScope.launch { repository.addSystemNotification(title, body, category) }
    }

    fun updatePlanTier(tier: PlanTier) {
        viewModelScope.launch {
            repository.updatePlanTier(tier)
                .onSuccess { _statusMessage.value = "Plan updated to ${tier.name}" }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun submitSupportTicket(subject: String, description: String, category: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.submitSupportTicket(userEmail, subject, description, category)
                .onSuccess {
                    _statusMessage.value = "Support ticket submitted"
                    onSuccess()
                }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun adminUpdateTicket(ticketId: String, status: TicketStatus, reply: String) {
        viewModelScope.launch {
            repository.adminUpdateSupportTicket(ticketId, status, reply)
                .onSuccess { _statusMessage.value = "Ticket updated" }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun adminPublishAnnouncement(title: String, message: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.adminPublishAnnouncement(title, message)
                .onSuccess {
                    _statusMessage.value = "Announcement published"
                    onSuccess()
                }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun adminUpdateUser(targetUid: String, status: String, plan: PlanTier) {
        viewModelScope.launch {
            repository.adminUpdateUserStatusAndPlan(targetUid, status, plan)
                .onSuccess { _statusMessage.value = "User updated" }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun deleteAccountProfile(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteUserAccountData()
                .onSuccess { onDone() }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    private fun <T> Flow<T>.toUiState(fallbackError: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w("KhataVM", fallbackError, rootError)
                emit(UiState.Error(rootError.message ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )
}
