package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.LocalSecurityPrefs
import com.example.data.local.ThemePreference
import com.example.data.model.AppLanguage
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.LedgerTransaction
import com.example.data.model.PlanTier
import com.example.data.model.SaleRecord
import com.example.data.model.Supplier
import com.example.data.model.SupportTicket
import com.example.data.model.TicketStatus
import com.example.data.model.UserProfile
import com.example.ui.theme.LocalKhataColors
import com.example.util.Formatters
import com.example.util.PdfAndCsvExporter
import com.example.util.ShareAndIntents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    userProfile: UserProfile?,
    customerCount: Int,
    onSelectPlan: (PlanTier) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val khataColors = LocalKhataColors.current
    val currentTier = PlanTier.fromRaw(userProfile?.planTier)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscription & Plans") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = khataColors.saffronGold)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Active Plan: ${currentTier.name}", style = MaterialTheme.typography.titleLarge)
                        Text("Customers used: $customerCount", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            PlanComparisonCard(
                title = "Free Plan",
                price = "₹0 / month",
                features = listOf("Core Customer & Supplier Ledger", "Up to 50 Customers", "Basic Daily & Monthly Reports", "WhatsApp & SMS Reminders"),
                selected = currentTier == PlanTier.FREE,
                onSelect = { onSelectPlan(PlanTier.FREE) }
            )
            PlanComparisonCard(
                title = "Premium Business Plan",
                price = "₹199 / month",
                features = listOf("Unlimited Customers & Ledger Entries", "Multi-Business & Multi-Device Sync", "PDF & CSV Financial Exports", "Priority Support & Custom Reminders"),
                selected = currentTier == PlanTier.PREMIUM,
                onSelect = { onSelectPlan(PlanTier.PREMIUM) }
            )
        }
    }
}

@Composable
private fun PlanComparisonCard(
    title: String,
    price: String,
    features: List<String>,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(price, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            features.forEach { f ->
                Text("✓ $f", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onSelect,
                enabled = !selected,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selected) "Current Plan" else "Switch to $title")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSyncScreen(
    lastSyncMillis: Long,
    customers: List<Customer>,
    suppliers: List<Supplier>,
    transactions: List<LedgerTransaction>,
    expenses: List<Expense>,
    sales: List<SaleRecord>,
    onManualSync: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val khataColors = LocalKhataColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Cloud Sync") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = khataColors.creditGreenContainer),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudDone, contentDescription = null, tint = khataColors.creditGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("Offline-First Firestore Persistence Active", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = if (lastSyncMillis > 0) "Last synced: ${Formatters.formatDateTime(lastSyncMillis)}" else "Syncing with Cloud...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${customers.size} Customers • ${suppliers.size} Suppliers • ${transactions.size} Transactions",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Button(onClick = onManualSync, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Sync Now")
            }

            OutlinedButton(
                onClick = {
                    val uri = PdfAndCsvExporter.exportFullLedgerCsv(
                        context, customers, suppliers, transactions, expenses, sales
                    )
                    ShareAndIntents.shareFileUri(context, uri, "text/csv", "Save Full Khata Backup CSV")
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export Complete CSV Backup")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: LocalSecurityPrefs,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetTheme: (ThemePreference) -> Unit,
    onSetupPin: () -> Unit,
    onDisablePin: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onToggleNotifications: (Boolean, Boolean) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onToggleNotifications(granted, prefs.dailySummaryNotificationsEnabled)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Security") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("App Language", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppLanguage.entries.forEach { lang ->
                    FilterChip(
                        selected = prefs.language == lang,
                        onClick = { onSetLanguage(lang) },
                        label = { Text(lang.label) },
                        modifier = Modifier.testTag("settings_lang_${lang.code}")
                    )
                }
            }

            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePreference.entries.forEach { mode ->
                    FilterChip(
                        selected = prefs.themePreference == mode,
                        onClick = { onSetTheme(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            HorizontalDivider()
            Text("Security & App Lock", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("4-Digit App PIN Lock", style = MaterialTheme.typography.bodyLarge)
                    Text("Require PIN when opening KhataMitra", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = prefs.pinEnabled,
                    onCheckedChange = { enabled -> if (enabled) onSetupPin() else onDisablePin() }
                )
            }
            if (prefs.pinEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Biometric Unlock (Fingerprint/Face)", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = prefs.biometricEnabled, onCheckedChange = onToggleBiometric)
                }
            }
            if (prefs.pinEnabled) {
                OutlinedButton(onClick = onSetupPin, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Lock, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Change 4-Digit PIN")
                }
            }

            HorizontalDivider()
            Text("Notifications", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Payment & Due Reminders", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = prefs.reminderNotificationsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onToggleNotifications(enabled, prefs.dailySummaryNotificationsEnabled)
                        }
                    }
                )
            }

            HorizontalDivider()
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }

            TextButton(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Delete Account Profile")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account Profile?") },
            text = { Text("This deletes your KhataMitra user profile and signs you out. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDeleteAccount() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete & Sign Out") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    tickets: List<SupportTicket>,
    onSubmitTicket: (subject: String, description: String, category: String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Ledger") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Support") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
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
                Text("Frequently Asked Questions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                FaqItem("Does KhataMitra work offline?", "Yes! All customers, suppliers, and entries are cached on your device and auto-synced when internet returns.")
                Spacer(Modifier.height(8.dp))
                FaqItem("Can I reverse a wrong transaction?", "Yes, tap any entry to view its receipt, edit it with an audit trail, or reverse it safely without deleting history.")
            }
            item {
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Raise a Support Ticket", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject *") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Describe your issue *") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (subject.isNotBlank() && description.isNotBlank()) {
                            onSubmitTicket(subject, description, category)
                            subject = ""
                            description = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Submit Ticket") }
            }
            if (tickets.isNotEmpty()) {
                item { Text("Your Tickets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(tickets, key = { it.id }) { t ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${t.subject} [${t.status}]", style = MaterialTheme.typography.titleMedium)
                            Text(t.description, style = MaterialTheme.typography.bodyMedium)
                            if (!t.adminReply.isNullOrBlank()) {
                                Text("Support Reply: ${t.adminReply}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    text = "Privacy Policy & Terms: User ledger records are strictly isolated per authenticated Google account and never shared with unauthorized parties.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FaqItem(q: String, a: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(q, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(a, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    users: List<UserProfile>,
    tickets: List<SupportTicket>,
    onUpdateTicket: (String, TicketStatus, String) -> Unit,
    onPublishAnnouncement: (String, String) -> Unit,
    onUpdateUser: (String, String, PlanTier) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var annTitle by remember { mutableStateOf("") }
    var annMsg by remember { mutableStateOf("") }
    val premiumUsers = users.count { it.planTier == PlanTier.PREMIUM.name }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Super Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Total Users", style = MaterialTheme.typography.labelMedium)
                            Text(users.size.toString(), style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    Card(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Premium Users", style = MaterialTheme.typography.labelMedium)
                            Text(premiumUsers.toString(), style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    Card(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Open Tickets", style = MaterialTheme.typography.labelMedium)
                            Text(tickets.count { it.status != TicketStatus.RESOLVED.name }.toString(), style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Publish Announcement", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(value = annTitle, onValueChange = { annTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = annMsg, onValueChange = { annMsg = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            if (annTitle.isNotBlank() && annMsg.isNotBlank()) {
                                onPublishAnnouncement(annTitle, annMsg)
                                annTitle = ""
                                annMsg = ""
                            }
                        }) { Text("Broadcast") }
                    }
                }
            }

            item { Text("Registered Users (Private Ledgers Hidden)", style = MaterialTheme.typography.titleMedium) }
            items(users, key = { it.id }) { u ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(u.displayName, style = MaterialTheme.typography.titleMedium)
                            Text("${u.email ?: u.userId} • ${u.planTier} • ${u.accountStatus}", style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(onClick = {
                            val nextPlan = if (u.planTier == PlanTier.FREE.name) PlanTier.PREMIUM else PlanTier.FREE
                            onUpdateUser(u.userId, u.accountStatus, nextPlan)
                        }) {
                            Text(if (u.planTier == PlanTier.FREE.name) "Upgrade" else "Set Free")
                        }
                    }
                }
            }

            item { Text("Support Tickets Queue", style = MaterialTheme.typography.titleMedium) }
            items(tickets, key = { it.id }) { t ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${t.subject} [${t.status}]", style = MaterialTheme.typography.titleMedium)
                        Text(t.description, style = MaterialTheme.typography.bodyMedium)
                        if (t.status != TicketStatus.RESOLVED.name) {
                            OutlinedButton(onClick = {
                                onUpdateTicket(t.id, TicketStatus.RESOLVED, "Resolved by support team.")
                            }) { Text("Mark Resolved") }
                        }
                    }
                }
            }
        }
    }
}
