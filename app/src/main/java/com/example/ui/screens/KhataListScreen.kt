package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.Customer
import com.example.data.model.Supplier
import com.example.ui.components.BalanceChip
import com.example.ui.components.CameraCaptureMode
import com.example.ui.components.CameraScannerDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.LocalKhataColors
import com.example.ui.viewmodel.CustomerFilter
import com.example.ui.viewmodel.PartySort
import com.example.util.BrandingImageHelper
import com.example.util.Formatters
import com.example.util.LocalAppStrings

@Composable
fun KhataListScreen(
    customers: List<Customer>,
    suppliers: List<Supplier>,
    searchQuery: String,
    activeFilter: CustomerFilter,
    activeSort: PartySort,
    initialTab: Int = 0,
    onSearchChange: (String) -> Unit,
    onFilterChange: (CustomerFilter) -> Unit,
    onSortChange: (PartySort) -> Unit,
    onCustomerClick: (Customer) -> Unit,
    onSupplierClick: (Supplier) -> Unit,
    onOpenAddCustomer: () -> Unit,
    onOpenAddSupplier: () -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var showSortMenu by remember { mutableStateOf(false) }

    val filteredSuppliers = remember(suppliers, searchQuery, activeSort) {
        val base = if (searchQuery.isBlank()) suppliers else suppliers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                (it.companyName?.contains(searchQuery, ignoreCase = true) == true) ||
                (it.phone?.contains(searchQuery, ignoreCase = true) == true)
        }
        when (activeSort) {
            PartySort.RECENT -> base.sortedByDescending { it.lastTransactionAtMillis ?: 0L }
            PartySort.BALANCE_HIGH -> base.sortedByDescending { kotlin.math.abs(it.currentBalance) }
            PartySort.NAME_AZ -> base.sortedBy { it.name.lowercase() }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (selectedTab == 0) onOpenAddCustomer() else onOpenAddSupplier() },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("khata_fab_add")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = if (selectedTab == 0) strings.addCustomer else strings.addSupplier
                )
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
                    modifier = Modifier.testTag("tab_customers")
                ) {
                    Text("${strings.customers} (${customers.size})")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("tab_suppliers")
                ) {
                    Text("${strings.suppliers} (${suppliers.size})")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text(strings.searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("khata_search_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Most Recent") },
                            onClick = { onSortChange(PartySort.RECENT); showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Highest Balance") },
                            onClick = { onSortChange(PartySort.BALANCE_HIGH); showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Name A–Z") },
                            onClick = { onSortChange(PartySort.NAME_AZ); showSortMenu = false }
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomerFilter.entries.forEach { filter ->
                        val label = when (filter) {
                            CustomerFilter.ALL -> "All"
                            CustomerFilter.TO_RECEIVE -> "To Receive"
                            CustomerFilter.TO_PAY -> "To Pay"
                            CustomerFilter.OVERDUE -> "Overdue"
                        }
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { onFilterChange(filter) },
                            label = { Text(label) },
                            leadingIcon = if (activeFilter == filter) {
                                { Icon(Icons.Filled.FilterList, contentDescription = null, Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedTab == 0) {
                    if (customers.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "No Customers Found",
                                subtitle = "Add customers manually or import directly from your phone contacts.",
                                actionLabel = "+ ${strings.addCustomer}",
                                onAction = onOpenAddCustomer
                            )
                        }
                    } else {
                        items(customers, key = { it.id }) { customer ->
                            CustomerListCard(customer = customer, onClick = { onCustomerClick(customer) })
                        }
                    }
                } else {
                    if (filteredSuppliers.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "No Suppliers Found",
                                subtitle = "Track stock purchases, GST suppliers, and due-date payables.",
                                actionLabel = "+ ${strings.addSupplier}",
                                onAction = onOpenAddSupplier
                            )
                        }
                    } else {
                        items(filteredSuppliers, key = { it.id }) { supplier ->
                            SupplierListCard(supplier = supplier, onClick = { onSupplierClick(supplier) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerListCard(customer: Customer, onClick: () -> Unit) {
    val khataColors = LocalKhataColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, khataColors.cardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!customer.photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = customer.photoUri,
                    contentDescription = customer.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.name.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        customer.phone?.takeIf { it.isNotBlank() },
                        customer.category
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                customer.lastTransactionAtMillis?.let { ts ->
                    Text(
                        text = "Last txn: ${Formatters.formatShortDay(ts)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            BalanceChip(balance = customer.currentBalance, isCustomer = true)
        }
    }
}

@Composable
private fun SupplierListCard(supplier: Supplier, onClick: () -> Unit) {
    val khataColors = LocalKhataColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, khataColors.cardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("supplier_card_${supplier.id}"),
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
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(khataColors.saffronGoldContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = supplier.name.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = khataColors.saffronGold,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = supplier.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        supplier.companyName?.takeIf { it.isNotBlank() },
                        supplier.phone?.takeIf { it.isNotBlank() }
                    ).joinToString(" • ").ifBlank { "Supplier" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            BalanceChip(balance = supplier.currentBalance, isCustomer = false)
        }
    }
}

@Composable
fun AddEditCustomerDialog(
    existing: Customer? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        phone: String?,
        address: String?,
        category: String,
        notes: String?,
        photoUri: String?,
        openingBalance: Double
    ) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "Regular") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var photoUri by remember { mutableStateOf(existing?.photoUri) }
    var openingBalText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                        val hasPhoneIdx = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                        if (nameIdx >= 0) {
                            name = cursor.getString(nameIdx) ?: name
                        }
                        if (idIdx >= 0 && hasPhoneIdx >= 0 && cursor.getInt(hasPhoneIdx) > 0) {
                            val contactId = cursor.getString(idIdx)
                            context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )?.use { pCursor ->
                                if (pCursor.moveToFirst()) {
                                    val numIdx = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (numIdx >= 0) {
                                        phone = pCursor.getString(numIdx)?.filter { it.isDigit() || it == '+' } ?: phone
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Unable to read contact", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contactPickerLauncher.launch(null)
        } else {
            Toast.makeText(context, "Contact permission denied; enter details manually", Toast.LENGTH_SHORT).show()
        }
    }

    var showCameraCapture by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val saved = BrandingImageHelper.savePickedImageToInternal(
                context = context,
                sourceUri = uri,
                prefix = "cust",
                maxDimPx = 512,
                folder = "photos"
            )
            photoUri = saved ?: uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Customer" else "Edit Customer") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Customer profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = name.trim().take(2).uppercase().ifBlank { "CU" },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showCameraCapture = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("customer_camera_photo_button")
                            ) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Camera", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("customer_gallery_photo_button")
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Gallery", maxLines = 1)
                            }
                        }
                        if (!photoUri.isNullOrBlank()) {
                            TextButton(
                                onClick = { photoUri = null },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(4.dp))
                                Text("Remove Photo", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                if (existing == null) {
                    OutlinedButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.READ_CONTACTS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) contactPickerLauncher.launch(null)
                            else contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Contacts, contentDescription = null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import from Contacts", maxLines = 1)
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorText = null },
                    label = { Text("Customer Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input")
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Regular / VIP / Wholesale)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (existing == null) {
                    OutlinedTextField(
                        value = openingBalText,
                        onValueChange = { openingBalText = it },
                        label = { Text("Opening Balance ₹ (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorText = "Please enter customer name"
                        return@Button
                    }
                    val openBal = openingBalText.toDoubleOrNull() ?: 0.0
                    onSave(name, phone, address, category, notes, photoUri, openBal)
                },
                modifier = Modifier.testTag("save_customer_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showCameraCapture) {
        CameraScannerDialog(
            mode = CameraCaptureMode.PROFILE_PHOTO,
            targetFolder = "photos",
            filePrefix = "cust",
            onDismiss = { showCameraCapture = false },
            onImageSaved = { saved ->
                photoUri = saved
                showCameraCapture = false
            }
        )
    }
}

@Composable
fun AddEditSupplierDialog(
    existing: Supplier? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        companyName: String?,
        phone: String?,
        address: String?,
        gstNumber: String?,
        openingBalance: Double
    ) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var companyName by remember { mutableStateOf(existing?.companyName ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var gstNumber by remember { mutableStateOf(existing?.gstNumber ?: "") }
    var openingBalText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Supplier" else "Edit Supplier") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorText = null },
                    label = { Text("Supplier Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_name_input")
                )
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company / Agency Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = gstNumber,
                    onValueChange = { gstNumber = it },
                    label = { Text("GST Number (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (existing == null) {
                    OutlinedTextField(
                        value = openingBalText,
                        onValueChange = { openingBalText = it },
                        label = { Text("Opening Payable ₹ (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorText = "Please enter supplier name"
                        return@Button
                    }
                    onSave(name, companyName, phone, address, gstNumber, openingBalText.toDoubleOrNull() ?: 0.0)
                },
                modifier = Modifier.testTag("save_supplier_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
