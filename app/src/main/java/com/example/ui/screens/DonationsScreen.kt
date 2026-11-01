package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.model.Donation
import com.example.data.model.FundCategory
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.*
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.OnMintContainer
import com.example.ui.theme.StatusRejected
import com.example.ui.theme.StatusVerified
import com.example.ui.viewmodel.GaonFundViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DonationsScreen(
    viewModel: GaonFundViewModel,
    modifier: Modifier = Modifier
) {
    val donations by viewModel.donations.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val qrCodes by viewModel.qrCodes.collectAsState()
    val isAdmin by GoogleAuthHelper.isAdmin.collectAsState()
    val isActionLoading by viewModel.isActionLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var statusFilterTab by remember { mutableIntStateOf(0) } // 0: All, 1: Verified, 2: Pending, 3: Rejected
    var showAddDonationDialog by remember { mutableStateOf(false) }
    var showQrDonationModal by remember { mutableStateOf(false) }
    var selectedReceiptDonation by remember { mutableStateOf<Donation?>(null) }
    var donationToReject by remember { mutableStateOf<Donation?>(null) }

    val verifiedCount = remember(donations) { donations.count { it.isVerified || it.status == "verified" } }
    val pendingCount = remember(donations) { donations.count { !it.isVerified && it.status != "rejected" } }
    val rejectedCount = remember(donations) { donations.count { it.status == "rejected" } }

    val filteredDonations = remember(donations, searchQuery, selectedCategoryId, statusFilterTab, isAdmin) {
        donations.filter { don ->
            val matchesCategory = selectedCategoryId == null || don.categoryId == selectedCategoryId
            val matchesStatus = when (statusFilterTab) {
                1 -> don.isVerified || don.status == "verified"
                2 -> !don.isVerified && don.status != "rejected"
                3 -> don.status == "rejected"
                else -> true // All
            }
            val matchesSearch = searchQuery.isBlank() ||
                don.donorName.contains(searchQuery, ignoreCase = true) ||
                don.receiptNumber.contains(searchQuery, ignoreCase = true) ||
                don.categoryName.contains(searchQuery, ignoreCase = true) ||
                don.purpose.contains(searchQuery, ignoreCase = true) ||
                don.referenceNumber.contains(searchQuery, ignoreCase = true) ||
                don.utrNumber.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesStatus && matchesSearch
        }
    }

    Scaffold(
        modifier = modifier.testTag("donations_screen"),
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showAddDonationDialog = true },
                    containerColor = ForestGreenPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_donation_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "दान दर्ज करें")
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

            // Villager Direct Donation Action Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_donation_action_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ForestGreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "गाँव कोष में अंशदान करें",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "अधिकृत QR कोड से UPI भुगतान करें अथवा UTR जमा करें।",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Button(
                        onClick = { showQrDonationModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("open_qr_donation_btn")
                    ) {
                        Text("QR दान", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("donation_search_field"),
                placeholder = { Text("दानदाता का नाम, रसीद संख्या, यूटीआर खोजें...") },
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

            // Category Filter Chips
            CategoryFilterRow(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { selectedCategoryId = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status Filter Tabs (All, Verified, Pending, Rejected)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilterTab == 0,
                    onClick = { statusFilterTab = 0 },
                    label = { Text("सभी (${donations.size})", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForestGreenPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_all_donations")
                )
                FilterChip(
                    selected = statusFilterTab == 1,
                    onClick = { statusFilterTab = 1 },
                    label = { Text("सत्यापित (${verifiedCount})", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StatusVerified,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_verified_donations")
                )
                FilterChip(
                    selected = statusFilterTab == 2,
                    onClick = { statusFilterTab = 2 },
                    label = { Text("लंबित (${pendingCount})", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE65100),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_pending_donations")
                )
                FilterChip(
                    selected = statusFilterTab == 3,
                    onClick = { statusFilterTab = 3 },
                    label = { Text("अस्वीकृत (${rejectedCount})", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StatusRejected,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_rejected_donations")
                )
            }

            // Pending Donations Alert Banner (if any exist)
            if (pendingCount > 0 && statusFilterTab != 2) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { statusFilterTab = 2 }
                        .testTag("pending_donations_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$pendingCount दान रिकॉर्ड सत्यापन प्रतीक्षित हैं",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = if (isAdmin) "समीक्षा कर 1-क्लिक में आधिकारिक कोष में जोड़ें।" else "कोषाध्यक्ष द्वारा सत्यापन के बाद आधिकारिक बैलेंस में जुड़ेंगे।",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.DarkGray
                            )
                        }
                        TextButton(
                            onClick = { statusFilterTab = 2 },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("देखें", color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Info bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (statusFilterTab) {
                        1 -> "सत्यापित दान अभिलेख (${filteredDonations.size})"
                        2 -> "सत्यापन प्रतीक्षित दान (${filteredDonations.size})"
                        3 -> "अस्वीकृत दान रिकॉर्ड (${filteredDonations.size})"
                        else -> "सार्वजनिक दान अभिलेख (${filteredDonations.size})"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (isAdmin) {
                    Text(
                        text = "व्यवस्थापक मोड",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForestGreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }


            Spacer(modifier = Modifier.height(8.dp))

            if (filteredDonations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VolunteerActivism,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when {
                                searchQuery.isNotBlank() || selectedCategoryId != null -> "कोई परिणाम नहीं मिला।"
                                statusFilterTab == 2 -> "वर्तमान में कोई दान सत्यापन हेतु लंबित नहीं है।"
                                else -> "वर्तमान में कोई दान रिकॉर्ड उपलब्ध नहीं है।"
                            },
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
                    items(filteredDonations, key = { it.id }) { don ->
                        DonationItemCard(
                            donation = don,
                            isAdmin = isAdmin,
                            onViewReceipt = { selectedReceiptDonation = don },
                            onVerify = { viewModel.verifyDonation(don.id) },
                            onReject = { donationToReject = don }
                        )
                    }
                }
            }
        }
    }

    if (showQrDonationModal) {
        val activeCategories = if (categories.isNotEmpty()) {
            categories.filter { it.isActive }
        } else {
            viewModel.repository.defaultCategories
        }
        QrCodeDonationModal(
            categories = activeCategories,
            qrCodes = qrCodes,
            isSubmitting = isActionLoading,
            onDismiss = { showQrDonationModal = false },
            onSubmitDonation = { donorName, isAnon, amt, catId, catName, method, ref, utr, purpose, proof, screenshot, date ->
                viewModel.submitDonationApplication(
                    donorName = donorName,
                    isAnonymous = isAnon,
                    amount = amt,
                    categoryId = catId,
                    categoryName = catName,
                    paymentMethod = method,
                    referenceNumber = ref,
                    utrNumber = utr,
                    purpose = purpose,
                    proofNote = proof,
                    screenshotUrl = screenshot,
                    date = date,
                    onSuccess = { showQrDonationModal = false }
                )
            }
        )
    }

    donationToReject?.let { don ->
        RejectDonationDialog(
            donation = don,
            onDismiss = { donationToReject = null },
            onConfirmReject = { reason ->
                viewModel.rejectDonation(
                    donationId = don.id,
                    reason = reason,
                    onSuccess = { donationToReject = null }
                )
            }
        )
    }

    if (showAddDonationDialog) {
        val activeCategories = if (categories.isNotEmpty()) {
            categories.filter { it.isActive }
        } else {
            viewModel.repository.defaultCategories
        }
        AddDonationDialog(
            categories = activeCategories,
            isSaving = isActionLoading,
            onDismiss = { if (!isActionLoading) showAddDonationDialog = false },
            onSave = { donor, isAnon, amt, catId, catName, method, ref, purpose, proof, date, isVer ->
                viewModel.addDonation(
                    donorName = donor,
                    isAnonymous = isAnon,
                    amount = amt,
                    categoryId = catId,
                    categoryName = catName,
                    paymentMethod = method,
                    referenceNumber = ref,
                    purpose = purpose,
                    proofNote = proof,
                    date = date,
                    isVerified = isVer,
                    onSuccess = { showAddDonationDialog = false }
                )
            }
        )
    }

    selectedReceiptDonation?.let { don ->
        DigitalReceiptModal(
            donation = don,
            isAdmin = isAdmin,
            onVerify = { viewModel.verifyDonation(don.id) },
            onReject = {
                donationToReject = don
                selectedReceiptDonation = null
            },
            onDismiss = { selectedReceiptDonation = null }
        )
    }
}


@Composable
private fun DonationItemCard(
    donation: Donation,
    isAdmin: Boolean,
    onViewReceipt: () -> Unit,
    onVerify: () -> Unit,
    onReject: () -> Unit
) {
    val isVerified = donation.isVerified || donation.status == "verified"
    val isRejected = donation.status == "rejected"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("donation_card_${donation.id}")
            .clickable { onViewReceipt() },
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
                            .background(
                                when {
                                    isRejected -> Color(0xFFFFEBEE)
                                    isVerified -> MintContainer
                                    else -> Color(0xFFFFF3E0)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isRejected -> Icons.Default.Cancel
                                isVerified -> Icons.Default.VolunteerActivism
                                else -> Icons.Default.HourglassTop
                            },
                            contentDescription = null,
                            tint = when {
                                isRejected -> StatusRejected
                                isVerified -> ForestGreenPrimary
                                else -> Color(0xFFE65100)
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (donation.isAnonymous) "गुमनाम दानदाता (Anonymous)" else donation.donorName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = donation.categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestGreenPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        if (donation.utrNumber.isNotBlank()) {
                            Text(
                                text = "UTR: ${donation.utrNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatRupee(donation.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isRejected -> Color.Gray
                            isVerified -> ForestGreenPrimary
                            else -> Color(0xFFE65100)
                        }
                    )
                    StatusBadge(
                        when {
                            isRejected -> "अस्वीकृत"
                            isVerified -> "सत्यापित"
                            else -> "सत्यापन लंबित"
                        }
                    )
                }
            }

            if (isRejected && donation.rejectionReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFEBEE),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "कारण: ${donation.rejectionReason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusRejected,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
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
                    text = "रसीद: ${donation.receiptNumber} • ${donation.date}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onViewReceipt,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("रसीद", style = MaterialTheme.typography.labelSmall)
                    }
                    if (isAdmin && !isVerified && !isRejected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedButton(
                            onClick = onReject,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejected)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("अस्वीकार", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = onVerify,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("सत्यापित", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDonationDialog(
    categories: List<FundCategory>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        donorName: String,
        isAnonymous: Boolean,
        amount: Double,
        categoryId: String,
        categoryName: String,
        paymentMethod: String,
        referenceNumber: String,
        purpose: String,
        proofNote: String,
        date: String,
        isVerified: Boolean
    ) -> Unit
) {
    val currentDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var donorName by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    var isCategoryDropdownOpen by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("नकद (Cash)") }
    var referenceNumber by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var proofNote by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(currentDate) }
    var isVerified by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val paymentMethods = listOf("नकद (Cash)", "बैंक ट्रांसफर (NEFT/RTGS)", "यूपीआई (UPI)", "चेक (Cheque)")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_donation_dialog")
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "नया दान अंशदान दर्ज करें",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimary
                    )
                    Text(
                        text = "भदेसर गाँव फंड में सार्वजनिक दान की प्रविष्टि करें।",
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isAnonymous,
                            onCheckedChange = { isAnonymous = it },
                            modifier = Modifier.testTag("anonymous_checkbox")
                        )
                        Text("दानदाता को गुप्त रखें (Anonymous Donor)")
                    }
                }

                if (!isAnonymous) {
                    item {
                        OutlinedTextField(
                            value = donorName,
                            onValueChange = { donorName = it },
                            label = { Text("दानदाता का नाम *") },
                            modifier = Modifier.fillMaxWidth().testTag("donor_name_field"),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("दान राशि (₹) *") },
                        modifier = Modifier.fillMaxWidth().testTag("donation_amount_field"),
                        singleLine = true
                    )
                }

                // Category Selector
                item {
                    ExposedDropdownMenuBox(
                        expanded = isCategoryDropdownOpen,
                        onExpandedChange = { isCategoryDropdownOpen = !isCategoryDropdownOpen }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.nameHindi ?: "फंड श्रेणी चुनें",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("फंड श्रेणी *") },
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
                    Text("भुगतान विधि:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        paymentMethods.forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = { Text(method.split(" ")[0], style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = referenceNumber,
                        onValueChange = { referenceNumber = it },
                        label = { Text("रसीद / यूटीआर / चेक नंबर (वैकल्पिक)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        label = { Text("दान का उद्देश्य") },
                        placeholder = { Text("जैसे: मंदिर निर्माण सामग्री, सोहराय पर्व, आदि") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = proofNote,
                        onValueChange = { proofNote = it },
                        label = { Text("जमा रसीद / प्रमाण विवरण") },
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isVerified) MintContainer.copy(alpha = 0.5f) else Color(0xFFFFF3E0),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "सत्यापन स्थिति (Verification Status):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isVerified) ForestGreenPrimary else Color(0xFFE65100)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = isVerified,
                                onClick = { isVerified = true },
                                label = { Text("सत्यापित (Verified)") },
                                leadingIcon = {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ForestGreenPrimary,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                ),
                                modifier = Modifier.testTag("option_verified_donation")
                            )
                            FilterChip(
                                selected = !isVerified,
                                onClick = { isVerified = false },
                                label = { Text("लंबित (Pending)") },
                                leadingIcon = {
                                    Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE65100),
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                ),
                                modifier = Modifier.testTag("option_pending_donation")
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isVerified)
                                "✓ राशि प्राप्त हो चुकी है। यह तुरंत कुल कोष बैलेंस में जुड़ेगी और आधिकारिक रसीद जारी होगी।"
                            else
                                "⏳ राशि अभी बैंक क्लियरेंस या कोषाध्यक्ष सत्यापन प्रतीक्षित है। सत्यापन के बाद ही कोष बैलेंस में जुड़ेगी।",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isVerified) ForestGreenPrimary else Color(0xFFE65100)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !isSaving
                        ) {
                            Text("रद्द करें")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            enabled = !isSaving,
                            onClick = {
                                val cleanAmount = amountText.replace("₹", "").replace(",", "").trim().toDoubleOrNull()
                                if (cleanAmount == null || cleanAmount <= 0.0) {
                                    errorMessage = "कृपया वैध दान राशि दर्ज करें (शून्य से अधिक)।"
                                    return@Button
                                }
                                val cat = selectedCategory ?: categories.firstOrNull()
                                if (cat == null) {
                                    errorMessage = "कृपया एक फंड श्रेणी चुनें।"
                                    return@Button
                                }
                                val cleanCatId = cat.id.trim().ifBlank { "cat_mandir" }
                                val cleanCatName = cat.nameHindi.trim().ifBlank { "मंदिर निर्माण फंड" }
                                val cleanDate = date.trim().let { if (it.length >= 8) it else currentDate }
                                val cleanDonor = if (isAnonymous) "गुमनाम दानदाता" else donorName.trim().ifBlank { "ग्रामवासी" }

                                errorMessage = null
                                onSave(
                                    cleanDonor,
                                    isAnonymous,
                                    cleanAmount,
                                    cleanCatId,
                                    cleanCatName,
                                    paymentMethod,
                                    referenceNumber.trim(),
                                    purpose.trim().ifBlank { "गाँव विकास एवं जनहित" },
                                    proofNote.trim(),
                                    cleanDate,
                                    isVerified
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVerified) ForestGreenPrimary else Color(0xFFE65100)
                            ),
                            modifier = Modifier.testTag("save_donation_button")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("सेव हो रहा है...")
                            } else {
                                Icon(
                                    imageVector = if (isVerified) Icons.Default.Check else Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isVerified) "दान दर्ज करें एवं रसीद जारी करें"
                                    else "लंबित दान दर्ज करें (सत्यापन प्रतीक्षित)"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
