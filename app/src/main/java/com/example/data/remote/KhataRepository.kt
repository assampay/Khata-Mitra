package com.example.data.remote

import android.content.Context
import com.example.R
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KhataRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        createConfiguredFirestore(context.applicationContext)
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.withFirestoreErrorLogging(
        uid: String,
        operationType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    // --- Real-Time Streams ---

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        require(uid.isNotBlank())
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .withFirestoreErrorLogging(uid, OperationType.GET, path)
        )
    }

    fun observeBusinesses(uid: String): Flow<List<BusinessProfile>> = flow {
        require(uid.isNotBlank())
        val path = "users/$uid/businesses"
        emitAll(
            db.collection("users").document(uid).collection("businesses")
                .snapshots()
                .map { it.toObjects(BusinessProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .withFirestoreErrorLogging(uid, OperationType.LIST, path)
        )
    }

    fun observeCustomers(uid: String): Flow<List<Customer>> = flow {
        require(uid.isNotBlank())
        val path = "users/$uid/customers"
        emitAll(
            db.collection("users").document(uid).collection("customers")
                .snapshots()
                .map { it.toObjects(Customer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .withFirestoreErrorLogging(uid, OperationType.LIST, path)
        )
    }

    fun observeSuppliers(uid: String): Flow<List<Supplier>> = flow {
        require(uid.isNotBlank())
        val path = "users/$uid/suppliers"
        emitAll(
            db.collection("users").document(uid).collection("suppliers")
                .snapshots()
                .map { it.toObjects(Supplier::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .withFirestoreErrorLogging(uid, OperationType.LIST, path)
        )
    }

    fun observeTransactions(uid: String): Flow<List<LedgerTransaction>> = flow {
        require(uid.isNotBlank())
        val path = "users/$uid/transactions"
        emitAll(
            db.collection("users").document(uid).collection("transactions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(LedgerTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.txnTimeMillis }
                }
                .withFirestoreErrorLogging(uid, OperationType.LIST, path)
        )
    }

    fun observeExpenses(uid: String): Flow<List<Expense>> = flow {
        require(uid.isNotBlank())
        val path = "users/$uid/expenses"
        emitAll(
            db.collection("users").document(uid).collection("expenses")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Expense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.expenseTimeMillis }
                }
                .withFirestoreErrorLogging(uid, OperationType.LIST, path)
        )
    }

    fun observeSales(uid: String): Flow<List<SaleRecord>> = flow {
        require(uid.isNotBlank())
        val path = "users/$uid/sales"
        emitAll(
            db.collection("users").document(uid).collection("sales")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SaleRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.saleTimeMillis }
                }
                .withFirestoreErrorLogging(uid, OperationType.LIST, path)
        )
    }

    fun observeReminders(uid: String): Flow<List<ReminderRecord>> = flow {
        require(uid.isNotBlank())
        val path = "users/$uid/reminders"
        emitAll(
            db.collection("users").document(uid).collection("reminders")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ReminderRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.scheduledAtMillis }
                }
                .withFirestoreErrorLogging(uid, OperationType.LIST, path)
        )
    }

    fun observeNotifications(uid: String): Flow<List<AppNotification>> = flow {
        require(uid.isNotBlank())
        val path = "users/$uid/notifications"
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAtMillis }
                }
                .withFirestoreErrorLogging(uid, OperationType.LIST, path)
        )
    }

    fun observeMySupportTickets(uid: String): Flow<List<SupportTicket>> = flow {
        require(uid.isNotBlank())
        val path = "support_tickets"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { it.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .withFirestoreErrorLogging(uid, OperationType.LIST, path)
        )
    }

    fun observeAnnouncements(uid: String): Flow<List<Announcement>> = flow {
        require(uid.isNotBlank())
        val path = "announcements"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { it.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .withFirestoreErrorLogging(uid, OperationType.LIST, path)
        )
    }

    // --- Admin Streams (only collected when isAdmin is true) ---

    fun observeAllUsersAdmin(uid: String): Flow<List<UserProfile>> = flow {
        require(uid.isNotBlank())
        emitAll(
            db.collection("users")
                .snapshots()
                .map { it.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .withFirestoreErrorLogging(uid, OperationType.LIST, "users")
        )
    }

    fun observeAllSupportTicketsAdmin(uid: String): Flow<List<SupportTicket>> = flow {
        require(uid.isNotBlank())
        emitAll(
            db.collection("support_tickets")
                .snapshots()
                .map { it.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .withFirestoreErrorLogging(uid, OperationType.LIST, "support_tickets")
        )
    }

    suspend fun checkIsAdmin(uid: String, email: String?): Boolean {
        if (email == "sahib7536@gmail.com") return true
        return try {
            val doc = db.collection("admins").document(uid).get().await()
            doc.exists()
        } catch (_: Exception) {
            false
        }
    }

    // --- Bootstrap & Profile ---

    suspend fun ensureUserAndDefaultBusiness(
        displayName: String,
        email: String?,
        preferredLanguage: AppLanguage
    ): Result<String> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val userSnap = userRef.get().await()

            val businessesCol = userRef.collection("businesses")
            val existingBusinesses = businessesCol.get().await()
            var activeBizId = userSnap.getString("activeBusinessId")

            if (existingBusinesses.isEmpty) {
                val bizRef = businessesCol.document()
                activeBizId = bizRef.id
                val defaultBiz = BusinessProfile(
                    userId = uid,
                    businessName = if (displayName.isNotBlank()) "${displayName.split(" ").first()}'s Store" else "My Business Khata",
                    ownerName = displayName.ifBlank { "Shop Owner" },
                    businessType = "Kirana & General Store"
                )
                bizRef.set(defaultBiz.toCreateMap()).await()
            } else if (activeBizId.isNullOrBlank()) {
                activeBizId = existingBusinesses.documents.first().id
            }

            if (!userSnap.exists()) {
                val profile = UserProfile(
                    userId = uid,
                    displayName = displayName.ifBlank { "Merchant" },
                    email = email,
                    planTier = PlanTier.FREE.name,
                    activeBusinessId = activeBizId,
                    language = preferredLanguage.name
                )
                userRef.set(profile.toCreateMap()).await()
            } else if (userSnap.getString("activeBusinessId").isNullOrBlank() && !activeBizId.isNullOrBlank()) {
                userRef.update(
                    mapOf(
                        "activeBusinessId" to activeBizId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(activeBizId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserLanguage(language: AppLanguage): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            docPath = ref.path
            ref.update(
                mapOf(
                    "language" to language.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setActiveBusiness(businessId: String): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            require(businessId.isNotBlank())
            val ref = db.collection("users").document(uid)
            docPath = ref.path
            ref.update(
                mapOf(
                    "activeBusinessId" to businessId.take(128),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePlanTier(tier: PlanTier): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            docPath = ref.path
            ref.update(
                mapOf(
                    "planTier" to tier.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- Businesses ---

    suspend fun saveBusiness(business: BusinessProfile): Result<String> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            require(business.businessName.isNotBlank()) { "Business name is required" }
            require(business.ownerName.isNotBlank()) { "Owner name is required" }
            val col = db.collection("users").document(uid).collection("businesses")
            if (business.id.isBlank()) {
                val ref = col.document()
                docPath = ref.path
                ref.set(business.copy(userId = uid).toCreateMap()).await()
                Result.success(ref.id)
            } else {
                val ref = col.document(business.id)
                docPath = ref.path
                val updates = mutableMapOf<String, Any>(
                    "businessName" to business.businessName.take(100),
                    "ownerName" to business.ownerName.take(80),
                    "businessType" to business.businessType.take(50).ifBlank { "Retail" },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                business.phone?.takeIf { it.isNotBlank() }?.let { updates["phone"] = it.take(20) }
                business.address?.takeIf { it.isNotBlank() }?.let { updates["address"] = it.take(200) }
                business.gstNumber?.takeIf { it.isNotBlank() }?.let { updates["gstNumber"] = it.take(20) }
                business.upiId?.takeIf { it.isNotBlank() }?.let { updates["upiId"] = it.take(100) }
                business.openingDate?.takeIf { it.isNotBlank() }?.let { updates["openingDate"] = it.take(30) }
                updates["logoUri"] = business.logoUri?.takeIf { it.isNotBlank() }?.take(500) ?: ""
                updates["signatureUri"] = business.signatureUri?.takeIf { it.isNotBlank() }?.take(500) ?: ""
                ref.update(updates).await()
                Result.success(business.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- Customers ---

    suspend fun addCustomer(
        businessId: String,
        name: String,
        phone: String?,
        address: String?,
        category: String,
        notes: String?,
        photoUri: String?,
        openingBalance: Double,
        dueDateMillis: Long?
    ): Result<String> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            require(businessId.isNotBlank())
            require(name.isNotBlank()) { "Customer name is required" }
            val userRef = db.collection("users").document(uid)
            val custRef = userRef.collection("customers").document()
            docPath = custRef.path
            val nowMs = System.currentTimeMillis()
            val customer = Customer(
                userId = uid,
                businessId = businessId,
                name = name.trim(),
                phone = phone?.trim(),
                address = address?.trim(),
                category = category.trim().ifBlank { "Regular" },
                notes = notes?.trim(),
                photoUri = photoUri,
                openingBalance = openingBalance,
                currentBalance = openingBalance,
                dueDateMillis = dueDateMillis,
                lastTransactionAtMillis = if (openingBalance != 0.0) nowMs else null
            )

            val batch = db.batch()
            batch.set(custRef, customer.toCreateMap())
            if (openingBalance != 0.0) {
                val txnRef = userRef.collection("transactions").document()
                val txn = LedgerTransaction(
                    userId = uid,
                    businessId = businessId,
                    partyId = custRef.id,
                    partyName = customer.name,
                    partyType = PartyType.CUSTOMER.name,
                    txnType = TxnType.OPENING_BALANCE.name,
                    amount = kotlin.math.abs(openingBalance),
                    balanceImpact = openingBalance,
                    paymentMode = PaymentMode.CREDIT.name,
                    description = "Opening balance",
                    status = TxnStatus.ACTIVE.name,
                    txnTimeMillis = nowMs,
                    auditHistory = listOf("${formatAuditTimestamp(nowMs)}: Opening balance ₹${kotlin.math.abs(openingBalance)}")
                )
                batch.set(txnRef, txn.toCreateMap())
            }
            batch.commit().await()
            Result.success(custRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCustomer(
        customerId: String,
        name: String,
        phone: String?,
        address: String?,
        category: String,
        notes: String?,
        photoUri: String?,
        dueDateMillis: Long?
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            require(customerId.isNotBlank() && name.isNotBlank())
            val ref = db.collection("users").document(uid).collection("customers").document(customerId)
            docPath = ref.path
            val updates = mutableMapOf<String, Any>(
                "name" to name.trim().take(100),
                "category" to category.trim().take(50).ifBlank { "Regular" },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            phone?.trim()?.takeIf { it.isNotBlank() }?.let { updates["phone"] = it.take(20) }
            address?.trim()?.takeIf { it.isNotBlank() }?.let { updates["address"] = it.take(200) }
            notes?.trim()?.takeIf { it.isNotBlank() }?.let { updates["notes"] = it.take(500) }
            updates["photoUri"] = photoUri?.trim()?.take(500) ?: ""
            dueDateMillis?.takeIf { it >= 0L }?.let { updates["dueDateMillis"] = it }
            ref.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("customers").document(customerId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Suppliers ---

    suspend fun addSupplier(
        businessId: String,
        name: String,
        companyName: String?,
        phone: String?,
        address: String?,
        gstNumber: String?,
        openingBalance: Double,
        dueDateMillis: Long?
    ): Result<String> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            require(businessId.isNotBlank() && name.isNotBlank())
            val userRef = db.collection("users").document(uid)
            val supRef = userRef.collection("suppliers").document()
            docPath = supRef.path
            val nowMs = System.currentTimeMillis()
            val supplier = Supplier(
                userId = uid,
                businessId = businessId,
                name = name.trim(),
                companyName = companyName?.trim(),
                phone = phone?.trim(),
                address = address?.trim(),
                gstNumber = gstNumber?.trim(),
                openingBalance = openingBalance,
                currentBalance = openingBalance,
                dueDateMillis = dueDateMillis,
                lastTransactionAtMillis = if (openingBalance != 0.0) nowMs else null
            )

            val batch = db.batch()
            batch.set(supRef, supplier.toCreateMap())
            if (openingBalance != 0.0) {
                val txnRef = userRef.collection("transactions").document()
                val txn = LedgerTransaction(
                    userId = uid,
                    businessId = businessId,
                    partyId = supRef.id,
                    partyName = supplier.name,
                    partyType = PartyType.SUPPLIER.name,
                    txnType = TxnType.OPENING_BALANCE.name,
                    amount = kotlin.math.abs(openingBalance),
                    balanceImpact = openingBalance,
                    paymentMode = PaymentMode.CREDIT.name,
                    description = "Supplier opening payable",
                    status = TxnStatus.ACTIVE.name,
                    txnTimeMillis = nowMs,
                    auditHistory = listOf("${formatAuditTimestamp(nowMs)}: Opening payable ₹${kotlin.math.abs(openingBalance)}")
                )
                batch.set(txnRef, txn.toCreateMap())
            }
            batch.commit().await()
            Result.success(supRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSupplier(
        supplierId: String,
        name: String,
        companyName: String?,
        phone: String?,
        address: String?,
        gstNumber: String?,
        dueDateMillis: Long?
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("suppliers").document(supplierId)
            docPath = ref.path
            val updates = mutableMapOf<String, Any>(
                "name" to name.trim().take(100),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            companyName?.trim()?.takeIf { it.isNotBlank() }?.let { updates["companyName"] = it.take(100) }
            phone?.trim()?.takeIf { it.isNotBlank() }?.let { updates["phone"] = it.take(20) }
            address?.trim()?.takeIf { it.isNotBlank() }?.let { updates["address"] = it.take(200) }
            gstNumber?.trim()?.takeIf { it.isNotBlank() }?.let { updates["gstNumber"] = it.take(20) }
            dueDateMillis?.takeIf { it >= 0L }?.let { updates["dueDateMillis"] = it }
            ref.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSupplier(supplierId: String): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("suppliers").document(supplierId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Ledger Transactions (Atomic Batch + Audit Trail) ---

    suspend fun recordLedgerTransaction(
        businessId: String,
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
        txnTimeMillis: Long
    ): Result<String> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            require(amount > 0.0) { "Amount must be greater than zero" }
            require(businessId.isNotBlank() && partyId.isNotBlank())
            val userRef = db.collection("users").document(uid)
            val txnRef = userRef.collection("transactions").document()
            docPath = txnRef.path

            val balanceImpact = computeBalanceImpact(partyType, txnType, amount)
            val auditLine = "${formatAuditTimestamp(System.currentTimeMillis())}: Created ${txnType.label} ₹$amount (${paymentMode.label})"

            val txn = LedgerTransaction(
                userId = uid,
                businessId = businessId,
                partyId = partyId,
                partyName = partyName,
                partyType = partyType.name,
                txnType = txnType.name,
                amount = amount,
                balanceImpact = balanceImpact,
                paymentMode = paymentMode.name,
                description = description?.trim(),
                itemDetails = itemDetails?.trim(),
                referenceNumber = referenceNumber?.trim(),
                attachmentUri = attachmentUri,
                status = TxnStatus.ACTIVE.name,
                txnTimeMillis = txnTimeMillis,
                auditHistory = listOf(auditLine)
            )

            val partyCol = if (partyType == PartyType.CUSTOMER) "customers" else "suppliers"
            val partyRef = userRef.collection(partyCol).document(partyId)

            val batch = db.batch()
            batch.set(txnRef, txn.toCreateMap())
            batch.update(
                partyRef,
                mapOf(
                    "currentBalance" to FieldValue.increment(balanceImpact),
                    "lastTransactionAtMillis" to txnTimeMillis,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (txnType == TxnType.PAYMENT_RECEIVED) {
                val notifRef = userRef.collection("notifications").document()
                val notif = AppNotification(
                    userId = uid,
                    title = "Payment Received",
                    body = "Received ₹${amount.toLong()} from $partyName via ${paymentMode.label}",
                    category = NotificationCategory.PAYMENT.name,
                    readFlag = false,
                    createdAtMillis = System.currentTimeMillis()
                )
                batch.set(notifRef, notif.toCreateMap())
            }

            batch.commit().await()
            Result.success(txnRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun editLedgerTransaction(
        existing: LedgerTransaction,
        newAmount: Double,
        newPaymentMode: PaymentMode,
        newDescription: String?,
        newItemDetails: String?,
        newReferenceNumber: String?,
        newTxnTimeMillis: Long
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            require(existing.status != TxnStatus.REVERSED.name) { "Reversed transactions are locked" }
            require(newAmount > 0.0) { "Amount must be greater than zero" }
            val userRef = db.collection("users").document(uid)
            val txnRef = userRef.collection("transactions").document(existing.id)
            docPath = txnRef.path

            val partyType = PartyType.fromRaw(existing.partyType)
            val txnType = TxnType.fromRaw(existing.txnType)
            val newImpact = computeBalanceImpact(partyType, txnType, newAmount)
            val delta = newImpact - existing.balanceImpact

            val updatedAudit = (existing.auditHistory +
                "${formatAuditTimestamp(System.currentTimeMillis())}: Edited from ₹${existing.amount} to ₹$newAmount (${newPaymentMode.label})")
                .takeLast(20)

            val updates = mutableMapOf<String, Any>(
                "amount" to newAmount,
                "balanceImpact" to newImpact,
                "paymentMode" to newPaymentMode.name,
                "status" to TxnStatus.EDITED.name,
                "txnTimeMillis" to newTxnTimeMillis,
                "auditHistory" to updatedAudit,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            newDescription?.trim()?.takeIf { it.isNotBlank() }?.let { updates["description"] = it.take(500) }
            newItemDetails?.trim()?.takeIf { it.isNotBlank() }?.let { updates["itemDetails"] = it.take(300) }
            newReferenceNumber?.trim()?.takeIf { it.isNotBlank() }?.let { updates["referenceNumber"] = it.take(80) }

            val partyCol = if (partyType == PartyType.CUSTOMER) "customers" else "suppliers"
            val partyRef = userRef.collection(partyCol).document(existing.partyId)

            val batch = db.batch()
            batch.update(txnRef, updates)
            if (delta != 0.0) {
                batch.update(
                    partyRef,
                    mapOf(
                        "currentBalance" to FieldValue.increment(delta),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun reverseLedgerTransaction(
        existing: LedgerTransaction,
        reason: String
    ): Result<String> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            require(existing.status != TxnStatus.REVERSED.name) { "Already reversed" }
            val userRef = db.collection("users").document(uid)
            val origRef = userRef.collection("transactions").document(existing.id)
            val revRef = userRef.collection("transactions").document()
            docPath = origRef.path

            val nowMs = System.currentTimeMillis()
            val reverseImpact = -existing.balanceImpact
            val auditEntry = "${formatAuditTimestamp(nowMs)}: Reversed (${reason.ifBlank { "Correction" }}) via #${revRef.id.take(6)}"
            val updatedAudit = (existing.auditHistory + auditEntry).takeLast(20)

            val reversalTxn = LedgerTransaction(
                userId = uid,
                businessId = existing.businessId,
                partyId = existing.partyId,
                partyName = existing.partyName,
                partyType = existing.partyType,
                txnType = TxnType.REVERSAL.name,
                amount = existing.amount,
                balanceImpact = reverseImpact,
                paymentMode = existing.paymentMode,
                description = "Reversal of #${existing.id.take(6)}: ${reason.ifBlank { "Entry reversed" }}",
                status = TxnStatus.ACTIVE.name,
                txnTimeMillis = nowMs,
                auditHistory = listOf(auditEntry)
            )

            val partyCol = if (PartyType.fromRaw(existing.partyType) == PartyType.CUSTOMER) "customers" else "suppliers"
            val partyRef = userRef.collection(partyCol).document(existing.partyId)

            val batch = db.batch()
            batch.update(
                origRef,
                mapOf(
                    "status" to TxnStatus.REVERSED.name,
                    "reversedByTxnId" to revRef.id,
                    "auditHistory" to updatedAudit,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(revRef, reversalTxn.toCreateMap())
            batch.update(
                partyRef,
                mapOf(
                    "currentBalance" to FieldValue.increment(reverseImpact),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(revRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- Expenses ---

    suspend fun addExpense(
        businessId: String,
        amount: Double,
        category: String,
        paymentMode: PaymentMode,
        notes: String?,
        receiptUri: String?,
        expenseTimeMillis: Long
    ): Result<String> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            require(amount > 0.0 && businessId.isNotBlank())
            val ref = db.collection("users").document(uid).collection("expenses").document()
            docPath = ref.path
            val expense = Expense(
                userId = uid,
                businessId = businessId,
                amount = amount,
                category = category.ifBlank { "Other" },
                paymentMode = paymentMode.name,
                notes = notes?.trim(),
                receiptUri = receiptUri,
                expenseTimeMillis = expenseTimeMillis
            )
            ref.set(expense.toCreateMap()).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("expenses").document(expenseId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Sales ---

    suspend fun addSale(
        businessId: String,
        customerId: String?,
        customerName: String,
        productName: String,
        quantity: Double,
        unitPrice: Double,
        discount: Double,
        taxPercent: Double,
        paidAmount: Double,
        paymentMode: PaymentMode,
        saleTimeMillis: Long
    ): Result<String> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            require(quantity > 0.0 && unitPrice >= 0.0 && businessId.isNotBlank())
            val subtotal = (quantity * unitPrice - discount).coerceAtLeast(0.0)
            val totalAmount = subtotal * (1.0 + (taxPercent.coerceIn(0.0, 100.0) / 100.0))
            val safePaid = paidAmount.coerceIn(0.0, totalAmount)
            val remaining = (totalAmount - safePaid).coerceAtLeast(0.0)
            val invoiceNumber = "INV-${(saleTimeMillis % 1000000).toString().padStart(6, '0')}"

            val userRef = db.collection("users").document(uid)
            val saleRef = userRef.collection("sales").document()
            docPath = saleRef.path

            val sale = SaleRecord(
                userId = uid,
                businessId = businessId,
                invoiceNumber = invoiceNumber,
                customerId = customerId,
                customerName = customerName.trim().ifBlank { "Walk-in Customer" },
                productName = productName.trim(),
                quantity = quantity,
                unitPrice = unitPrice,
                discount = discount.coerceAtLeast(0.0),
                taxPercent = taxPercent.coerceIn(0.0, 100.0),
                totalAmount = totalAmount,
                paidAmount = safePaid,
                remainingBalance = remaining,
                paymentMode = paymentMode.name,
                saleTimeMillis = saleTimeMillis
            )

            val batch = db.batch()
            batch.set(saleRef, sale.toCreateMap())

            // If linked to a customer and there's an unpaid balance, automatically post to customer's khata!
            if (!customerId.isNullOrBlank() && remaining > 0.0) {
                val txnRef = userRef.collection("transactions").document()
                val txn = LedgerTransaction(
                    userId = uid,
                    businessId = businessId,
                    partyId = customerId,
                    partyName = sale.customerName,
                    partyType = PartyType.CUSTOMER.name,
                    txnType = TxnType.CREDIT_GIVEN.name,
                    amount = remaining,
                    balanceImpact = remaining,
                    paymentMode = PaymentMode.CREDIT.name,
                    description = "Invoice $invoiceNumber (${sale.productName})",
                    itemDetails = "${sale.quantity} x ₹${sale.unitPrice}",
                    referenceNumber = invoiceNumber,
                    status = TxnStatus.ACTIVE.name,
                    txnTimeMillis = saleTimeMillis,
                    auditHistory = listOf("${formatAuditTimestamp(saleTimeMillis)}: Auto-posted from Sale $invoiceNumber")
                )
                batch.set(txnRef, txn.toCreateMap())
                batch.update(
                    userRef.collection("customers").document(customerId),
                    mapOf(
                        "currentBalance" to FieldValue.increment(remaining),
                        "lastTransactionAtMillis" to saleTimeMillis,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(saleRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    // --- Reminders & Notifications ---

    suspend fun logReminder(
        businessId: String,
        partyId: String,
        partyName: String,
        partyPhone: String?,
        amount: Double,
        channel: ReminderChannel,
        message: String,
        scheduledAtMillis: Long,
        status: ReminderStatus
    ): Result<String> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            val ref = userRef.collection("reminders").document()
            docPath = ref.path
            val record = ReminderRecord(
                userId = uid,
                businessId = businessId,
                partyId = partyId,
                partyName = partyName,
                partyPhone = partyPhone,
                amount = amount.coerceAtLeast(0.0),
                channel = channel.name,
                message = message.take(500),
                scheduledAtMillis = scheduledAtMillis,
                status = status.name
            )
            val batch = db.batch()
            batch.set(ref, record.toCreateMap())
            val notifRef = userRef.collection("notifications").document()
            batch.set(
                notifRef,
                AppNotification(
                    userId = uid,
                    title = if (status == ReminderStatus.SCHEDULED) "Reminder Scheduled" else "Reminder Sent",
                    body = "${channel.name} reminder for $partyName (₹${amount.toLong()})",
                    category = NotificationCategory.REMINDER.name,
                    readFlag = false,
                    createdAtMillis = System.currentTimeMillis()
                ).toCreateMap()
            )
            batch.commit().await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("notifications").document(notificationId)
            docPath = ref.path
            ref.update(
                mapOf(
                    "readFlag" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addSystemNotification(
        title: String,
        body: String,
        category: NotificationCategory
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("notifications").document()
            docPath = ref.path
            ref.set(
                AppNotification(
                    userId = uid,
                    title = title,
                    body = body,
                    category = category.name,
                    readFlag = false,
                    createdAtMillis = System.currentTimeMillis()
                ).toCreateMap()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    // --- Support & Admin ---

    suspend fun submitSupportTicket(
        userEmail: String?,
        subject: String,
        description: String,
        category: String
    ): Result<String> {
        var docPath = "support_tickets"
        return try {
            val uid = requireUserId()
            val ref = db.collection("support_tickets").document()
            docPath = ref.path
            val ticket = SupportTicket(
                userId = uid,
                userEmail = userEmail,
                subject = subject.trim(),
                description = description.trim(),
                category = category.trim().ifBlank { "General" },
                status = TicketStatus.OPEN.name
            )
            ref.set(ticket.toCreateMap()).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateSupportTicket(
        ticketId: String,
        status: TicketStatus,
        adminReply: String
    ): Result<Unit> {
        var docPath = "support_tickets/$ticketId"
        return try {
            requireUserId()
            val ref = db.collection("support_tickets").document(ticketId)
            val updates = mutableMapOf<String, Any>(
                "status" to status.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (adminReply.isNotBlank()) {
                updates["adminReply"] = adminReply.trim().take(500)
            }
            ref.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminPublishAnnouncement(title: String, message: String): Result<String> {
        var docPath = "announcements"
        return try {
            requireUserId()
            val ref = db.collection("announcements").document()
            docPath = ref.path
            ref.set(
                Announcement(
                    title = title.trim(),
                    message = message.trim(),
                    activeFlag = true
                ).toCreateMap()
            ).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateUserStatusAndPlan(
        targetUserId: String,
        accountStatus: String,
        planTier: PlanTier
    ): Result<Unit> {
        val docPath = "users/$targetUserId"
        return try {
            requireUserId()
            db.collection("users").document(targetUserId).update(
                mapOf(
                    "accountStatus" to accountStatus,
                    "planTier" to planTier.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccountData(): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            userRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    companion object {
        fun computeBalanceImpact(partyType: PartyType, txnType: TxnType, amount: Double): Double {
            return if (partyType == PartyType.CUSTOMER) {
                when (txnType) {
                    TxnType.CREDIT_GIVEN, TxnType.DEBIT_ADJUSTMENT, TxnType.OPENING_BALANCE, TxnType.REFUND -> amount
                    TxnType.PAYMENT_RECEIVED, TxnType.CREDIT_ADJUSTMENT -> -amount
                    TxnType.REVERSAL, TxnType.PURCHASE_PAYABLE, TxnType.SUPPLIER_PAYMENT -> -amount
                }
            } else {
                // For Suppliers: positive currentBalance = Payable to Supplier
                when (txnType) {
                    TxnType.PURCHASE_PAYABLE, TxnType.CREDIT_GIVEN, TxnType.OPENING_BALANCE, TxnType.CREDIT_ADJUSTMENT -> amount
                    TxnType.SUPPLIER_PAYMENT, TxnType.PAYMENT_RECEIVED, TxnType.DEBIT_ADJUSTMENT, TxnType.REFUND -> -amount
                    TxnType.REVERSAL -> -amount
                }
            }
        }

        private fun formatAuditTimestamp(millis: Long): String {
            return SimpleDateFormat("dd MMM HH:mm", Locale.ENGLISH).format(Date(millis))
        }

        fun createConfiguredFirestore(context: Context): FirebaseFirestore {
            val databaseId = context.getString(R.string.firestore_database_id)
            val instance = FirebaseFirestore.getInstance(databaseId)
            try {
                instance.firestoreSettings = firestoreSettings {
                    setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                }
            } catch (_: IllegalStateException) {
                // Settings already applied on this Firestore instance
            }
            return instance
        }
    }
}
