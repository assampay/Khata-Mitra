package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.LedgerTransaction
import com.example.data.model.SaleRecord
import com.example.data.model.Supplier
import com.example.data.model.TxnStatus
import com.example.data.model.TxnType
import com.example.data.model.UserProfile
import com.example.ui.theme.LocalKhataColors
import com.example.ui.theme.moneyTextStyle
import com.example.ui.viewmodel.ReportTimeframe
import com.example.util.Formatters
import com.example.util.LocalAppStrings
import com.example.util.PdfAndCsvExporter
import com.example.util.ShareAndIntents

enum class MoreRoute {
    REPORTS, REMINDERS, UPI_QR, NOTIFICATIONS, BUSINESS_PROFILE, SUBSCRIPTION, BACKUP_SYNC, SETTINGS, SUPPORT, ADMIN
}

@Composable
fun MoreHubScreen(
    userProfile: UserProfile?,
    activeBusiness: BusinessProfile?,
    isAdmin: Boolean,
    overdueCount: Int,
    onNavigate: (MoreRoute) -> Unit
) {
    val strings = LocalAppStrings.current
    val khataColors = LocalKhataColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!activeBusiness?.logoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = activeBusiness.logoUri,
                            contentDescription = activeBusiness.businessName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (userProfile?.displayName ?: "KM").take(2).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeBusiness?.businessName ?: userProfile?.displayName ?: "KhataMitra",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${userProfile?.email ?: "Signed in"} • ${userProfile?.planTier ?: "FREE"} Plan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            MoreMenuRow(strings.reports, "Cashflow charts, PDF & CSV exports", Icons.Filled.BarChart, khataColors.creditGreen) {
                onNavigate(MoreRoute.REPORTS)
            }
        }
        item {
            MoreMenuRow(strings.reminders, if (overdueCount > 0) "$overdueCount overdue payments" else "WhatsApp, SMS & scheduled dues", Icons.Filled.NotificationsActive, khataColors.debitRed) {
                onNavigate(MoreRoute.REMINDERS)
            }
        }
        item {
            MoreMenuRow(strings.upiQr, activeBusiness?.upiId ?: "Set merchant UPI ID & QR", Icons.Filled.QrCode2, MaterialTheme.colorScheme.primary) {
                onNavigate(MoreRoute.UPI_QR)
            }
        }
        item {
            MoreMenuRow(strings.businessProfile, "Logo, Signature, GSTIN & Multi-Business", Icons.Filled.Store, MaterialTheme.colorScheme.secondary) {
                onNavigate(MoreRoute.BUSINESS_PROFILE)
            }
        }
        item {
            MoreMenuRow(strings.subscription, "Manage Free / Premium features", Icons.Filled.WorkspacePremium, khataColors.saffronGold) {
                onNavigate(MoreRoute.SUBSCRIPTION)
            }
        }
        item {
            MoreMenuRow(strings.backupSync, "Offline cache, Cloud sync & CSV Backup", Icons.Filled.CloudUpload, khataColors.creditGreen) {
                onNavigate(MoreRoute.BACKUP_SYNC)
            }
        }
        item {
            MoreMenuRow(strings.settings, "PIN Lock, Biometrics, Language & Theme", Icons.Filled.Settings, MaterialTheme.colorScheme.onSurface) {
                onNavigate(MoreRoute.SETTINGS)
            }
        }
        item {
            MoreMenuRow(strings.support, "FAQs, Support Tickets & Privacy Policy", Icons.AutoMirrored.Filled.HelpCenter, MaterialTheme.colorScheme.primary) {
                onNavigate(MoreRoute.SUPPORT)
            }
        }
        if (isAdmin) {
            item {
                MoreMenuRow(strings.adminPanel, "Users, Tickets & Announcements", Icons.Filled.AdminPanelSettings, khataColors.debitRed) {
                    onNavigate(MoreRoute.ADMIN)
                }
            }
        }
    }
}

@Composable
private fun MoreMenuRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    val khataColors = LocalKhataColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, khataColors.cardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = tint)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsAnalyticsScreen(
    business: BusinessProfile?,
    customers: List<Customer>,
    suppliers: List<Supplier>,
    transactions: List<LedgerTransaction>,
    expenses: List<Expense>,
    sales: List<SaleRecord>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val khataColors = LocalKhataColors.current
    var timeframe by remember { mutableStateOf(ReportTimeframe.MONTHLY) }

    val cutoff = remember(timeframe) {
        if (timeframe == ReportTimeframe.DAILY) Formatters.startOfTodayMillis()
        else Formatters.daysAgoMillis(timeframe.days)
    }

    val activeTxns = remember(transactions, cutoff) {
        transactions.filter { it.txnTimeMillis >= cutoff && it.status != TxnStatus.REVERSED.name }
    }
    val periodExpenses = remember(expenses, cutoff) {
        expenses.filter { it.expenseTimeMillis >= cutoff }
    }
    val periodSales = remember(sales, cutoff) {
        sales.filter { it.saleTimeMillis >= cutoff }
    }

    val totalCreditGiven = activeTxns.filter { it.txnType == TxnType.CREDIT_GIVEN.name }.sumOf { it.amount }
    val totalCollected = activeTxns.filter { it.txnType == TxnType.PAYMENT_RECEIVED.name }.sumOf { it.amount }
    val totalSalesAmt = periodSales.sumOf { it.totalAmount }
    val totalExpAmt = periodExpenses.sumOf { it.amount }
    val totalReceivable = customers.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
    val totalPayable = suppliers.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }

    val modeTotals = remember(activeTxns) {
        activeTxns.groupBy { it.paymentMode }.mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports & Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reports_back_button")) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportTimeframe.entries.forEach { tf ->
                        FilterChip(
                            selected = timeframe == tf,
                            onClick = { timeframe = tf },
                            label = { Text(tf.label) }
                        )
                    }
                }
            }

            // Cash Flow Bar Chart
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Business Cashflow (${timeframe.label})", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(14.dp))
                        val bars = listOf(
                            Triple("Credit", totalCreditGiven, khataColors.debitRed),
                            Triple("Collected", totalCollected, khataColors.creditGreen),
                            Triple("Sales", totalSalesAmt, MaterialTheme.colorScheme.primary),
                            Triple("Expenses", totalExpAmt, khataColors.saffronGold)
                        )
                        val maxVal = (bars.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        ) {
                            val barWidth = size.width / (bars.size * 2f)
                            bars.forEachIndexed { idx, (_, value, color) ->
                                val h = ((value / maxVal) * (size.height - 16f)).toFloat().coerceAtLeast(8f)
                                val x = barWidth * (idx * 2f + 0.5f)
                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(x, size.height - h),
                                    size = Size(barWidth, h),
                                    cornerRadius = CornerRadius(12f, 12f)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            bars.forEach { (label, value, color) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = color)
                                    Text(Formatters.formatCompactRupee(value), style = moneyTextStyle(13))
                                }
                            }
                        }
                    }
                }
            }

            // Payment Mode Breakdown
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Payment Mode Report", style = MaterialTheme.typography.titleMedium)
                        if (modeTotals.isEmpty()) {
                            Text("No transactions in this period", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            modeTotals.forEach { (mode, amt) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(mode, style = MaterialTheme.typography.bodyLarge)
                                    Text(Formatters.formatRupee(amt), style = moneyTextStyle(15))
                                }
                            }
                        }
                    }
                }
            }

            // Outstanding Summary & Export Actions
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Export Reports", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val uri = PdfAndCsvExporter.generateBusinessReportPdf(
                                        context, business, timeframe.label,
                                        totalReceivable, totalPayable, totalSalesAmt, totalExpAmt, totalCollected, totalCreditGiven
                                    )
                                    ShareAndIntents.shareFileUri(context, uri, "application/pdf", "Share Business Report PDF")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Export PDF")
                            }
                            OutlinedButton(
                                onClick = {
                                    val uri = PdfAndCsvExporter.exportFullLedgerCsv(
                                        context, customers, suppliers, transactions, expenses, sales
                                    )
                                    ShareAndIntents.shareFileUri(context, uri, "text/csv", "Export CSV Ledger")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.TableChart, contentDescription = null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Export CSV")
                            }
                        }
                    }
                }
            }
        }
    }
}
