package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.BusinessProfile
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.PaymentMode
import com.example.data.model.SaleRecord
import com.example.ui.components.CameraCaptureMode
import com.example.ui.components.CameraScannerDialog
import com.example.ui.components.DocumentPreviewDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.LocalKhataColors
import com.example.ui.theme.moneyTextStyle
import com.example.util.BrandingImageHelper
import com.example.util.Formatters
import com.example.util.PdfAndCsvExporter
import com.example.util.ShareAndIntents

val EXPENSE_CATEGORIES = listOf(
    "Rent", "Electricity", "Employee Salary", "Purchase of Stock",
    "Transport", "Food", "Internet", "Maintenance", "Marketing", "Other"
)

@Composable
fun BusinessBookScreen(
    business: BusinessProfile?,
    sales: List<SaleRecord>,
    expenses: List<Expense>,
    customers: List<Customer>,
    initialTab: Int = 0,
    onOpenAddSale: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    val context = LocalContext.current
    val khataColors = LocalKhataColors.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var previewExpenseReceipt by remember { mutableStateOf<Expense?>(null) }

    val totalSales = remember(sales) { sales.sumOf { it.totalAmount } }
    val totalExpenses = remember(expenses) { expenses.sumOf { it.amount } }
    val categoryTotals = remember(expenses) {
        expenses.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (selectedTab == 0) onOpenAddSale() else onOpenAddExpense() },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("book_fab_add")
            ) {
                Icon(Icons.Filled.Add, contentDescription = if (selectedTab == 0) "Add Sale" else "Add Expense")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.testTag("tab_sales")
                ) {
                    Text("Sales (${sales.size})")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("tab_expenses")
                ) {
                    Text("Expenses (${expenses.size})")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedTab == 0) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Total Sales", style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        text = Formatters.formatRupee(totalSales),
                                        style = moneyTextStyle(24),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "${sales.size} Invoices",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    if (sales.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "No Sales Recorded Yet",
                                subtitle = "Create retail invoices with GST/discount and automatically post unpaid credit to customer khata.",
                                actionLabel = "+ Add Sale",
                                onAction = onOpenAddSale
                            )
                        }
                    } else {
                        items(sales, key = { it.id }) { sale ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, khataColors.cardBorder, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Receipt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${sale.invoiceNumber} • ${sale.customerName}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "${sale.productName} (${sale.quantity} x ${Formatters.formatRupee(sale.unitPrice)})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Paid: ${Formatters.formatRupee(sale.paidAmount)} • Due: ${Formatters.formatRupee(sale.remainingBalance)} • ${Formatters.formatShortDay(sale.saleTimeMillis)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (sale.remainingBalance > 0) khataColors.debitRed else khataColors.creditGreen
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = Formatters.formatRupee(sale.totalAmount),
                                            style = moneyTextStyle(16),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(onClick = {
                                            val uri = PdfAndCsvExporter.generateSaleInvoicePdf(context, business, sale)
                                            ShareAndIntents.shareFileUri(context, uri, "application/pdf", "Share Invoice ${sale.invoiceNumber}")
                                        }) {
                                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "Share Invoice PDF", tint = khataColors.debitRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = khataColors.saffronGoldContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Total Business Expenses", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = Formatters.formatRupee(totalExpenses),
                                    style = moneyTextStyle(24),
                                    color = khataColors.saffronGold
                                )
                                if (categoryTotals.isNotEmpty() && totalExpenses > 0) {
                                    Spacer(Modifier.height(10.dp))
                                    categoryTotals.take(4).forEach { (cat, sum) ->
                                        val ratio = (sum / totalExpenses).toFloat().coerceIn(0f, 1f)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(cat, style = MaterialTheme.typography.labelSmall)
                                            Text(Formatters.formatRupee(sum), style = MaterialTheme.typography.labelSmall)
                                        }
                                        LinearProgressIndicator(
                                            progress = { ratio },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (expenses.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "No Expenses Recorded",
                                subtitle = "Track shop rent, electricity, staff salaries, stock purchases, and daily costs.",
                                actionLabel = "+ Add Expense",
                                onAction = onOpenAddExpense
                            )
                        }
                    } else {
                        items(expenses, key = { it.id }) { exp ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, khataColors.cardBorder, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(exp.category, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${exp.paymentMode} • ${Formatters.formatDateTime(exp.expenseTimeMillis)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!exp.notes.isNullOrBlank()) {
                                            Text(exp.notes, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                    Text(
                                        text = Formatters.formatRupee(exp.amount),
                                        style = moneyTextStyle(16),
                                        color = khataColors.debitRed
                                    )
                                    if (!exp.receiptUri.isNullOrBlank()) {
                                        IconButton(onClick = { previewExpenseReceipt = exp }) {
                                            Icon(
                                                Icons.Filled.Receipt,
                                                contentDescription = "View scanned receipt",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onDeleteExpense(exp.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete expense")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    previewExpenseReceipt?.let { exp ->
        if (!exp.receiptUri.isNullOrBlank()) {
            DocumentPreviewDialog(
                title = "${exp.category} Receipt",
                imageUri = exp.receiptUri,
                suggestedFileName = "Expense_${exp.category.replace(" ", "_")}.png",
                onDismiss = { previewExpenseReceipt = null }
            )
        }
    }
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (amount: Double, category: String, mode: PaymentMode, notes: String?, receiptUri: String?) -> Unit
) {
    val context = LocalContext.current
    val khataColors = LocalKhataColors.current
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(EXPENSE_CATEGORIES.first()) }
    var showCatMenu by remember { mutableStateOf(false) }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var notes by remember { mutableStateOf("") }
    var receiptUri by remember { mutableStateOf<String?>(null) }
    var showReceiptScanner by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val saved = BrandingImageHelper.savePickedImageToInternal(
                context = context,
                sourceUri = uri,
                prefix = "receipt",
                maxDimPx = 1440,
                folder = "scans"
            )
            receiptUri = saved ?: uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Business Expense") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorMsg = null },
                    label = { Text("Amount ₹ *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
                )
                Box {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showCatMenu = true })
                    DropdownMenu(expanded = showCatMenu, onDismissRequest = { showCatMenu = false }) {
                        EXPENSE_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { category = cat; showCatMenu = false }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(PaymentMode.CASH, PaymentMode.UPI, PaymentMode.BANK, PaymentMode.OTHER).forEach { m ->
                        FilterChip(selected = paymentMode == m, onClick = { paymentMode = m }, label = { Text(m.label) })
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Receipt Document", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showReceiptScanner = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("expense_scan_receipt_button")
                    ) {
                        Icon(Icons.Filled.DocumentScanner, contentDescription = null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Scan Receipt", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("expense_pick_receipt_button")
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Gallery", maxLines = 1)
                    }
                }
                if (!receiptUri.isNullOrBlank()) {
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
                            model = receiptUri,
                            contentDescription = "Receipt scan preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = "Receipt Attached ✓",
                            style = MaterialTheme.typography.labelLarge,
                            color = khataColors.creditGreen,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { receiptUri = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove receipt", Modifier.size(18.dp))
                        }
                    }
                }
                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        errorMsg = "Enter a valid expense amount"
                        return@Button
                    }
                    onSave(amt, category, paymentMode, notes, receiptUri)
                },
                modifier = Modifier.testTag("save_expense_button")
            ) { Text("Save Expense") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showReceiptScanner) {
        CameraScannerDialog(
            mode = CameraCaptureMode.DOCUMENT_SCAN,
            targetFolder = "scans",
            filePrefix = "receipt",
            onDismiss = { showReceiptScanner = false },
            onImageSaved = { saved ->
                receiptUri = saved
                showReceiptScanner = false
            }
        )
    }
}

@Composable
fun AddSaleDialog(
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onSave: (
        customerId: String?,
        customerName: String,
        productName: String,
        quantity: Double,
        unitPrice: Double,
        discount: Double,
        taxPercent: Double,
        paidAmount: Double,
        mode: PaymentMode
    ) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerName by remember { mutableStateOf("Walk-in Customer") }
    var showCustMenu by remember { mutableStateOf(false) }
    var productName by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("1") }
    var unitPriceText by remember { mutableStateOf("") }
    var discountText by remember { mutableStateOf("0") }
    var taxText by remember { mutableStateOf("0") }
    var paidText by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(PaymentMode.CASH) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val qty = qtyText.toDoubleOrNull() ?: 0.0
    val unitPrice = unitPriceText.toDoubleOrNull() ?: 0.0
    val discount = discountText.toDoubleOrNull() ?: 0.0
    val tax = (taxText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
    val computedTotal = ((qty * unitPrice - discount).coerceAtLeast(0.0)) * (1.0 + tax / 100.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Sale Invoice") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (customers.isNotEmpty()) {
                    Box {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "Walk-in Customer (tap to pick)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Customer") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showCustMenu = true })
                        DropdownMenu(expanded = showCustMenu, onDismissRequest = { showCustMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Walk-in Customer") },
                                onClick = { selectedCustomer = null; customerName = "Walk-in Customer"; showCustMenu = false }
                            )
                            customers.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = { selectedCustomer = c; customerName = c.name; showCustMenu = false }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it; errorMsg = null },
                    label = { Text("Item / Product Description *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sale_product_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitPriceText,
                        onValueChange = { unitPriceText = it },
                        label = { Text("Rate ₹ *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("sale_rate_input")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = { Text("Discount ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = taxText,
                        onValueChange = { taxText = it },
                        label = { Text("GST %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "Total Invoice: ${Formatters.formatRupee(computedTotal)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = paidText,
                    onValueChange = { paidText = it },
                    label = { Text("Paid Now ₹ (blank = full paid)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMode.entries.forEach { m ->
                        FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.label) })
                    }
                }
                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (productName.isBlank() || qty <= 0.0 || unitPrice <= 0.0) {
                        errorMsg = "Enter item name, quantity, and unit price"
                        return@Button
                    }
                    val paid = paidText.toDoubleOrNull() ?: computedTotal
                    onSave(selectedCustomer?.id, customerName, productName, qty, unitPrice, discount, tax, paid, mode)
                },
                modifier = Modifier.testTag("save_sale_button")
            ) { Text("Save Invoice") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
