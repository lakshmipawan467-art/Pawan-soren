package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Donation
import com.example.data.model.Expense
import com.example.data.model.FundCategory
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.GaonFundViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: GaonFundViewModel,
    onNavigateToTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()
    val donations by viewModel.donations.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val qrCodes by viewModel.qrCodes.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val isActionLoading by viewModel.isActionLoading.collectAsState()
    val isAdmin by GoogleAuthHelper.isAdmin.collectAsState()

    var selectedReceiptDonation by remember { mutableStateOf<Donation?>(null) }
    var selectedVoucherExpense by remember { mutableStateOf<Expense?>(null) }
    var selectedRecentCategoryFilter by remember { mutableStateOf<String?>(null) }
    var transactionTypeTab by remember { mutableIntStateOf(0) } // 0: All, 1: Contributions (जमा), 2: Expenditures (खर्च)
    var txSearchQuery by remember { mutableStateOf("") }
    var showQrDonationModal by remember { mutableStateOf(false) }
    var selectedCategoryForDetail by remember { mutableStateOf<FundCategory?>(null) }

    // Combined recent transactions list from Firestore
    val verifiedDonations = remember(donations) { donations.filter { it.isVerified } }
    val verifiedExpenses = remember(expenses) { expenses.filter { it.status == "Verified and Recorded" } }

    // Unique categories from recent transactions
    val recentCategoryNames = remember(verifiedDonations, verifiedExpenses) {
        val set = mutableSetOf<String>()
        verifiedDonations.take(15).forEach { if (it.categoryName.isNotBlank()) set.add(it.categoryName) }
        verifiedExpenses.take(15).forEach { if (it.categoryName.isNotBlank()) set.add(it.categoryName) }
        set.toList()
    }

    val combinedTransactions = remember(
        verifiedDonations,
        verifiedExpenses,
        transactionTypeTab,
        selectedRecentCategoryFilter,
        txSearchQuery
    ) {
        val list = mutableListOf<DashboardTransaction>()
        val query = txSearchQuery.trim().lowercase()

        if (transactionTypeTab != 2) {
            verifiedDonations.forEach { don ->
                val matchesCategory = selectedRecentCategoryFilter == null || don.categoryName == selectedRecentCategoryFilter
                val matchesQuery = query.isBlank() ||
                        don.donorName.lowercase().contains(query) ||
                        don.receiptNumber.lowercase().contains(query) ||
                        don.purpose.lowercase().contains(query) ||
                        don.categoryName.lowercase().contains(query)
                if (matchesCategory && matchesQuery) {
                    list.add(DashboardTransaction.Contribution(don))
                }
            }
        }
        if (transactionTypeTab != 1) {
            verifiedExpenses.forEach { exp ->
                val matchesCategory = selectedRecentCategoryFilter == null || exp.categoryName == selectedRecentCategoryFilter
                val matchesQuery = query.isBlank() ||
                        exp.purpose.lowercase().contains(query) ||
                        exp.vendorName.lowercase().contains(query) ||
                        exp.voucherNumber.lowercase().contains(query) ||
                        exp.categoryName.lowercase().contains(query)
                if (matchesCategory && matchesQuery) {
                    list.add(DashboardTransaction.Expenditure(exp))
                }
            }
        }
        list.sortedByDescending { it.date }.take(if (transactionTypeTab == 0) 10 else 15)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // 1. Village Header Banner Card
        item {
            VillageHeaderBanner()
        }

        // 2. Primary Total Balance Summary Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("official_balance_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF69F0AE))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "वर्तमान बची राशि (कुल गाँव कोष)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formatRupee(summary.currentBalance),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = "बैलेंस",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Inflows and Outflows Pill Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color(0xFF69F0AE),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("कुल जमा", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                    Text(
                                        formatRupee(summary.totalVerifiedDonations),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = Color(0xFFFF8A80),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("कुल खर्च", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                    Text(
                                        formatRupee(summary.totalVerifiedExpenses),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "बैलेंस = कुल सत्यापित दान − कुल सत्यापित खर्च (वास्तविक व निष्पक्ष)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // Quick Village Fund Actions (QR Donation & Transparency)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showQrDonationModal = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dashboard_qr_donate_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("QR कोड से दान", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }

                OutlinedButton(
                    onClick = { onNavigateToTab(1) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dashboard_view_all_donations_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreenPrimary)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("दान पावती सूची", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // 3. Quick KPI Cards (2x2 Grid)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "सत्यापित दान राशि",
                    amount = summary.totalVerifiedDonations,
                    icon = Icons.Default.VolunteerActivism,
                    containerColor = MintContainer,
                    contentColor = OnMintContainer,
                    subtitle = "${verifiedDonations.size} कुल अंशदान",
                    modifier = Modifier.weight(1f).clickable { onNavigateToTab(1) },
                    testTag = "total_donations_stat"
                )
                StatCard(
                    title = "सत्यापित गाँव खर्च",
                    amount = summary.totalVerifiedExpenses,
                    icon = Icons.Default.Payments,
                    containerColor = Color(0xFFFFEBEE),
                    contentColor = Color(0xFFC62828),
                    subtitle = "${verifiedExpenses.size} व्यय वाउचर",
                    modifier = Modifier.weight(1f).clickable { onNavigateToTab(2) },
                    testTag = "total_expenses_stat"
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTab(3) }
                        .testTag("active_projects_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GoldenContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GoldenTreasury.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Engineering, contentDescription = null, tint = GoldenTreasury)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("विकास परियोजनाएँ", style = MaterialTheme.typography.labelMedium, color = OnGoldenContainer)
                            Text("${summary.totalProjectsCount} सक्रिय", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnGoldenContainer)
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTab(2) }
                        .testTag("pending_expenses_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LightGreenContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldSecondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PendingActions, contentDescription = null, tint = EmeraldSecondary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("समीक्षाधीन अनुरोध", style = MaterialTheme.typography.labelMedium, color = OnLightGreenContainer)
                            Text("${summary.pendingExpenseRequestsCount} लंबित", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnLightGreenContainer)
                        }
                    }
                }
            }
        }

        // Pending Donations Alert Banner (if any donations are pending verification)
        if (summary.pendingDonationsCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTab(1) }
                        .testTag("dashboard_pending_donations_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFE0B2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "सत्यापन प्रतीक्षित दान: ${summary.pendingDonationsCount} अंशदान",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "कुल राशि: ${formatRupee(summary.pendingDonationsTotal)} • सत्यापन उपरांत ग्राम कोष में जुड़ेगी।",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray
                            )
                        }
                        Button(
                            onClick = { onNavigateToTab(1) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            modifier = Modifier.height(34.dp).testTag("view_pending_donations_button")
                        ) {
                            Text("देखें / सत्यापन", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 4. Fund-Wise Summary Cards (Horizontal Carousel of Categories)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "गाँव फंड श्रेणियाँ एवं शेष राशि",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "विभिन्न उद्देश्यों के लिए संचित कोष",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                    TextButton(onClick = { onNavigateToTab(1) }) {
                        Text("विवरण", color = ForestGreenPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(categories.filter { it.isActive }, key = { it.id }) { cat ->
                        val fundDon = donations.filter { it.categoryId == cat.id && it.isVerified }.sumOf { it.amount }
                        val fundExp = expenses.filter { it.categoryId == cat.id && it.status == "Verified and Recorded" }.sumOf { it.amount }
                        val bal = fundDon - fundExp

                        CategoryBalanceCard(
                            category = cat,
                            balance = bal,
                            donationsTotal = fundDon,
                            expensesTotal = fundExp,
                            onClick = { selectedCategoryForDetail = cat }
                        )
                    }
                }
            }
        }

        // 5. Recent Contributions & Expenditures Activity Stream
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ForestGreenPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "गाँव कोष लेनदेन (जमा एवं खर्च)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "क्लाउड फायरस्टोर से सीधे प्राप्त लाइव पारदर्शी रिकॉर्ड्स",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                    TextButton(onClick = { onNavigateToTab(if (transactionTypeTab == 2) 2 else 1) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("पूरा ब्यौरा", color = ForestGreenPrimary, fontWeight = FontWeight.Bold)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = ForestGreenPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Segmented tab row: All vs Contributions vs Expenditures
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_segmented_row")
                ) {
                    SegmentedButton(
                        selected = transactionTypeTab == 0,
                        onClick = { transactionTypeTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text(
                            text = "सभी (${verifiedDonations.size + verifiedExpenses.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (transactionTypeTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    SegmentedButton(
                        selected = transactionTypeTab == 1,
                        onClick = { transactionTypeTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text(
                            text = "जमा अंशदान (${verifiedDonations.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (transactionTypeTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    SegmentedButton(
                        selected = transactionTypeTab == 2,
                        onClick = { transactionTypeTab = 2 },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text(
                            text = "गाँव खर्च (${verifiedExpenses.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (transactionTypeTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Search Bar within Dashboard
                OutlinedTextField(
                    value = txSearchQuery,
                    onValueChange = { txSearchQuery = it },
                    placeholder = { Text("दानदाता, उद्देश्य, रसीद या वाउचर संख्या खोजें...", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (txSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { txSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "साफ करें", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_tx_search_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (recentCategoryNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedRecentCategoryFilter == null,
                            onClick = { selectedRecentCategoryFilter = null },
                            label = { Text("सभी श्रेणियाँ", style = MaterialTheme.typography.labelSmall) }
                        )
                        recentCategoryNames.forEach { catName ->
                            FilterChip(
                                selected = selectedRecentCategoryFilter == catName,
                                onClick = {
                                    selectedRecentCategoryFilter = if (selectedRecentCategoryFilter == catName) null else catName
                                },
                                label = { Text(catName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }

        if (combinedTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (txSearchQuery.isNotBlank() || selectedRecentCategoryFilter != null)
                                    "खोज के अनुसार कोई रिकॉर्ड नहीं मिला।"
                                else when (transactionTypeTab) {
                                    1 -> "वर्तमान में कोई सत्यापित जमा राशि उपलब्ध नहीं है।"
                                    2 -> "वर्तमान में कोई सत्यापित खर्च रिकॉर्ड उपलब्ध नहीं है।"
                                    else -> "वर्तमान में कोई सत्यापित लेनदेन उपलब्ध नहीं है। व्यवस्थापक द्वारा दर्ज किए जाने पर वास्तविक रिकॉर्ड्स यहाँ प्रदर्शित होंगे।"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        } else {
            combinedTransactions.forEach { tx ->
                item(key = tx.id) {
                    when (tx) {
                        is DashboardTransaction.Contribution -> {
                            val don = tx.donation
                            RecentTransactionCard(
                                isDonation = true,
                                title = if (don.isAnonymous) "गुमनाम दानदाता (Anonymous)" else don.donorName,
                                subtitle = "${don.paymentMethod} • ${don.purpose}",
                                categoryName = don.categoryName,
                                amount = don.amount,
                                date = don.date,
                                refNumber = don.receiptNumber,
                                status = "सत्यापित",
                                onClick = { selectedReceiptDonation = don }
                            )
                        }
                        is DashboardTransaction.Expenditure -> {
                            val exp = tx.expense
                            RecentTransactionCard(
                                isDonation = false,
                                title = exp.purpose,
                                subtitle = if (exp.vendorName.isNotBlank()) "विक्रेता: ${exp.vendorName}" else exp.billNumber,
                                categoryName = exp.categoryName,
                                amount = exp.amount,
                                date = exp.date,
                                refNumber = exp.voucherNumber,
                                status = exp.status,
                                onClick = { selectedVoucherExpense = exp }
                            )
                        }
                    }
                }
            }
        }

        // 6. Monthly Collection vs Expenditure Bar Chart Card
        item {
            MonthlyBarChart(donations = donations, expenses = expenses)
        }

        // 7. Village Transparency & Committee Assurance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LightGreenContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "सार्वजनिक वित्तीय पारदर्शिता नियम",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "गाँव कोष की वास्तविक धनराशि अधिकृत ग्राम विकास समिति के अधिकृत बैंक खाते में सुरक्षित रहती है। GAON FUND केवल वित्तीय रिकॉर्ड्स को सार्वजनिक व पारदर्शी बनाए रखने का कार्य करता है।",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }

    selectedReceiptDonation?.let { don ->
        DigitalReceiptModal(
            donation = don,
            isAdmin = isAdmin,
            onVerify = { viewModel.verifyDonation(don.id) },
            onDismiss = { selectedReceiptDonation = null }
        )
    }

    selectedVoucherExpense?.let { exp ->
        VoucherDetailModal(expense = exp, onDismiss = { selectedVoucherExpense = null })
    }

    selectedCategoryForDetail?.let { cat ->
        FundDetailModal(
            fund = cat,
            donations = donations,
            expenses = expenses,
            projects = projects,
            isAdmin = isAdmin,
            onDismiss = { selectedCategoryForDetail = null },
            onDonateToFund = { _ ->
                selectedCategoryForDetail = null
                showQrDonationModal = true
            },
            onViewReceipt = { don ->
                selectedReceiptDonation = don
            },
            onViewVoucher = { exp ->
                selectedVoucherExpense = exp
            }
        )
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
}

@Composable
private fun CategoryBalanceCard(
    category: FundCategory,
    balance: Double,
    donationsTotal: Double,
    expensesTotal: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() }
            .testTag("category_card_${category.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LightGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Surface(
                    color = if (balance >= 0) StatusVerified.copy(alpha = 0.12f) else StatusRejected.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (balance >= 0) "सक्रिय" else "ऋणात्मक",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (balance >= 0) StatusVerified else StatusRejected,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = category.nameHindi,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatRupee(balance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (balance >= 0) ForestGreenPrimary else StatusRejected
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "जमा: ₹${donationsTotal.toLong()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = "खर्च: ₹${expensesTotal.toLong()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

private sealed class DashboardTransaction(
    val id: String,
    val date: String,
    val amount: Double,
    val categoryName: String,
    val refNumber: String
) {
    class Contribution(val donation: Donation) : DashboardTransaction(
        id = donation.id,
        date = donation.date,
        amount = donation.amount,
        categoryName = donation.categoryName,
        refNumber = donation.receiptNumber
    )
    class Expenditure(val expense: Expense) : DashboardTransaction(
        id = expense.id,
        date = expense.date,
        amount = expense.amount,
        categoryName = expense.categoryName,
        refNumber = expense.voucherNumber
    )
}

@Composable
private fun RecentTransactionCard(
    isDonation: Boolean,
    title: String,
    subtitle: String = "",
    categoryName: String,
    amount: Double,
    date: String,
    refNumber: String,
    status: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("recent_transaction_card_${refNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
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
                    .clip(CircleShape)
                    .background(if (isDonation) MintContainer else Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDonation) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isDonation) ForestGreenPrimary else StatusRejected,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.DarkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = ForestGreenPrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestGreenPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$refNumber • $date",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isDonation) "+" else "-"} ${formatRupee(amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDonation) ForestGreenPrimary else StatusRejected
                )
                Spacer(modifier = Modifier.height(2.dp))
                StatusBadge(status = status)
            }
        }
    }
}
