package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AppNotification
import com.example.data.model.BusinessProfile
import com.example.data.model.Customer
import com.example.data.model.ReminderChannel
import com.example.data.model.ReminderRecord
import com.example.data.model.ReminderStatus
import com.example.ui.components.CameraCaptureMode
import com.example.ui.components.CameraScannerDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.QrCodeCanvas
import com.example.ui.theme.LocalKhataColors
import com.example.ui.theme.moneyTextStyle
import com.example.util.BrandingImageHelper
import com.example.util.Formatters
import com.example.util.PdfAndCsvExporter
import com.example.util.ShareAndIntents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    business: BusinessProfile?,
    customers: List<Customer>,
    reminders: List<ReminderRecord>,
    onBack: () -> Unit,
    onLogReminder: (Customer, ReminderChannel, String, ReminderStatus) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val khataColors = LocalKhataColors.current
    val pendingCustomers = remember(customers) {
        customers.filter { it.currentBalance > 0 }.sortedByDescending { it.currentBalance }
    }
    var customTemplate by remember {
        mutableStateOf("Hello {name}, your outstanding balance is {amount} with {shop}. Kindly pay when convenient.")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Reminders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = customTemplate,
                    onValueChange = { customTemplate = it },
                    label = { Text("Polite Reminder Template ({name}, {amount}, {shop})") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (pendingCustomers.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "All Payments Settled!",
                        subtitle = "No pending customer dues at the moment."
                    )
                }
            } else {
                items(pendingCustomers, key = { it.id }) { cust ->
                    val shopName = business?.businessName ?: "Our Store"
                    val msg = customTemplate
                        .replace("{name}", cust.name)
                        .replace("{amount}", Formatters.formatRupee(cust.currentBalance))
                        .replace("{shop}", shopName)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, khataColors.cardBorder, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(cust.name, style = MaterialTheme.typography.titleMedium)
                                    Text(cust.phone ?: "No phone", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = Formatters.formatRupee(cust.currentBalance),
                                    style = moneyTextStyle(18),
                                    color = khataColors.creditGreen
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onLogReminder(cust, ReminderChannel.WHATSAPP, msg, ReminderStatus.SENT)
                                        ShareAndIntents.shareTextViaWhatsAppOrSystem(context, cust.phone, msg)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("WhatsApp")
                                }
                                OutlinedButton(
                                    onClick = {
                                        onLogReminder(cust, ReminderChannel.SMS, msg, ReminderStatus.SENT)
                                        ShareAndIntents.sendSmsReminder(context, cust.phone, msg)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Sms, contentDescription = null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("SMS")
                                }
                            }
                        }
                    }
                }
            }

            if (reminders.isNotEmpty()) {
                item {
                    Text("Reminder History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(reminders.take(15), key = { it.id }) { rem ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${rem.partyName} • ${rem.channel} • ${Formatters.formatRupee(rem.amount)}", style = MaterialTheme.typography.labelLarge)
                            Text(Formatters.formatDateTime(rem.scheduledAtMillis), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiQrScreen(
    business: BusinessProfile?,
    initialAmount: Double? = null,
    onSaveUpiId: (BusinessProfile) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var upiId by remember(business?.upiId) { mutableStateOf(business?.upiId ?: "merchant@upi") }
    var amountText by remember(initialAmount) { mutableStateOf(initialAmount?.takeIf { it > 0 }?.toString() ?: "") }
    var note by remember { mutableStateOf("Khata Payment") }

    val amount = amountText.toDoubleOrNull()
    val shopName = business?.businessName ?: "KhataMitra Store"
    val upiUri = remember(upiId, shopName, amount, note) {
        ShareAndIntents.buildUpiPaymentUri(upiId, shopName, amount, note)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("UPI Payment QR") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            QrCodeCanvas(payload = upiUri, size = 220.dp)

            Text(shopName, style = MaterialTheme.typography.titleLarge)
            Text("UPI ID: $upiId", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(
                value = upiId,
                onValueChange = { upiId = it },
                label = { Text("Merchant UPI ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount ₹ (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Payment Note") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val uri = PdfAndCsvExporter.exportQrBitmapUri(context, upiUri, shopName)
                        ShareAndIntents.shareFileUri(context, uri, "image/png", "Share UPI QR")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share QR")
                }
                OutlinedButton(
                    onClick = { ShareAndIntents.launchUpiPayIntent(context, upiUri) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open UPI")
                }
            }

            if (business != null && upiId.isNotBlank() && upiId != business.upiId) {
                OutlinedButton(
                    onClick = { onSaveUpiId(business.copy(upiId = upiId.trim())) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save UPI ID to Business Profile")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onMarkRead: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (notifications.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "All Caught Up",
                        subtitle = "Payment confirmations, reminder logs, and backup alerts appear here."
                    )
                }
            } else {
                items(notifications, key = { it.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.readFlag) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (!item.readFlag) onMarkRead(item.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Text(item.body, style = MaterialTheme.typography.bodyMedium)
                                Text(Formatters.formatDateTime(item.createdAtMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!item.readFlag) {
                                Icon(Icons.Filled.Check, contentDescription = "Unread", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    activeBusiness: BusinessProfile?,
    businesses: List<BusinessProfile>,
    onSelectBusiness: (String) -> Unit,
    onSaveBusiness: (BusinessProfile) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val khataColors = LocalKhataColors.current
    var isCreatingNew by remember { mutableStateOf(false) }
    var name by remember(activeBusiness, isCreatingNew) {
        mutableStateOf(if (isCreatingNew) "" else activeBusiness?.businessName ?: "")
    }
    var owner by remember(activeBusiness, isCreatingNew) {
        mutableStateOf(if (isCreatingNew) "" else activeBusiness?.ownerName ?: "")
    }
    var phone by remember(activeBusiness, isCreatingNew) {
        mutableStateOf(if (isCreatingNew) "" else activeBusiness?.phone ?: "")
    }
    var bizType by remember(activeBusiness, isCreatingNew) {
        mutableStateOf(if (isCreatingNew) "Retail & Kirana" else activeBusiness?.businessType ?: "Kirana & General Store")
    }
    var address by remember(activeBusiness, isCreatingNew) {
        mutableStateOf(if (isCreatingNew) "" else activeBusiness?.address ?: "")
    }
    var gst by remember(activeBusiness, isCreatingNew) {
        mutableStateOf(if (isCreatingNew) "" else activeBusiness?.gstNumber ?: "")
    }
    var upi by remember(activeBusiness, isCreatingNew) {
        mutableStateOf(if (isCreatingNew) "" else activeBusiness?.upiId ?: "")
    }
    var logoUri by remember(activeBusiness, isCreatingNew) {
        mutableStateOf(if (isCreatingNew) null else activeBusiness?.logoUri?.takeIf { it.isNotBlank() })
    }
    var signatureUri by remember(activeBusiness, isCreatingNew) {
        mutableStateOf(if (isCreatingNew) null else activeBusiness?.signatureUri?.takeIf { it.isNotBlank() })
    }
    var showSignaturePad by remember { mutableStateOf(false) }

    fun persistImmediatelyIfEditing(newLogo: String?, newSig: String?) {
        if (!isCreatingNew && activeBusiness != null && name.isNotBlank() && owner.isNotBlank()) {
            onSaveBusiness(
                activeBusiness.copy(
                    businessName = name.trim(),
                    ownerName = owner.trim(),
                    phone = phone.trim().ifBlank { null },
                    businessType = bizType.trim().ifBlank { "Retail" },
                    gstNumber = gst.trim().ifBlank { null },
                    upiId = upi.trim().ifBlank { null },
                    address = address.trim().ifBlank { null },
                    logoUri = newLogo,
                    signatureUri = newSig
                )
            )
        }
    }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val saved = BrandingImageHelper.savePickedImageToInternal(context, uri, "logo", 512)
            if (saved != null) {
                logoUri = saved
                persistImmediatelyIfEditing(saved, signatureUri)
            }
        }
    }

    var showLogoCamera by remember { mutableStateOf(false) }
    var showSignatureScan by remember { mutableStateOf(false) }

    val signaturePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val saved = BrandingImageHelper.savePickedImageToInternal(context, uri, "sig", 600)
            if (saved != null) {
                signatureUri = saved
                persistImmediatelyIfEditing(logoUri, saved)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Profile & Branding") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isCreatingNew = !isCreatingNew }) {
                        Icon(Icons.Filled.AddBusiness, contentDescription = "Add New Business")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (businesses.size > 1 && !isCreatingNew) {
                Text("Your Businesses", style = MaterialTheme.typography.titleMedium)
                businesses.forEach { b ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectBusiness(b.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (b.id == activeBusiness?.id) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(b.businessName, style = MaterialTheme.typography.titleMedium)
                                Text(b.businessType, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (b.id == activeBusiness?.id) Icon(Icons.Filled.Check, contentDescription = "Active")
                        }
                    }
                }
            }

            // Business Logo & Authorized Signature Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, khataColors.cardBorder, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Logo & Authorized Signature (For Invoices & PDFs)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Business Logo Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, khataColors.cardBorder, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!logoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = logoUri,
                                    contentDescription = "Business Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Store,
                                    contentDescription = "No logo uploaded",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Business Logo", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "Appears on Home header, invoices, and statements.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        logoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.testTag("upload_logo_button")
                                ) {
                                    Icon(Icons.Filled.Upload, contentDescription = null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (logoUri == null) "Upload" else "Change")
                                }
                                OutlinedButton(
                                    onClick = { showLogoCamera = true },
                                    modifier = Modifier.testTag("camera_logo_button")
                                ) {
                                    Icon(Icons.Filled.PhotoCamera, contentDescription = "Capture Logo", Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Camera")
                                }
                                if (!logoUri.isNullOrBlank()) {
                                    IconButton(
                                        onClick = {
                                            logoUri = null
                                            persistImmediatelyIfEditing(null, signatureUri)
                                        },
                                        modifier = Modifier.testTag("remove_logo_button")
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Remove logo",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Authorized Signature Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Authorized Signature", style = MaterialTheme.typography.labelLarge)
                            if (!signatureUri.isNullOrBlank()) {
                                TextButton(
                                    onClick = {
                                        signatureUri = null
                                        persistImmediatelyIfEditing(logoUri, null)
                                    },
                                    modifier = Modifier.testTag("remove_signature_button")
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Remove", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(88.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, khataColors.cardBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!signatureUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = signatureUri,
                                    contentDescription = "Uploaded Signature",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                )
                            } else {
                                Text(
                                    text = "No signature uploaded or drawn yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    signaturePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("upload_signature_button")
                            ) {
                                Icon(Icons.Filled.Upload, contentDescription = null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Upload", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { showSignatureScan = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scan_signature_button")
                            ) {
                                Icon(Icons.Filled.DocumentScanner, contentDescription = null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Scan", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { showSignaturePad = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("draw_signature_button")
                            ) {
                                Icon(Icons.Filled.Draw, contentDescription = null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Draw", maxLines = 1)
                            }
                        }
                    }
                }
            }

            Text(
                text = if (isCreatingNew) "Create Additional Business" else "Edit Business Details",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Business Name *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("Owner Name *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Business Mobile") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bizType, onValueChange = { bizType = it }, label = { Text("Business Type") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = gst, onValueChange = { gst = it }, label = { Text("GST Number (optional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = upi, onValueChange = { upi = it }, label = { Text("Merchant UPI ID (optional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Shop Address") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    if (name.isNotBlank() && owner.isNotBlank()) {
                        val base = if (isCreatingNew || activeBusiness == null) BusinessProfile() else activeBusiness
                        onSaveBusiness(
                            base.copy(
                                businessName = name.trim(),
                                ownerName = owner.trim(),
                                phone = phone.trim().ifBlank { null },
                                businessType = bizType.trim().ifBlank { "Retail" },
                                gstNumber = gst.trim().ifBlank { null },
                                upiId = upi.trim().ifBlank { null },
                                address = address.trim().ifBlank { null },
                                logoUri = logoUri,
                                signatureUri = signatureUri
                            )
                        )
                        isCreatingNew = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_business_profile_button")
            ) {
                Text(if (isCreatingNew) "Create Business" else "Save Profile")
            }
        }
    }

    if (showSignaturePad) {
        SignaturePadDialog(
            onDismiss = { showSignaturePad = false },
            onSave = { strokes, w, h ->
                val saved = BrandingImageHelper.saveSignatureStrokesToInternal(context, strokes, w, h)
                if (saved != null) {
                    signatureUri = saved
                    persistImmediatelyIfEditing(logoUri, saved)
                }
                showSignaturePad = false
            }
        )
    }

    if (showLogoCamera) {
        CameraScannerDialog(
            mode = CameraCaptureMode.PROFILE_PHOTO,
            targetFolder = "branding",
            filePrefix = "logo",
            maxDimPx = 512,
            onDismiss = { showLogoCamera = false },
            onImageSaved = { saved ->
                logoUri = saved
                persistImmediatelyIfEditing(saved, signatureUri)
                showLogoCamera = false
            }
        )
    }

    if (showSignatureScan) {
        CameraScannerDialog(
            mode = CameraCaptureMode.DOCUMENT_SCAN,
            targetFolder = "branding",
            filePrefix = "sig",
            maxDimPx = 800,
            onDismiss = { showSignatureScan = false },
            onImageSaved = { saved ->
                signatureUri = saved
                persistImmediatelyIfEditing(logoUri, saved)
                showSignatureScan = false
            }
        )
    }
}

@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSave: (strokes: List<List<Pair<Float, Float>>>, width: Float, height: Float) -> Unit
) {
    val completedStrokes = remember { mutableStateListOf<List<Pair<Float, Float>>>() }
    var currentStroke by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Draw Authorized Signature") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Sign inside the box below using your finger.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentStroke = listOf(offset.x to offset.y)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentStroke = currentStroke + (change.position.x to change.position.y)
                                },
                                onDragEnd = {
                                    if (currentStroke.isNotEmpty()) {
                                        completedStrokes.add(currentStroke)
                                        currentStroke = emptyList()
                                    }
                                },
                                onDragCancel = {
                                    currentStroke = emptyList()
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val allStrokes = completedStrokes + if (currentStroke.isNotEmpty()) listOf(currentStroke) else emptyList()
                        for (pts in allStrokes) {
                            if (pts.size < 2) continue
                            val path = Path().apply {
                                moveTo(pts.first().first, pts.first().second)
                                for (i in 1 until pts.size) {
                                    val prev = pts[i - 1]
                                    val curr = pts[i]
                                    quadraticTo(
                                        prev.first,
                                        prev.second,
                                        (prev.first + curr.first) / 2f,
                                        (prev.second + curr.second) / 2f
                                    )
                                }
                                lineTo(pts.last().first, pts.last().second)
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFF0D4C5E),
                                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = completedStrokes.isNotEmpty() && canvasSize.width > 0 && canvasSize.height > 0,
                onClick = {
                    onSave(completedStrokes.toList(), canvasSize.width.toFloat(), canvasSize.height.toFloat())
                },
                modifier = Modifier.testTag("confirm_drawn_signature_button")
            ) {
                Text("Use Signature")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    completedStrokes.clear()
                    currentStroke = emptyList()
                }) {
                    Text("Clear")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
