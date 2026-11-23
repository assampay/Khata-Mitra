package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.BusinessProfile
import com.example.data.model.Customer
import com.example.data.model.LedgerTransaction
import com.example.data.model.PartyType
import com.example.data.model.PaymentMode
import com.example.data.model.Supplier
import com.example.data.model.TxnStatus
import com.example.data.model.TxnType
import com.example.ui.components.CameraCaptureMode
import com.example.ui.components.CameraScannerDialog
import com.example.ui.components.DocumentPreviewDialog
import com.example.ui.theme.LocalKhataColors
import com.example.ui.theme.moneyTextStyle
import com.example.util.BrandingImageHelper
import com.example.util.Formatters
import com.example.util.ShareAndIntents

@Composable
fun TransactionEntryDialog(
    initialPartyType: PartyType,
    initialTxnType: TxnType,
    preselectedPartyId: String?,
    preselectedPartyName: String?,
    customers: List<Customer>,
    suppliers: List<Supplier>,
    onDismiss: () -> Unit,
    onSave: (
        partyId: String,
        partyName: String,
        partyType: PartyType,
        txnType: TxnType,
        amount: Double,
        mode: PaymentMode,
        description: String?,
        itemDetails: String?,
        referenceNumber: String?,
        attachmentUri: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val khataColors = LocalKhataColors.current
    var selectedPartyId by remember { mutableStateOf(preselectedPartyId ?: "") }
    var selectedPartyName by remember { mutableStateOf(preselectedPartyName ?: "") }
    var showPartyDropdown by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    var paymentMode by remember {
        mutableStateOf(
            if (initialTxnType == TxnType.CREDIT_GIVEN || initialTxnType == TxnType.PURCHASE_PAYABLE)
                PaymentMode.CREDIT else PaymentMode.CASH
        )
    }
    var description by remember { mutableStateOf("") }
    var itemDetails by remember { mutableStateOf("") }
    var referenceNumber by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<String?>(null) }
    var showScanner by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val saved = BrandingImageHelper.savePickedImageToInternal(
                context = context,
                sourceUri = uri,
                prefix = "bill",
                maxDimPx = 1440,
                folder = "scans"
            )
            attachmentUri = saved ?: uri.toString()
        }
    }

    val isDebitSide = initialTxnType == TxnType.CREDIT_GIVEN || initialTxnType == TxnType.PURCHASE_PAYABLE
    val accent = if (isDebitSide) khataColors.debitRed else khataColors.creditGreen

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (initialTxnType) {
                    TxnType.CREDIT_GIVEN -> "Give Credit (You Gave ₹)"
                    TxnType.PAYMENT_RECEIVED -> "Receive Payment (You Got ₹)"
                    TxnType.PURCHASE_PAYABLE -> "Record Supplier Purchase"
                    TxnType.SUPPLIER_PAYMENT -> "Pay Supplier"
                    else -> initialTxnType.label
                },
                color = accent,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Party selector if not preselected
                if (preselectedPartyId.isNullOrBlank()) {
                    Box {
                        OutlinedTextField(
                            value = selectedPartyName.ifBlank {
                                if (initialPartyType == PartyType.CUSTOMER) "Select Customer *" else "Select Supplier *"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (initialPartyType == PartyType.CUSTOMER) "Customer" else "Supplier") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPartyDropdown = true }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showPartyDropdown = true }
                        )
                        DropdownMenu(
                            expanded = showPartyDropdown,
                            onDismissRequest = { showPartyDropdown = false }
                        ) {
                            if (initialPartyType == PartyType.CUSTOMER) {
                                customers.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text("${c.name} (${Formatters.formatRupee(c.currentBalance)})") },
                                        onClick = {
                                            selectedPartyId = c.id
                                            selectedPartyName = c.name
                                            showPartyDropdown = false
                                            errorMsg = null
                                        }
                                    )
                                }
                            } else {
                                suppliers.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("${s.name} (${Formatters.formatRupee(s.currentBalance)})") },
                                        onClick = {
                                            selectedPartyId = s.id
                                            selectedPartyName = s.name
                                            showPartyDropdown = false
                                            errorMsg = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Account: $selectedPartyName",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorMsg = null },
                    label = { Text("Amount ₹ *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    textStyle = moneyTextStyle(22),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("txn_amount_input")
                )

                Text("Payment Mode", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMode.entries.forEach { mode ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = { Text(mode.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = itemDetails,
                    onValueChange = { itemDetails = it },
                    label = { Text("Item / Bill Details (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (paymentMode == PaymentMode.UPI || paymentMode == PaymentMode.BANK) {
                    OutlinedTextField(
                        value = referenceNumber,
                        onValueChange = { referenceNumber = it },
                        label = { Text("UPI / Bank Reference No.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Bill / Receipt Document", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showScanner = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("txn_scan_bill_button")
                    ) {
                        Icon(Icons.Filled.DocumentScanner, contentDescription = null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Scan Bill", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("txn_pick_bill_button")
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Gallery", maxLines = 1)
                    }
                }
                if (!attachmentUri.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, khataColors.cardBorder, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AsyncImage(
                            model = attachmentUri,
                            contentDescription = "Attached bill scan",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = "Bill Document Attached ✓",
                            style = MaterialTheme.typography.labelLarge,
                            color = khataColors.creditGreen,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { attachmentUri = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove bill", Modifier.size(18.dp))
                        }
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (selectedPartyId.isBlank()) {
                        errorMsg = "Please select a party first"
                        return@Button
                    }
                    if (amount == null || amount <= 0.0) {
                        errorMsg = "Enter a valid amount greater than ₹0"
                        return@Button
                    }
                    onSave(
                        selectedPartyId,
                        selectedPartyName,
                        initialPartyType,
                        initialTxnType,
                        amount,
                        paymentMode,
                        description,
                        itemDetails,
                        referenceNumber,
                        attachmentUri
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.testTag("save_txn_button")
            ) {
                Text("Save Entry", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showScanner) {
        CameraScannerDialog(
            mode = CameraCaptureMode.DOCUMENT_SCAN,
            targetFolder = "scans",
            filePrefix = "bill",
            onDismiss = { showScanner = false },
            onImageSaved = { saved ->
                attachmentUri = saved
                showScanner = false
            }
        )
    }
}

@Composable
fun TransactionDetailDialog(
    txn: LedgerTransaction,
    business: BusinessProfile?,
    onDismiss: () -> Unit,
    onEdit: (newAmount: Double, mode: PaymentMode, notes: String?) -> Unit,
    onReverse: (reason: String) -> Unit
) {
    val context = LocalContext.current
    val khataColors = LocalKhataColors.current
    var isEditing by remember { mutableStateOf(false) }
    var isReversing by remember { mutableStateOf(false) }
    var showAttachmentPreview by remember { mutableStateOf(false) }
    var editAmountText by remember { mutableStateOf(txn.amount.toString()) }
    var editNotes by remember { mutableStateOf(txn.description ?: "") }
    var editMode by remember { mutableStateOf(PaymentMode.fromRaw(txn.paymentMode)) }
    var reverseReason by remember { mutableStateOf("") }

    val canModify = txn.status != TxnStatus.REVERSED.name && txn.txnType != TxnType.REVERSAL.name

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when {
                    isEditing -> "Edit Transaction (Audited)"
                    isReversing -> "Reverse Transaction"
                    else -> "Transaction Receipt"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editAmountText,
                        onValueChange = { editAmountText = it },
                        label = { Text("Corrected Amount ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMode.entries.forEach { m ->
                            FilterChip(
                                selected = editMode == m,
                                onClick = { editMode = m },
                                label = { Text(m.label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (isReversing) {
                    Text(
                        text = "This keeps the original entry in the audit trail and creates an offsetting reversal entry to restore ${txn.partyName}'s balance.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = reverseReason,
                        onValueChange = { reverseReason = it },
                        label = { Text("Reason for reversal") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(txn.partyName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = Formatters.formatRupee(txn.amount),
                                style = moneyTextStyle(26),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${TxnType.fromRaw(txn.txnType).label} • ${txn.paymentMode} • ${txn.status}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = Formatters.formatDateTime(txn.txnTimeMillis),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!txn.itemDetails.isNullOrBlank()) {
                                Text("Items: ${txn.itemDetails}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (!txn.referenceNumber.isNullOrBlank()) {
                                Text("Ref: ${txn.referenceNumber}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (!txn.description.isNullOrBlank()) {
                                Text("Notes: ${txn.description}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (!txn.attachmentUri.isNullOrBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable { showAttachmentPreview = true }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AsyncImage(
                                        model = txn.attachmentUri,
                                        contentDescription = "Attached Bill Document",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Scanned Bill / Attachment", style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            "Tap to inspect, save copy, or share",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Icon(Icons.Filled.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    if (txn.auditHistory.isNotEmpty()) {
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.History, contentDescription = null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Audit History", style = MaterialTheme.typography.labelLarge)
                        }
                        txn.auditHistory.forEach { entry ->
                            Text(
                                text = "• $entry",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val receipt = "Receipt from ${business?.businessName ?: "KhataMitra"}\n" +
                                    "Party: ${txn.partyName}\n" +
                                    "Type: ${TxnType.fromRaw(txn.txnType).label}\n" +
                                    "Amount: ${Formatters.formatRupee(txn.amount)} (${txn.paymentMode})\n" +
                                    "Date: ${Formatters.formatDateTime(txn.txnTimeMillis)}"
                                ShareAndIntents.shareTextViaWhatsAppOrSystem(context, null, receipt)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share")
                        }
                        if (canModify) {
                            OutlinedButton(
                                onClick = { isEditing = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edit")
                            }
                        }
                    }
                    if (canModify) {
                        TextButton(
                            onClick = { isReversing = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = khataColors.debitRed),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reverse Transaction")
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                isEditing -> Button(onClick = {
                    val amt = editAmountText.toDoubleOrNull()
                    if (amt != null && amt > 0.0) {
                        onEdit(amt, editMode, editNotes)
                    }
                }) { Text("Save Edit") }
                isReversing -> Button(
                    onClick = { onReverse(reverseReason) },
                    colors = ButtonDefaults.buttonColors(containerColor = khataColors.debitRed)
                ) { Text("Confirm Reversal", color = Color.White) }
                else -> TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            if (isEditing || isReversing) {
                TextButton(onClick = { isEditing = false; isReversing = false }) { Text("Back") }
            }
        }
    )

    if (showAttachmentPreview && !txn.attachmentUri.isNullOrBlank()) {
        DocumentPreviewDialog(
            title = "${txn.partyName} — Scanned Bill",
            imageUri = txn.attachmentUri,
            suggestedFileName = "Bill_${txn.partyName.replace(" ", "_")}_${txn.id.take(6)}.png",
            onDismiss = { showAttachmentPreview = false }
        )
    }
}
