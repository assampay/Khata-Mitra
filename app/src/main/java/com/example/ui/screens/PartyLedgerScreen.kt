package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.BusinessProfile
import com.example.data.model.Customer
import com.example.data.model.LedgerTransaction
import com.example.data.model.PartyType
import com.example.data.model.ReminderChannel
import com.example.data.model.Supplier
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.LocalKhataColors
import com.example.ui.theme.moneyTextStyle
import com.example.util.Formatters
import com.example.util.LocalAppStrings
import com.example.util.PdfAndCsvExporter
import com.example.util.ShareAndIntents
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyLedgerScreen(
    partyType: PartyType,
    customer: Customer?,
    supplier: Supplier?,
    business: BusinessProfile?,
    transactions: List<LedgerTransaction>,
    onBack: () -> Unit,
    onGiveCreditOrPurchase: () -> Unit,
    onReceiveOrPay: () -> Unit,
    onEditParty: () -> Unit,
    onDeleteParty: () -> Unit,
    onTransactionClick: (LedgerTransaction) -> Unit,
    onOpenUpiCollect: (Double) -> Unit,
    onLogReminder: (ReminderChannel, String) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val khataColors = LocalKhataColors.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val partyName = if (partyType == PartyType.CUSTOMER) customer?.name ?: "Customer" else supplier?.name ?: "Supplier"
    val partyPhone = if (partyType == PartyType.CUSTOMER) customer?.phone else supplier?.phone
    val currentBalance = if (partyType == PartyType.CUSTOMER) customer?.currentBalance ?: 0.0 else supplier?.currentBalance ?: 0.0
    val subtitleInfo = if (partyType == PartyType.CUSTOMER) {
        listOfNotNull(customer?.phone, customer?.category, customer?.address).filter { it.isNotBlank() }.joinToString(" • ")
    } else {
        listOfNotNull(supplier?.companyName, supplier?.phone, supplier?.gstNumber?.let { "GST: $it" }).filter { it.isNotBlank() }.joinToString(" • ")
    }

    val partyTxns = remember(transactions, customer?.id, supplier?.id) {
        val targetId = if (partyType == PartyType.CUSTOMER) customer?.id else supplier?.id
        transactions.filter { it.partyId == targetId }.sortedByDescending { it.txnTimeMillis }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val photo = if (partyType == PartyType.CUSTOMER) customer?.photoUri else null
                        if (!photo.isNullOrBlank()) {
                            AsyncImage(
                                model = photo,
                                contentDescription = partyName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Column {
                            Text(partyName, style = MaterialTheme.typography.titleLarge)
                            if (subtitleInfo.isNotBlank()) {
                                Text(
                                    text = subtitleInfo,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ledger_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!partyPhone.isNullOrBlank()) {
                        IconButton(onClick = { ShareAndIntents.dialPhone(context, partyPhone) }) {
                            Icon(Icons.Filled.Call, contentDescription = "Call")
                        }
                    }
                    IconButton(onClick = onEditParty) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onGiveCreditOrPurchase,
                        colors = ButtonDefaults.buttonColors(containerColor = khataColors.debitRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("ledger_debit_button")
                    ) {
                        Text(
                            text = if (partyType == PartyType.CUSTOMER) strings.giveCreditBtn else "PURCHASE ₹",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onReceiveOrPay,
                        colors = ButtonDefaults.buttonColors(containerColor = khataColors.creditGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("ledger_credit_button")
                    ) {
                        Text(
                            text = if (partyType == PartyType.CUSTOMER) strings.receivePaymentBtn else "PAID ₹",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Balance Summary Card
            item {
                val (statusLabel, accent) = when {
                    abs(currentBalance) < 0.01 -> "Account Settled" to MaterialTheme.colorScheme.onSurfaceVariant
                    partyType == PartyType.CUSTOMER && currentBalance > 0 -> strings.youWillGet to khataColors.creditGreen
                    partyType == PartyType.CUSTOMER && currentBalance < 0 -> strings.youWillGive to khataColors.debitRed
                    partyType == PartyType.SUPPLIER && currentBalance > 0 -> "Amount Payable" to khataColors.debitRed
                    else -> "Advance Paid" to khataColors.creditGreen
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = accent
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Formatters.formatRupee(abs(currentBalance)),
                                    style = moneyTextStyle(28),
                                    color = accent
                                )
                            }
                            Text(
                                text = "${partyTxns.size} Entries",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Row: WhatsApp, SMS, PDF, UPI QR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LedgerActionPill(
                                label = "WhatsApp",
                                icon = Icons.AutoMirrored.Filled.Message,
                                tint = khataColors.creditGreen,
                                onClick = {
                                    val msg = ShareAndIntents.buildReminderMessage(
                                        partyName,
                                        abs(currentBalance),
                                        business?.businessName ?: "Our Store"
                                    )
                                    onLogReminder(ReminderChannel.WHATSAPP, msg)
                                    ShareAndIntents.shareTextViaWhatsAppOrSystem(context, partyPhone, msg)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            LedgerActionPill(
                                label = "SMS",
                                icon = Icons.Filled.Sms,
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = {
                                    val msg = ShareAndIntents.buildReminderMessage(
                                        partyName,
                                        abs(currentBalance),
                                        business?.businessName ?: "Our Store"
                                    )
                                    onLogReminder(ReminderChannel.SMS, msg)
                                    ShareAndIntents.sendSmsReminder(context, partyPhone, msg)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            LedgerActionPill(
                                label = "PDF",
                                icon = Icons.Filled.PictureAsPdf,
                                tint = khataColors.debitRed,
                                onClick = {
                                    val uri = PdfAndCsvExporter.generatePartyStatementPdf(
                                        context = context,
                                        business = business,
                                        partyName = partyName,
                                        partyPhone = partyPhone,
                                        currentBalance = currentBalance,
                                        isCustomer = partyType == PartyType.CUSTOMER,
                                        transactions = partyTxns
                                    )
                                    ShareAndIntents.shareFileUri(
                                        context, uri, "application/pdf", "Share Statement PDF"
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (partyType == PartyType.CUSTOMER && currentBalance > 0) {
                                LedgerActionPill(
                                    label = "UPI QR",
                                    icon = Icons.Filled.QrCode2,
                                    tint = khataColors.saffronGold,
                                    onClick = { onOpenUpiCollect(currentBalance) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            if (partyTxns.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No Ledger Entries Yet",
                        subtitle = "Tap the buttons below to record your first credit or payment entry."
                    )
                }
            } else {
                items(partyTxns, key = { it.id }) { txn ->
                    TransactionRowCard(txn = txn, onClick = { onTransactionClick(txn) })
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove $partyName?") },
            text = { Text("This removes the party profile from your active list. Transaction audit records remain preserved.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteParty()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun LedgerActionPill(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.SemiBold
        )
    }
}
