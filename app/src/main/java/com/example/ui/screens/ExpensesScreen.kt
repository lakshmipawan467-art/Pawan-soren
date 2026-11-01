package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Expense
import com.example.data.model.FundCategory
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.*
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.StatusRejected
import com.example.ui.viewmodel.GaonFundViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExpensesScreen(
    viewModel: GaonFundViewModel,
    modifier: Modifier = Modifier
) {
    val expenses by viewModel.expenses.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isAdmin by GoogleAuthHelper.isAdmin.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var selectedVoucherExpense by remember { mutableStateOf<Expense?>(null) }
    var expenseToReject by remember { mutableStateOf<Expense?>(null) }

    val filteredExpenses = remember(expenses, searchQuery, selectedCategoryId, selectedStatusFilter) {
        expenses.filter { exp ->
            (selectedCategoryId == null || exp.categoryId == selectedCategoryId) &&
            (selectedStatusFilter == null || exp.status == selectedStatusFilter) &&
            (searchQuery.isBlank() ||
             exp.purpose.contains(searchQuery, ignoreCase = true) ||
             exp.voucherNumber.contains(searchQuery, ignoreCase = true) ||
             exp.vendorName.contains(searchQuery, ignoreCase = true) ||
             exp.categoryName.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        modifier = modifier.testTag("expenses_screen"),
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showAddExpenseDialog = true },
                    containerColor = Color(0xFFC62828),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_expense_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "खर्च अनुरोध दर्ज करें")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_search_field"),
                placeholder = { Text("उद्देश्य, वाउचर या विक्रेता खोजें...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "साफ करें")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter
            CategoryFilterRow(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { selectedCategoryId = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "सार्वजनिक खर्च रिकॉर्ड्स (${filteredExpenses.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "केवल 'सत्यापित' खर्च कोष से घटता है",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedCategoryId != null)
                                "कोई परिणाम नहीं मिला।"
                            else
                                "वर्तमान में कोई खर्च रिकॉर्ड उपलब्ध नहीं है।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredExpenses, key = { it.id }) { exp ->
                        ExpenseItemCard(
                            expense = exp,
                            isAdmin = isAdmin,
                            onViewVoucher = { selectedVoucherExpense = exp },
                            onApprove = { viewModel.updateExpenseStatus(exp.id, "Approved") },
                            onReject = { expenseToReject = exp },
                            onMarkPaid = { viewModel.updateExpenseStatus(exp.id, "Paid – Awaiting Verification") },
                            onVerifyAndRecord = { viewModel.updateExpenseStatus(exp.id, "Verified and Recorded") }
                        )
                    }
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            categories = categories.filter { it.isActive },
            onDismiss = { showAddExpenseDialog = false },
            onSave = { amt, catId, catName, purp, vend, bill, date, stat ->
                viewModel.addExpense(
                    amount = amt,
                    categoryId = catId,
                    categoryName = catName,
                    purpose = purp,
                    vendorName = vend,
                    billNumber = bill,
                    date = date,
                    status = stat,
                    onSuccess = { showAddExpenseDialog = false }
                )
            }
        )
    }

    selectedVoucherExpense?.let { exp ->
        VoucherDetailModal(expense = exp, onDismiss = { selectedVoucherExpense = null })
    }

    expenseToReject?.let { exp ->
        RejectExpenseDialog(
            expense = exp,
            onDismiss = { expenseToReject = null },
            onReject = { reason ->
                viewModel.updateExpenseStatus(exp.id, "Rejected", reason)
                expenseToReject = null
            }
        )
    }
}

@Composable
private fun ExpenseItemCard(
    expense: Expense,
    isAdmin: Boolean,
    onViewVoucher: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onMarkPaid: () -> Unit,
    onVerifyAndRecord: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_card_${expense.id}")
            .clickable { onViewVoucher() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = expense.purpose,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${expense.categoryName} • विक्रेता: ${expense.vendorName.ifBlank { "लागू नहीं" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "- ${formatRupee(expense.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFC62828)
                    )
                    StatusBadge(expense.status)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "वाउचर: ${expense.voucherNumber} • ${expense.date}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                OutlinedButton(
                    onClick = onViewVoucher,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("वाउचर देखें", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Admin Multi-step Workflow Buttons
            if (isAdmin) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (expense.status) {
                        "Pending Approval" -> {
                            OutlinedButton(
                                onClick = onReject,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejected),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("अस्वीकृत", style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = onApprove,
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("स्वीकृत करें", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        "Approved" -> {
                            Button(
                                onClick = onMarkPaid,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("भुगतान दर्ज करें", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        "Paid – Awaiting Verification", "Payment Pending" -> {
                            Button(
                                onClick = onVerifyAndRecord,
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("सत्यापित करें एवं दर्ज करें", style = MaterialTheme.typography.labelSmall)
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
private fun AddExpenseDialog(
    categories: List<FundCategory>,
    onDismiss: () -> Unit,
    onSave: (
        amount: Double,
        categoryId: String,
        categoryName: String,
        purpose: String,
        vendorName: String,
        billNumber: String,
        date: String,
        status: String
    ) -> Unit
) {
    val currentDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    var isCategoryDropdownOpen by remember { mutableStateOf(false) }
    var purpose by remember { mutableStateOf("") }
    var vendorName by remember { mutableStateOf("") }
    var billNumber by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(currentDate) }
    var status by remember { mutableStateOf("Pending Approval") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val statusOptions = listOf("Pending Approval", "Approved", "Verified and Recorded")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_expense_dialog")
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "नया गाँव खर्च दर्ज करें",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                    Text(
                        text = "गाँव फंड से किए जाने वाले किसी भी खर्च का विवरण प्रविष्ट करें।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                if (errorMessage != null) {
                    item {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        label = { Text("खर्च का उद्देश्य / विवरण *") },
                        placeholder = { Text("जैसे: नाली निर्माण हेतु गिट्टी व सीमेंट, सोलर लाइट मरम्मत") },
                        modifier = Modifier.fillMaxWidth().testTag("expense_purpose_field"),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("खर्च राशि (₹) *") },
                        modifier = Modifier.fillMaxWidth().testTag("expense_amount_field"),
                        singleLine = true
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = isCategoryDropdownOpen,
                        onExpandedChange = { isCategoryDropdownOpen = !isCategoryDropdownOpen }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.nameHindi ?: "फंड श्रेणी चुनें",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("संबंधित फंड श्रेणी *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownOpen) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isCategoryDropdownOpen,
                            onDismissRequest = { isCategoryDropdownOpen = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.nameHindi) },
                                    onClick = {
                                        selectedCategory = cat
                                        isCategoryDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = vendorName,
                        onValueChange = { vendorName = it },
                        label = { Text("दुकान / विक्रेता / ठेकेदार का नाम") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = billNumber,
                        onValueChange = { billNumber = it },
                        label = { Text("बिल / इनवॉइस नंबर") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("दिनांक (YYYY-MM-DD) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Text("प्रारंभिक स्थिति:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        statusOptions.forEach { opt ->
                            FilterChip(
                                selected = status == opt,
                                onClick = { status = opt },
                                label = {
                                    val label = when(opt) {
                                        "Pending Approval" -> "लंबित"
                                        "Approved" -> "स्वीकृत"
                                        else -> "सत्यापित"
                                    }
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("रद्द करें") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = amountText.toDoubleOrNull()
                                if (amt == null || amt <= 0) {
                                    errorMessage = "कृपया वैध राशि दर्ज करें।"
                                    return@Button
                                }
                                if (purpose.isBlank()) {
                                    errorMessage = "कृपया खर्च का उद्देश्य दर्ज करें।"
                                    return@Button
                                }
                                if (selectedCategory == null) {
                                    errorMessage = "कृपया एक फंड श्रेणी चुनें।"
                                    return@Button
                                }
                                onSave(
                                    amt,
                                    selectedCategory!!.id,
                                    selectedCategory!!.nameHindi,
                                    purpose,
                                    vendorName,
                                    billNumber,
                                    date,
                                    status
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            modifier = Modifier.testTag("save_expense_button")
                        ) {
                            Text("खर्च वाउचर दर्ज करें")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RejectExpenseDialog(
    expense: Expense,
    onDismiss: () -> Unit,
    onReject: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "खर्च अनुरोध अस्वीकृत करें",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StatusRejected
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${expense.purpose} (₹${expense.amount})",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("अस्वीकृति का कारण") },
                    placeholder = { Text("जैसे: बिल अधूरा है, बजट की सीमा से बाहर, आदि") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("वापस") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onReject(reason.ifBlank { "समिति द्वारा अस्वीकृत" }) },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRejected)
                    ) {
                        Text("अस्वीकृत करें")
                    }
                }
            }
        }
    }
}
