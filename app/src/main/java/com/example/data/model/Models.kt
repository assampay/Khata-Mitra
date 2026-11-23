package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppLanguage(val code: String, val label: String) {
    EN("EN", "English"),
    HI("HI", "हिन्दी"),
    AS("AS", "অসমীয়া"),
    BN("BN", "বাংলা");

    companion object {
        fun fromRaw(raw: String?): AppLanguage =
            entries.firstOrNull { it.name == raw } ?: EN
    }
}

enum class PlanTier {
    FREE, PREMIUM;

    companion object {
        fun fromRaw(raw: String?): PlanTier =
            entries.firstOrNull { it.name == raw } ?: FREE
    }
}

enum class PartyType {
    CUSTOMER, SUPPLIER;

    companion object {
        fun fromRaw(raw: String?): PartyType =
            entries.firstOrNull { it.name == raw } ?: CUSTOMER
    }
}

enum class TxnType(val label: String) {
    CREDIT_GIVEN("You Gave"),
    PAYMENT_RECEIVED("You Got"),
    PURCHASE_PAYABLE("Purchase"),
    SUPPLIER_PAYMENT("Paid Supplier"),
    DEBIT_ADJUSTMENT("Debit Adj."),
    CREDIT_ADJUSTMENT("Credit Adj."),
    OPENING_BALANCE("Opening Bal."),
    REVERSAL("Reversal"),
    REFUND("Refund");

    companion object {
        fun fromRaw(raw: String?): TxnType =
            entries.firstOrNull { it.name == raw } ?: CREDIT_GIVEN
    }
}

enum class PaymentMode(val label: String) {
    CASH("Cash"),
    UPI("UPI"),
    BANK("Bank"),
    CREDIT("Credit"),
    OTHER("Other");

    companion object {
        fun fromRaw(raw: String?): PaymentMode =
            entries.firstOrNull { it.name == raw } ?: CASH
    }
}

enum class TxnStatus {
    ACTIVE, EDITED, REVERSED;

    companion object {
        fun fromRaw(raw: String?): TxnStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class ReminderChannel {
    WHATSAPP, SMS, NOTIFICATION, SCHEDULED;

    companion object {
        fun fromRaw(raw: String?): ReminderChannel =
            entries.firstOrNull { it.name == raw } ?: WHATSAPP
    }
}

enum class ReminderStatus {
    SENT, SCHEDULED, DISMISSED;

    companion object {
        fun fromRaw(raw: String?): ReminderStatus =
            entries.firstOrNull { it.name == raw } ?: SENT
    }
}

enum class NotificationCategory {
    REMINDER, PAYMENT, SUMMARY, BACKUP, ALERT;

    companion object {
        fun fromRaw(raw: String?): NotificationCategory =
            entries.firstOrNull { it.name == raw } ?: ALERT
    }
}

enum class TicketStatus {
    OPEN, IN_PROGRESS, RESOLVED;

    companion object {
        fun fromRaw(raw: String?): TicketStatus =
            entries.firstOrNull { it.name == raw } ?: OPEN
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val phone: String? = null,
    val planTier: String = PlanTier.FREE.name,
    val activeBusinessId: String? = null,
    val language: String = AppLanguage.EN.name,
    val accountStatus: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.take(80).ifBlank { "Merchant" },
        "email" to email?.take(120),
        "phone" to phone?.take(20),
        "planTier" to planTier,
        "activeBusinessId" to activeBusinessId?.take(128),
        "language" to language,
        "accountStatus" to "ACTIVE",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class BusinessProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessName: String = "",
    val ownerName: String = "",
    val phone: String? = null,
    val businessType: String = "Kirana & General Store",
    val address: String? = null,
    val gstNumber: String? = null,
    val upiId: String? = null,
    val openingDate: String? = null,
    val logoUri: String? = null,
    val signatureUri: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "businessName" to businessName.take(100),
        "ownerName" to ownerName.take(80),
        "phone" to phone?.takeIf { it.isNotBlank() }?.take(20),
        "businessType" to businessType.take(50),
        "address" to address?.takeIf { it.isNotBlank() }?.take(200),
        "gstNumber" to gstNumber?.takeIf { it.isNotBlank() }?.take(20),
        "upiId" to upiId?.takeIf { it.isNotBlank() }?.take(100),
        "openingDate" to openingDate?.takeIf { it.isNotBlank() }?.take(30),
        "logoUri" to logoUri?.takeIf { it.isNotBlank() }?.take(500),
        "signatureUri" to signatureUri?.takeIf { it.isNotBlank() }?.take(500),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val name: String = "",
    val phone: String? = null,
    val address: String? = null,
    val category: String = "Regular",
    val notes: String? = null,
    val photoUri: String? = null,
    val openingBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val dueDateMillis: Long? = null,
    val lastTransactionAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "businessId" to businessId,
        "name" to name.take(100),
        "phone" to phone?.takeIf { it.isNotBlank() }?.take(20),
        "address" to address?.takeIf { it.isNotBlank() }?.take(200),
        "category" to category.take(50).ifBlank { "Regular" },
        "notes" to notes?.takeIf { it.isNotBlank() }?.take(500),
        "photoUri" to photoUri?.takeIf { it.isNotBlank() }?.take(500),
        "openingBalance" to openingBalance,
        "currentBalance" to currentBalance,
        "dueDateMillis" to dueDateMillis?.takeIf { it >= 0L },
        "lastTransactionAtMillis" to lastTransactionAtMillis?.takeIf { it >= 0L },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class Supplier(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val name: String = "",
    val companyName: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val gstNumber: String? = null,
    val openingBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val dueDateMillis: Long? = null,
    val lastTransactionAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "businessId" to businessId,
        "name" to name.take(100),
        "companyName" to companyName?.takeIf { it.isNotBlank() }?.take(100),
        "phone" to phone?.takeIf { it.isNotBlank() }?.take(20),
        "address" to address?.takeIf { it.isNotBlank() }?.take(200),
        "gstNumber" to gstNumber?.takeIf { it.isNotBlank() }?.take(20),
        "openingBalance" to openingBalance,
        "currentBalance" to currentBalance,
        "dueDateMillis" to dueDateMillis?.takeIf { it >= 0L },
        "lastTransactionAtMillis" to lastTransactionAtMillis?.takeIf { it >= 0L },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class LedgerTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val partyId: String = "",
    val partyName: String = "",
    val partyType: String = PartyType.CUSTOMER.name,
    val txnType: String = TxnType.CREDIT_GIVEN.name,
    val amount: Double = 0.0,
    val balanceImpact: Double = 0.0,
    val paymentMode: String = PaymentMode.CASH.name,
    val description: String? = null,
    val itemDetails: String? = null,
    val referenceNumber: String? = null,
    val attachmentUri: String? = null,
    val status: String = TxnStatus.ACTIVE.name,
    val reversedByTxnId: String? = null,
    val txnTimeMillis: Long = System.currentTimeMillis(),
    val auditHistory: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "businessId" to businessId,
        "partyId" to partyId,
        "partyName" to partyName.take(100),
        "partyType" to partyType,
        "txnType" to txnType,
        "amount" to amount,
        "balanceImpact" to balanceImpact,
        "paymentMode" to paymentMode,
        "description" to description?.takeIf { it.isNotBlank() }?.take(500),
        "itemDetails" to itemDetails?.takeIf { it.isNotBlank() }?.take(300),
        "referenceNumber" to referenceNumber?.takeIf { it.isNotBlank() }?.take(80),
        "attachmentUri" to attachmentUri?.takeIf { it.isNotBlank() }?.take(500),
        "status" to status,
        "reversedByTxnId" to reversedByTxnId?.takeIf { it.isNotBlank() }?.take(128),
        "txnTimeMillis" to txnTimeMillis,
        "auditHistory" to auditHistory.take(20),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val amount: Double = 0.0,
    val category: String = "Other",
    val paymentMode: String = PaymentMode.CASH.name,
    val notes: String? = null,
    val receiptUri: String? = null,
    val expenseTimeMillis: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "businessId" to businessId,
        "amount" to amount,
        "category" to category.take(50),
        "paymentMode" to paymentMode,
        "notes" to notes?.takeIf { it.isNotBlank() }?.take(300),
        "receiptUri" to receiptUri?.takeIf { it.isNotBlank() }?.take(500),
        "expenseTimeMillis" to expenseTimeMillis,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class SaleRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val invoiceNumber: String = "",
    val customerId: String? = null,
    val customerName: String = "",
    val productName: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val discount: Double = 0.0,
    val taxPercent: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val paymentMode: String = PaymentMode.CASH.name,
    val saleTimeMillis: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "businessId" to businessId,
        "invoiceNumber" to invoiceNumber.take(40),
        "customerId" to customerId?.takeIf { it.isNotBlank() }?.take(128),
        "customerName" to customerName.take(100),
        "productName" to productName.take(120),
        "quantity" to quantity,
        "unitPrice" to unitPrice,
        "discount" to discount,
        "taxPercent" to taxPercent,
        "totalAmount" to totalAmount,
        "paidAmount" to paidAmount,
        "remainingBalance" to remainingBalance,
        "paymentMode" to paymentMode,
        "saleTimeMillis" to saleTimeMillis,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class ReminderRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val partyId: String = "",
    val partyName: String = "",
    val partyPhone: String? = null,
    val amount: Double = 0.0,
    val channel: String = ReminderChannel.WHATSAPP.name,
    val message: String = "",
    val scheduledAtMillis: Long = System.currentTimeMillis(),
    val status: String = ReminderStatus.SENT.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "businessId" to businessId,
        "partyId" to partyId,
        "partyName" to partyName.take(100),
        "partyPhone" to partyPhone?.takeIf { it.isNotBlank() }?.take(20),
        "amount" to amount,
        "channel" to channel,
        "message" to message.take(500),
        "scheduledAtMillis" to scheduledAtMillis,
        "status" to status,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = NotificationCategory.ALERT.name,
    val readFlag: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title.take(120),
        "body" to body.take(400),
        "category" to category,
        "readFlag" to readFlag,
        "createdAtMillis" to createdAtMillis,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String? = null,
    val subject: String = "",
    val description: String = "",
    val category: String = "General",
    val status: String = TicketStatus.OPEN.name,
    val adminReply: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "userEmail" to userEmail?.takeIf { it.isNotBlank() }?.take(120),
        "subject" to subject.take(120),
        "description" to description.take(1000),
        "category" to category.take(50),
        "status" to TicketStatus.OPEN.name,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val activeFlag: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "title" to title.take(120),
        "message" to message.take(500),
        "activeFlag" to activeFlag,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
