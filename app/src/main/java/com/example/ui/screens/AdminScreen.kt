package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AuditLogItem
import com.example.data.model.Donation
import com.example.data.model.FundCategory
import com.example.data.model.QrCodeItem
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.*
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenTreasury
import com.example.ui.theme.StatusRejected
import com.example.ui.theme.StatusVerified
import com.example.ui.viewmodel.GaonFundViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminScreen(
    viewModel: GaonFundViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by GoogleAuthHelper.currentUser.collectAsState()
    val isAdmin by GoogleAuthHelper.isAdmin.collectAsState()
    val isSigningIn by GoogleAuthHelper.isSigningIn.collectAsState()
    val authError by GoogleAuthHelper.authError.collectAsState()

    val categories by viewModel.categories.collectAsState()
    val qrCodes by viewModel.qrCodes.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val donations by viewModel.donations.collectAsState()
    val isActionLoading by viewModel.isActionLoading.collectAsState()

    val pendingDonations = remember(donations) { donations.filter { !it.isVerified && it.status != "rejected" } }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddQrDialog by remember { mutableStateOf(false) }
    var donationToReject by remember { mutableStateOf<Donation?>(null) }
    var selectedReceiptDonation by remember { mutableStateOf<Donation?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("admin_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        // 1. Admin Authentication Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("admin_auth_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAdmin) ForestGreenPrimary else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isAdmin) Color.White.copy(alpha = 0.2f) else ForestGreenPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isAdmin) Color.White else ForestGreenPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isAdmin) "अधिकृत व्यवस्थापक (Admin)" else "व्यवस्थापक प्रमाणीकरण",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAdmin) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isAdmin) (currentUser?.email ?: "bhadesarbishrupa@gmail.com") else "केवल अधिकृत ग्राम कोषाध्यक्ष / व्यवस्थापक हेतु",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isAdmin) Color.White.copy(alpha = 0.85f) else Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (authError != null) {
                        Text(
                            text = authError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAdmin) Color.Yellow else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (!isAdmin) {
                        Button(
                            onClick = {
                                (context as? Activity)?.let { act ->
                                    GoogleAuthHelper.signInWithGoogle(act, coroutineScope)
                                }
                            },
                            enabled = !isSigningIn,
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("admin_google_signin_button")
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("प्रमाणीकरण हो रहा है...")
                            } else {
                                Icon(Icons.Default.Login, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("व्यवस्थापक लॉगिन (Google Sign-In)")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "पूर्ण वित्तीय एवं प्रबंधन अधिकार सक्रिय",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    GoogleAuthHelper.signOut(context, coroutineScope)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White))
                            ) {
                                Text("लॉगआउट")
                            }
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            // 2. Pending Actions Quick Summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f).testTag("admin_pending_donations_stat_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("लंबित दान सत्यापन", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${summary.pendingDonationsCount} दान",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("लंबित खर्च अनुमोदन", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${summary.pendingExpenseRequestsCount} अनुरोध",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldenTreasury
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("लंबित प्रश्न", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${summary.pendingComplaintsCount} प्रश्न",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                        }
                    }
                }
            }

            // Pending Donations Review & Instant Verification List
            if (pendingDonations.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("admin_pending_donations_review_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassTop,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "सत्यापन हेतु लंबित दान (${pendingDonations.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                                Text(
                                    text = formatRupee(pendingDonations.sumOf { it.amount }),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            pendingDonations.forEach { don ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (don.isAnonymous) "गुमनाम दानदाता" else don.donorName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "${don.categoryName} • ${don.paymentMethod}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                            if (don.utrNumber.isNotBlank()) {
                                                Text(
                                                    text = "UTR: ${don.utrNumber}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ForestGreenPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Text(
                                                text = "रसीद: ${don.receiptNumber} • ${don.date}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = formatRupee(don.amount),
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE65100),
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                OutlinedButton(
                                                    onClick = { selectedReceiptDonation = don },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(12.dp))
                                                }
                                                OutlinedButton(
                                                    onClick = { donationToReject = don },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejected)
                                                ) {
                                                    Text("अस्वीकार", style = MaterialTheme.typography.labelSmall)
                                                }
                                                Button(
                                                    onClick = { viewModel.verifyDonation(don.id) },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp).testTag("admin_verify_donation_${don.id}"),
                                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("सत्यापित", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Category Management
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "गाँव फंड श्रेणियाँ (${categories.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { showAddCategoryDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("add_category_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("नई श्रेणी जोड़ें", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            items(categories, key = { it.id }) { cat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ForestGreenPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.nameHindi, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(cat.description, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Surface(
                            color = StatusVerified.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("सक्रिय", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = StatusVerified)
                        }
                    }
                }
            }

            // 4. QR Code Donation Management Section
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "अधिकृत दान QR कोड प्रबंधन (${qrCodes.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ऑनलाइन चंदा प्राप्त करने हेतु अधिकृत बैंक UPI खाते",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                    OutlinedButton(
                        onClick = { showAddQrDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("add_qr_code_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("नया QR जोड़ें", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (qrCodes.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("कोई QR कोड नहीं मिला। 'नया QR जोड़ें' पर क्लिक करें।", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(qrCodes, key = { it.id }) { qr ->
                    val linkedCategory = categories.firstOrNull { it.id == qr.fundCategoryId }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QrCodeCanvas(
                                upiId = qr.upiId,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = qr.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = if (qr.isActive) StatusVerified.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (qr.isActive) "सक्रिय" else "निष्क्रिय",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (qr.isActive) StatusVerified else Color.DarkGray,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "UPI: ${qr.upiId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = ForestGreenPrimary
                                )
                                Text(
                                    text = "संबद्ध फंड: ${linkedCategory?.nameHindi ?: "सभी फंड (सामान्य गाँव कोष)"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = qr.isActive,
                                onCheckedChange = { viewModel.toggleQrCodeActive(qr.id, it) },
                                modifier = Modifier.testTag("toggle_qr_${qr.id}")
                            )
                        }
                    }
                }
            }

            // 5. Immutable Audit Logs Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "अपरिवर्तनीय ऑडिट लॉग्स (Audit Trail)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = ForestGreenPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${auditLogs.size} प्रविष्टियाँ",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestGreenPrimary
                        )
                    }
                }
            }

            if (auditLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "व्यवस्थापक द्वारा की जाने वाली प्रत्येक गतिविधि यहाँ स्वचालित रूप से दर्ज होगी।",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else {
                items(auditLogs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    color = GoldenTreasury.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${log.action} • ${log.entityType}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldenTreasury
                                    )
                                }
                                Text(
                                    text = log.adminEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                            if (log.newValue.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "विवरण: ${log.newValue}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (log.details.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = log.details,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSave = { hindi, eng, desc, icon ->
                viewModel.addCategory(hindi, eng, desc, icon, onSuccess = {
                    showAddCategoryDialog = false
                })
            }
        )
    }

    if (showAddQrDialog) {
        AddQrCodeDialog(
            categories = categories,
            isSaving = isActionLoading,
            onDismiss = { showAddQrDialog = false },
            onSave = { name, desc, upiId, catId, active ->
                viewModel.addOrUpdateQrCode(
                    id = "",
                    name = name,
                    description = desc,
                    upiId = upiId,
                    fundCategoryId = catId,
                    isActive = active,
                    onSuccess = { showAddQrDialog = false }
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
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (hindi: String, eng: String, desc: String, icon: String) -> Unit
) {
    var nameHindi by remember { mutableStateOf("") }
    var nameEnglish by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("add_category_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "नई फंड श्रेणी जोड़ें",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenPrimary
                )

                OutlinedTextField(
                    value = nameHindi,
                    onValueChange = { nameHindi = it },
                    label = { Text("फंड का हिंदी नाम *") },
                    placeholder = { Text("जैसे: पुस्तकालय फंड, गौशाला फंड") },
                    modifier = Modifier.fillMaxWidth().testTag("category_name_hindi_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = nameEnglish,
                    onValueChange = { nameEnglish = it },
                    label = { Text("English Name (वैकल्पिक)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("उद्देश्य एवं उपयोग विवरण") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("रद्द करें") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nameHindi.isNotBlank()) {
                                onSave(nameHindi, nameEnglish, description, "account_balance")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        modifier = Modifier.testTag("save_category_button")
                    ) {
                        Text("फंड श्रेणी सहेजें")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddQrCodeDialog(
    categories: List<FundCategory>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, upiId: String, fundCategoryId: String, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<FundCategory?>(null) }
    var isCatDropdownOpen by remember { mutableStateOf(false) }
    var isActive by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("add_qr_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "नया अधिकृत UPI QR कोड जोड़ें",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenPrimary
                )
                Text(
                    text = "ग्राम कोष हेतु अधिकृत UPI खाता व QR विवरण दर्ज करें।",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("QR कोड का शीर्षक *") },
                    placeholder = { Text("जैसे: मंदिर निर्माण UPI QR") },
                    modifier = Modifier.fillMaxWidth().testTag("qr_name_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("अधिकृत UPI ID (VPA) *") },
                    placeholder = { Text("जैसे: bhadesar.mandir@upi") },
                    modifier = Modifier.fillMaxWidth().testTag("qr_upi_id_field"),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = isCatDropdownOpen,
                    onExpandedChange = { isCatDropdownOpen = !isCatDropdownOpen }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.nameHindi ?: "सभी फंड (सामान्य गाँव कोष)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("संबद्ध फंड श्रेणी (वैकल्पिक)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCatDropdownOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCatDropdownOpen,
                        onDismissRequest = { isCatDropdownOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("सभी फंड (सामान्य गाँव कोष)") },
                            onClick = {
                                selectedCategory = null
                                isCatDropdownOpen = false
                            }
                        )
                        categories.filter { it.isActive }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nameHindi) },
                                onClick = {
                                    selectedCategory = cat
                                    isCatDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("विवरण") },
                    placeholder = { Text("जैसे: स्टेट बैंक ऑफ इंडिया, भदेसर अधिकृत खाता") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Text("सक्रिय करें (दान हेतु उपलब्ध)", style = MaterialTheme.typography.bodySmall)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) { Text("रद्द करें") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = !isSaving,
                        onClick = {
                            if (name.isBlank() || upiId.isBlank() || !upiId.contains("@")) {
                                errorMessage = "कृपया वैध शीर्षक और मान्य UPI ID (@ सहित) दर्ज करें।"
                                return@Button
                            }
                            errorMessage = null
                            onSave(name.trim(), description.trim(), upiId.trim(), selectedCategory?.id ?: "", isActive)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        modifier = Modifier.testTag("save_qr_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("सहेज रहे हैं...")
                        } else {
                            Text("QR कोड सहेजें")
                        }
                    }
                }
            }
        }
    }
}
