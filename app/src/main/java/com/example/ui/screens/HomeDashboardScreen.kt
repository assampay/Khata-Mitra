package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Announcement
import com.example.data.model.BusinessProfile
import com.example.data.model.LedgerTransaction
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.LocalKhataColors
import com.example.ui.theme.moneyTextStyle
import com.example.ui.viewmodel.DashboardSummary
import com.example.util.Formatters
import com.example.util.LocalAppStrings

@Composable
fun HomeDashboardScreen(
    activeBusiness: BusinessProfile?,
    businesses: List<BusinessProfile>,
    summary: DashboardSummary,
    recentTransactions: List<LedgerTransaction>,
    announcements: List<Announcement>,
    unreadNotificationCount: Int,
    isSyncing: Boolean,
    lastSyncMillis: Long,
    onSelectBusiness: (String) -> Unit,
    onManualSync: () -> Unit,
    onOpenNotifications: () -> Unit,
    onAddCustomer: () -> Unit,
    onAddSupplier: () -> Unit,
    onGiveCredit: () -> Unit,
    onReceivePayment: () -> Unit,
    onAddExpense: () -> Unit,
    onAddSale: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenUpiQr: () -> Unit,
    onTransactionClick: (LedgerTransaction) -> Unit,
    onViewAllTransactions: () -> Unit
) {
    val strings = LocalAppStrings.current
    val khataColors = LocalKhataColors.current
    var showBizMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Business & Sync Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, khataColors.cardBorder, RoundedCornerShape(14.dp))
                            .clickable { showBizMenu = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("business_switcher")
                    ) {
                        if (!activeBusiness?.logoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = activeBusiness.logoUri,
                                contentDescription = activeBusiness.businessName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Store,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = activeBusiness?.businessName ?: "KhataMitra",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeBusiness?.businessType ?: "Tap to switch business",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Switch business")
                    }
                    DropdownMenu(
                        expanded = showBizMenu,
                        onDismissRequest = { showBizMenu = false }
                    ) {
                        businesses.forEach { biz ->
                            DropdownMenuItem(
                                text = { Text(biz.businessName) },
                                onClick = {
                                    onSelectBusiness(biz.id)
                                    showBizMenu = false
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = khataColors.creditGreenContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onManualSync)
                            .testTag("sync_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isSyncing) Icons.Filled.CloudSync else Icons.Filled.CloudDone,
                                contentDescription = "Sync status",
                                tint = khataColors.creditGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSyncing) "Syncing" else strings.cloudSynced,
                                style = MaterialTheme.typography.labelSmall,
                                color = khataColors.creditGreen
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    Badge { Text(unreadNotificationCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                        }
                    }
                }
            }
        }

        // Announcements Banner
        val activeAnnouncement = announcements.firstOrNull { it.activeFlag }
        if (activeAnnouncement != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = khataColors.saffronGoldContainer),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = khataColors.saffronGold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = activeAnnouncement.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = activeAnnouncement.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Hero Net Balance Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(khataColors.heroStart, khataColors.heroEnd))
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "KHATA SUMMARY",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "${summary.customerCount} Customers • ${summary.supplierCount} Suppliers",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.youWillGet,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFA7F3D0)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Formatters.formatRupee(summary.totalToReceive),
                                style = moneyTextStyle(22),
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(46.dp)
                                .background(Color.White.copy(alpha = 0.25f))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.youWillGive,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFFECACA)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Formatters.formatRupee(summary.totalToPay),
                                style = moneyTextStyle(22),
                                color = Color.White
                            )
                        }
                    }
                    if (lastSyncMillis > 0L) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.16f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Net Position: ${Formatters.formatRupee(summary.totalToReceive - summary.totalToPay, showSign = true)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            if (summary.overdueCount > 0) {
                                Text(
                                    text = "${summary.overdueCount} Overdue",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFDE68A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Today's Business Pulse Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TodayMetricTile(
                    label = strings.todayReceived,
                    amount = summary.todayReceived,
                    accent = khataColors.creditGreen,
                    modifier = Modifier.weight(1f)
                )
                TodayMetricTile(
                    label = strings.todayGiven,
                    amount = summary.todayGiven,
                    accent = khataColors.debitRed,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TodayMetricTile(
                    label = strings.todaySales,
                    amount = summary.todaySales,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TodayMetricTile(
                    label = strings.todayExpenses,
                    amount = summary.todayExpenses,
                    accent = khataColors.saffronGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Actions Grid
        item {
            SectionHeader(title = strings.quickActions)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        label = strings.addCustomer,
                        icon = Icons.Filled.PersonAdd,
                        tint = MaterialTheme.colorScheme.primary,
                        tag = "qa_add_customer",
                        onClick = onAddCustomer,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        label = strings.giveCreditBtn.replace(" ₹", ""),
                        icon = Icons.Filled.ArrowUpward,
                        tint = khataColors.debitRed,
                        tag = "qa_give_credit",
                        onClick = onGiveCredit,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        label = strings.receivePaymentBtn.replace(" ₹", ""),
                        icon = Icons.Filled.ArrowDownward,
                        tint = khataColors.creditGreen,
                        tag = "qa_receive_payment",
                        onClick = onReceivePayment,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        label = strings.addSupplier,
                        icon = Icons.Filled.LocalShipping,
                        tint = MaterialTheme.colorScheme.secondary,
                        tag = "qa_add_supplier",
                        onClick = onAddSupplier,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        label = strings.addExpense,
                        icon = Icons.Filled.AccountBalanceWallet,
                        tint = khataColors.saffronGold,
                        tag = "qa_add_expense",
                        onClick = onAddExpense,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        label = strings.addSale,
                        icon = Icons.Filled.AddShoppingCart,
                        tint = MaterialTheme.colorScheme.primary,
                        tag = "qa_add_sale",
                        onClick = onAddSale,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        label = "Reports",
                        icon = Icons.Filled.BarChart,
                        tint = khataColors.creditGreen,
                        tag = "qa_reports",
                        onClick = onOpenReports,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        label = "UPI QR",
                        icon = Icons.Filled.QrCode2,
                        tint = MaterialTheme.colorScheme.primary,
                        tag = "qa_upi_qr",
                        onClick = onOpenUpiQr,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Recent Transactions
        item {
            SectionHeader(
                title = strings.recentTransactions,
                actionLabel = if (recentTransactions.isNotEmpty()) "View Khata" else null,
                onAction = onViewAllTransactions
            )
        }

        if (recentTransactions.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Start Your Digital Khata",
                    subtitle = "Add your first customer or supplier to track Udhar, payments, and daily cashflow.",
                    actionLabel = "+ Add First Customer",
                    onAction = onAddCustomer
                )
            }
        } else {
            items(recentTransactions.take(10), key = { it.id }) { txn ->
                TransactionRowCard(txn = txn, onClick = { onTransactionClick(txn) })
            }
        }
    }
}

@Composable
private fun TodayMetricTile(
    label: String,
    amount: Double,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val khataColors = LocalKhataColors.current
    Card(
        modifier = modifier.border(1.dp, khataColors.cardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = Formatters.formatRupee(amount),
                style = moneyTextStyle(18),
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val khataColors = LocalKhataColors.current
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, khataColors.cardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
