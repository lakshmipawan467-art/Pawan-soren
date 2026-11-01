package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Donation
import com.example.data.model.Expense
import com.example.data.model.FundCategory
import com.example.data.model.ProjectItem
import com.example.data.model.QrCodeItem
import com.example.data.model.NotificationItem
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatRupee(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return formatter.format(amount).replace("₹", "₹ ")
}

@Composable
fun VillageHeaderBanner(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("village_header_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(ForestGreenPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = "भदेसर गाँव",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "भदेसर गाँव (Bhadesar)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = ForestGreenPrimary,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "पारदर्शिता मंच",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "गाँव के सार्वजनिक कोष का पूर्ण, सत्यापित एवं खुला लेखा-जोखा।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    amount: Double,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    testTag: String = "stat_card"
) {
    Card(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor.copy(alpha = 0.9f),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatRupee(amount),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status) {
        "Verified and Recorded", "सत्यापित", "Verified" ->
            Triple(StatusVerified.copy(alpha = 0.15f), StatusVerified, "सत्यापित एवं दर्ज")
        "Approved", "स्वीकृत" ->
            Triple(StatusInProcess.copy(alpha = 0.15f), StatusInProcess, "स्वीकृत (Approved)")
        "Paid – Awaiting Verification" ->
            Triple(GoldenTreasury.copy(alpha = 0.15f), GoldenTreasury, "भुगतान हो गया - सत्यापन प्रतीक्षित")
        "Payment Pending" ->
            Triple(StatusPending.copy(alpha = 0.15f), StatusPending, "भुगतान लंबित")
        "Pending Approval", "Pending", "लंबित" ->
            Triple(Color(0xFFE65100).copy(alpha = 0.15f), Color(0xFFE65100), "लंबित (Pending)")
        "Rejected", "अस्वीकृत" ->
            Triple(StatusRejected.copy(alpha = 0.15f), StatusRejected, "अस्वीकृत (Rejected)")
        "Resolved", "हल हो गया" ->
            Triple(StatusVerified.copy(alpha = 0.15f), StatusVerified, "हल हो गया (Resolved)")
        else ->
            Triple(Color.LightGray.copy(alpha = 0.3f), Color.DarkGray, status)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

@Composable
fun CategoryFilterRow(
    categories: List<FundCategory>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCategoryId == null,
            onClick = { onCategorySelected(null) },
            label = { Text("सभी फंड (All)") },
            leadingIcon = {
                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ForestGreenPrimary,
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White
            )
        )
        categories.filter { it.isActive }.forEach { cat ->
            FilterChip(
                selected = selectedCategoryId == cat.id,
                onClick = { onCategorySelected(if (selectedCategoryId == cat.id) null else cat.id) },
                label = { Text(cat.nameHindi) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ForestGreenPrimary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
fun MonthlyBarChart(
    donations: List<Donation>,
    expenses: List<Expense>,
    modifier: Modifier = Modifier
) {
    // Group verified amounts by month
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val verifiedDonationsByMonth = DoubleArray(12) { 0.0 }
    val verifiedExpensesByMonth = DoubleArray(12) { 0.0 }

    donations.filter { it.isVerified && it.date.length >= 7 }.forEach { don ->
        try {
            val monthIndex = don.date.substring(5, 7).toInt() - 1
            if (monthIndex in 0..11) {
                verifiedDonationsByMonth[monthIndex] += don.amount
            }
        } catch (_: Exception) {}
    }

    expenses.filter { it.status == "Verified and Recorded" && it.date.length >= 7 }.forEach { exp ->
        try {
            val monthIndex = exp.date.substring(5, 7).toInt() - 1
            if (monthIndex in 0..11) {
                verifiedExpensesByMonth[monthIndex] += exp.amount
            }
        } catch (_: Exception) {}
    }

    val maxVal = maxOf(
        verifiedDonationsByMonth.maxOrNull() ?: 1.0,
        verifiedExpensesByMonth.maxOrNull() ?: 1.0,
        1000.0
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "मासिक आय एवं व्यय का विवरण",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(ForestGreenPrimary, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("जमा", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(10.dp).background(StatusRejected, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("खर्च", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Chart area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height - 24.dp.toPx()
                    val barGroupWidth = canvasWidth / 12f
                    val barWidth = barGroupWidth * 0.35f

                    // Draw baseline
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(0f, canvasHeight),
                        end = Offset(canvasWidth, canvasHeight),
                        strokeWidth = 2f
                    )

                    for (i in 0 until 12) {
                        val groupX = i * barGroupWidth
                        val donHeight = ((verifiedDonationsByMonth[i] / maxVal) * canvasHeight).toFloat()
                        val expHeight = ((verifiedExpensesByMonth[i] / maxVal) * canvasHeight).toFloat()

                        // Donation Bar (Green)
                        if (donHeight > 0) {
                            drawRoundRect(
                                color = ForestGreenPrimary,
                                topLeft = Offset(groupX + 2f, canvasHeight - donHeight),
                                size = Size(barWidth, donHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )
                        }

                        // Expense Bar (Red)
                        if (expHeight > 0) {
                            drawRoundRect(
                                color = StatusRejected,
                                topLeft = Offset(groupX + barWidth + 4f, canvasHeight - expHeight),
                                size = Size(barWidth, expHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )
                        }
                    }
                }
            }

            // Month Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                months.forEach { m ->
                    Text(
                        text = m,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun DigitalReceiptModal(
    donation: Donation,
    isAdmin: Boolean = false,
    onVerify: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("digital_receipt_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                val headerBg = if (donation.isVerified) LightGreenContainer else Color(0xFFFFF3E0)
                val iconColor = if (donation.isVerified) ForestGreenPrimary else Color(0xFFE65100)
                val headerIcon = if (donation.isVerified) Icons.Default.CheckCircle else Icons.Default.HourglassTop

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(headerBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = headerIcon,
                        contentDescription = "रसीद",
                        tint = iconColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (donation.isVerified) "डिजिटल दान रसीद" else "दान पावती (सत्यापन प्रतीक्षित)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (donation.isVerified) ForestGreenPrimary else Color(0xFFE65100)
                )
                Text(
                    text = if (donation.isVerified) "भदेसर ग्राम विकास समिति (GAON FUND)" else "भदेसर ग्राम विकास समिति • कोषाध्यक्ष सत्यापन शेष",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Receipt Content
                ReceiptRow("रसीद संख्या:", donation.receiptNumber)
                ReceiptRow("दानदाता का नाम:", if (donation.isAnonymous) "गुमनाम दानदाता (Anonymous)" else donation.donorName)
                ReceiptRow("फंड श्रेणी:", donation.categoryName)
                ReceiptRow("दान राशि:", formatRupee(donation.amount), isAmount = true)
                ReceiptRow("दिनांक:", donation.date)
                ReceiptRow("भुगतान विधि:", donation.paymentMethod)
                if (donation.referenceNumber.isNotBlank()) {
                    ReceiptRow("संदर्भ / यूटीआर:", donation.referenceNumber)
                }
                ReceiptRow("उद्देश्य:", donation.purpose)
                if (donation.proofNote.isNotBlank()) {
                    ReceiptRow("प्रमाण / विवरण:", donation.proofNote)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Verification Stamp
                val stampBg = if (donation.isVerified) LightGreenContainer else Color(0xFFFFF3E0)
                val stampTextColor = if (donation.isVerified) ForestGreenPrimary else Color(0xFFE65100)
                val stampIcon = if (donation.isVerified) Icons.Default.VerifiedUser else Icons.Default.PendingActions

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(stampBg, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = stampIcon,
                        contentDescription = null,
                        tint = stampTextColor,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (donation.isVerified) "आधिकारिक रूप से सत्यापित एवं कोष में दर्ज" else "सत्यापन प्रतीक्षित (Pending Verification)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = stampTextColor
                        )
                        Text(
                            text = if (donation.isVerified)
                                "यह डिजिटल प्रमाण भदेसर गाँव सार्वजनिक फंड द्वारा जारी है।"
                            else
                                "यह दान अभी व्यवस्थापक द्वारा सत्यापित किया जाना है। सत्यापन के बाद ही आधिकारिक गाँव कोष में जुड़ेगा।",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.DarkGray
                        )
                    }
                }

                if (donation.status == "rejected" || donation.rejectionReason.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = StatusRejected, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("अस्वीकृत दान रिकॉर्ड", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = StatusRejected)
                                Text("कारण: ${donation.rejectionReason.ifBlank { "बैंक रिकॉर्ड से मिलान न होने के कारण" }}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                            }
                        }
                    }
                }

                if (isAdmin && !donation.isVerified && donation.status != "rejected" && onVerify != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onReject != null) {
                            OutlinedButton(
                                onClick = {
                                    onReject()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f).testTag("modal_reject_donation_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejected)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("अस्वीकार करें", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Button(
                            onClick = {
                                onVerify()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).testTag("modal_verify_donation_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("सत्यापित करें", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                val context = LocalContext.current
                OutlinedButton(
                    onClick = {
                        val shareText = """
                            भदेसर गाँव सार्वजनिक फंड - डिजिटल रसीद
                            रसीद संख्या: ${donation.receiptNumber}
                            दानदाता: ${if (donation.isAnonymous) "गुमनाम दानदाता" else donation.donorName}
                            फंड: ${donation.categoryName}
                            राशि: ${formatRupee(donation.amount)}
                            दिनांक: ${donation.date}
                            स्थिति: ${if (donation.isVerified) "सत्यापित एवं कोष में दर्ज" else if (donation.status == "rejected") "अस्वीकृत" else "सत्यापन प्रतीक्षित"}
                            गाँव: भदेसर गाँव (चित्तौड़गढ़, राजस्थान)
                        """.trimIndent()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "रसीद साझा करें"))
                    },
                    modifier = Modifier.fillMaxWidth().testTag("share_receipt_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("रसीद साझा करें (Share Receipt)")
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag("close_receipt_button")
                ) {
                    Text("पावती बंद करें")
                }
            }
        }
    }
}

@Composable
fun RejectDonationDialog(
    donation: Donation,
    onDismiss: () -> Unit,
    onConfirmReject: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRejected, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("दान रिकॉर्ड अस्वीकार करें", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "क्या आप रसीद संख्या '${donation.receiptNumber}' (राशि: ${formatRupee(donation.amount)}) को अस्वीकार करना चाहते हैं? अस्वीकृत दान फंड बैलेंस में नहीं जुड़ेगा।",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("अस्वीकृति का कारण (वैकल्पिक)") },
                    placeholder = { Text("जैसे: बैंक खाते में राशि प्राप्त नहीं हुई / गलत UTR") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करें")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirmReject(reason) },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRejected)
                    ) {
                        Text("अस्वीकार करें")
                    }
                }
            }
        }
    }
}

@Composable
fun QrCodeCanvas(
    upiId: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, ForestGreenPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val numBlocks = 25
            val blockSize = canvasWidth / numBlocks

            // Draw white background
            drawRect(color = Color.White, size = size)

            // QR Finder pattern helper
            fun drawFinder(startX: Float, startY: Float) {
                val finderBlocks = 7
                val finderSize = finderBlocks * blockSize
                // Outer black box
                drawRoundRect(
                    color = Color(0xFF1B5E20),
                    topLeft = Offset(startX, startY),
                    size = Size(finderSize, finderSize),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                // Inner white box
                drawRect(
                    color = Color.White,
                    topLeft = Offset(startX + blockSize, startY + blockSize),
                    size = Size(finderSize - 2 * blockSize, finderSize - 2 * blockSize)
                )
                // Center black block
                drawRoundRect(
                    color = Color(0xFF1B5E20),
                    topLeft = Offset(startX + 2 * blockSize, startY + 2 * blockSize),
                    size = Size(finderSize - 4 * blockSize, finderSize - 4 * blockSize),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }

            // Top-left, Top-right, Bottom-left finders
            drawFinder(0f, 0f)
            drawFinder(canvasWidth - 7 * blockSize, 0f)
            drawFinder(0f, canvasHeight - 7 * blockSize)

            // Pseudo-random data module matrix generated deterministically from UPI ID
            val hash = upiId.hashCode()
            for (i in 0 until numBlocks) {
                for (j in 0 until numBlocks) {
                    val inTL = i < 8 && j < 8
                    val inTR = i >= numBlocks - 8 && j < 8
                    val inBL = i < 8 && j >= numBlocks - 8
                    val inCenter = i in 10..14 && j in 10..14
                    if (!inTL && !inTR && !inBL && !inCenter) {
                        val bit = ((hash xor (i * 31 + j * 17)) and 0x7)
                        if (bit == 0 || bit == 2 || bit == 5 || (i == 6 && j % 2 == 0) || (j == 6 && i % 2 == 0)) {
                            drawRoundRect(
                                color = Color(0xFF1C311E),
                                topLeft = Offset(i * blockSize + 1f, j * blockSize + 1f),
                                size = Size(blockSize - 1.5f, blockSize - 1.5f),
                                cornerRadius = CornerRadius(1.5f, 1.5f)
                            )
                        }
                    }
                }
            }

            // Center UPI Rupee Shield Badge
            val centerBadgeSize = 5 * blockSize
            val centerTopLeft = Offset((canvasWidth - centerBadgeSize) / 2f, (canvasHeight - centerBadgeSize) / 2f)
            drawRoundRect(
                color = ForestGreenPrimary,
                topLeft = centerTopLeft,
                size = Size(centerBadgeSize, centerBadgeSize),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = GoldenTreasury,
                topLeft = Offset(centerTopLeft.x + 2f, centerTopLeft.y + 2f),
                size = Size(centerBadgeSize - 4f, centerBadgeSize - 4f),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeDonationModal(
    categories: List<FundCategory>,
    qrCodes: List<QrCodeItem>,
    initialCategoryId: String? = null,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmitDonation: (
        donorName: String,
        isAnonymous: Boolean,
        amount: Double,
        categoryId: String,
        categoryName: String,
        paymentMethod: String,
        referenceNumber: String,
        utrNumber: String,
        purpose: String,
        proofNote: String,
        screenshotUrl: String,
        date: String
    ) -> Unit
) {
    val context = LocalContext.current
    val currentDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var selectedCat by remember {
        mutableStateOf(
            categories.firstOrNull { it.id == initialCategoryId } ?: categories.firstOrNull()
        )
    }
    var isCatDropdownOpen by remember { mutableStateOf(false) }

    // Find active QR code linked to this fund or generic
    val activeQrCode = remember(selectedCat, qrCodes) {
        val linked = qrCodes.firstOrNull { it.isActive && it.fundCategoryId == selectedCat?.id }
        linked ?: qrCodes.firstOrNull { it.isActive && it.fundCategoryId.isBlank() } ?: qrCodes.firstOrNull { it.isActive }
    }

    var donorName by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var utrNumber by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var proofNote by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(currentDate) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isEnlargedQr by remember { mutableStateOf(false) }

    val upiId = activeQrCode?.upiId?.ifBlank { "bhadesar.vikas@upi" } ?: "bhadesar.vikas@upi"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
                .testTag("qr_donation_modal")
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "QR कोड से ऑनलाइन दान करें",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                            Text(
                                text = "भदेसर गाँव अधिकृत UPI खाते में सीधे भुगतान करें।",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "बंद करें")
                        }
                    }
                }

                // Step 1: Select Fund
                item {
                    ExposedDropdownMenuBox(
                        expanded = isCatDropdownOpen,
                        onExpandedChange = { isCatDropdownOpen = !isCatDropdownOpen }
                    ) {
                        OutlinedTextField(
                            value = selectedCat?.nameHindi ?: "फंड चुनें",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("जिस फंड में दान देना चाहते हैं *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCatDropdownOpen) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isCatDropdownOpen,
                            onDismissRequest = { isCatDropdownOpen = false }
                        ) {
                            categories.filter { it.isActive }.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.nameHindi) },
                                    onClick = {
                                        selectedCat = cat
                                        isCatDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Step 2: Show QR Code & Copy UPI ID
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = activeQrCode?.name ?: "भदेसर गाँव विकास कोष QR",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                            Text(
                                text = activeQrCode?.description ?: "अधिकृत ग्राम कोष UPI खाता",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Interactive QR Canvas
                            QrCodeCanvas(
                                upiId = upiId,
                                modifier = Modifier
                                    .size(if (isEnlargedQr) 240.dp else 190.dp)
                                    .clickable { isEnlargedQr = !isEnlargedQr }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // UPI ID with copy & share button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("अधिकृत UPI ID:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(upiId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", upiId))
                                            Toast.makeText(context, "UPI ID कॉपी कर ली गई: $upiId", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "कॉपी करें", tint = ForestGreenPrimary)
                                    }
                                    IconButton(
                                        onClick = {
                                            val shareIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "भदेसर गाँव फंड में दान हेतु अधिकृत UPI ID: $upiId (${selectedCat?.nameHindi ?: "गाँव विकास कोष"})")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "UPI विवरण साझा करें"))
                                        }
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "साझा करें", tint = ForestGreenPrimary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ⓘ PhonePe, Google Pay, Paytm, BHIM द्वारा स्कैन करें अथवा UPI ID पर सीधे भुगतान करें।",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Step 3: Transaction Details submission
                item {
                    Text(
                        text = "भुगतान के बाद 12-अंकों का UTR / Transaction ID दर्ज करें",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMessage != null) {
                    item {
                        Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
                            modifier = Modifier.testTag("qr_anonymous_checkbox")
                        )
                        Text("दानदाता का नाम सार्वजनिक रूप से गुप्त रखें (Anonymous)")
                    }
                }

                if (!isAnonymous) {
                    item {
                        OutlinedTextField(
                            value = donorName,
                            onValueChange = { donorName = it },
                            label = { Text("दानदाता का नाम *") },
                            modifier = Modifier.fillMaxWidth().testTag("qr_donor_name_field"),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("दान राशि (₹) *") },
                        placeholder = { Text("जैसे: 500, 1000, 5000") },
                        modifier = Modifier.fillMaxWidth().testTag("qr_donation_amount_field"),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = utrNumber,
                        onValueChange = { utrNumber = it },
                        label = { Text("12-अंकों का UTR / Transaction No. *") },
                        placeholder = { Text("UPI संदर्भ / UTR संख्या") },
                        modifier = Modifier.fillMaxWidth().testTag("qr_utr_field"),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("भुगतान दिनांक (YYYY-MM-DD) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        label = { Text("दान का उद्देश्य / टिप्पणी (वैकल्पिक)") },
                        placeholder = { Text("जैसे: मंदिर निर्माण सामग्री, सोहराय पर्व, आदि") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = proofNote,
                        onValueChange = { proofNote = it },
                        label = { Text("स्क्रीनशॉट / अतिरिक्त विवरण (वैकल्पिक)") },
                        placeholder = { Text("जैसे: Google Pay से भेजा गया") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                            Text("रद्द करें")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            enabled = !isSubmitting,
                            onClick = {
                                val cleanAmt = amountText.replace("₹", "").replace(",", "").trim().toDoubleOrNull()
                                if (cleanAmt == null || cleanAmt <= 0.0) {
                                    errorMessage = "कृपया वैध दान राशि दर्ज करें (शून्य से अधिक)।"
                                    return@Button
                                }
                                val cleanUtr = utrNumber.trim()
                                if (cleanUtr.length < 6) {
                                    errorMessage = "कृपया वैध UTR / Transaction ID दर्ज करें।"
                                    return@Button
                                }
                                val cat = selectedCat ?: categories.firstOrNull()
                                if (cat == null) {
                                    errorMessage = "कृपया एक फंड श्रेणी चुनें।"
                                    return@Button
                                }
                                errorMessage = null
                                onSubmitDonation(
                                    if (isAnonymous) "गुमनाम दानदाता" else donorName.trim().ifBlank { "ग्रामवासी" },
                                    isAnonymous,
                                    cleanAmt,
                                    cat.id,
                                    cat.nameHindi,
                                    "यूपीआई (UPI QR)",
                                    cleanUtr,
                                    cleanUtr,
                                    purpose.trim().ifBlank { "गाँव विकास एवं जनहित" },
                                    proofNote.trim(),
                                    "",
                                    date.trim().ifBlank { currentDate }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                            modifier = Modifier.testTag("submit_qr_donation_btn")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("सबमिट हो रहा है...")
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("दान आवेदन सबमिट करें")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FundDetailModal(
    fund: FundCategory,
    donations: List<Donation>,
    expenses: List<Expense>,
    projects: List<ProjectItem>,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onDonateToFund: (FundCategory) -> Unit,
    onViewReceipt: (Donation) -> Unit,
    onViewVoucher: (Expense) -> Unit
) {
    val fundDonations = remember(donations, fund) { donations.filter { it.categoryId == fund.id } }
    val fundExpenses = remember(expenses, fund) { expenses.filter { it.categoryId == fund.id } }
    val fundProjects = remember(projects, fund) { projects.filter { it.categoryId == fund.id } }

    val verifiedDonations = remember(fundDonations) {
        fundDonations.filter { it.isVerified || it.status == "verified" }.sumOf { it.amount }
    }
    val verifiedExpenses = remember(fundExpenses) {
        fundExpenses.filter { it.status == "Verified and Recorded" }.sumOf { it.amount }
    }
    val fundBalance = verifiedDonations - verifiedExpenses
    val pendingDonationsCount = remember(fundDonations) {
        fundDonations.count { !it.isVerified && it.status != "rejected" }
    }

    var activeTab by remember { mutableIntStateOf(0) } // 0: Donations, 1: Expenses, 2: Projects

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.9f)
                .padding(vertical = 12.dp)
                .testTag("fund_detail_modal_${fund.id}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ForestGreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = fund.nameHindi,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                            Text(
                                text = fund.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "बंद करें")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Fund Balance Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${fund.nameHindi} - शेष बैलेंस",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatRupee(fundBalance),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("कुल सत्यापित जमा", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                    Text(formatRupee(verifiedDonations), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("कुल सत्यापित खर्च", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                    Text(formatRupee(verifiedExpenses), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                if (pendingDonationsCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$pendingDonationsCount दान रिकॉर्ड सत्यापन प्रतीक्षित हैं",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action: Donate to this Fund
                Button(
                    onClick = {
                        onDismiss()
                        onDonateToFund(fund)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("donate_to_fund_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenTreasury)
                ) {
                    Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("इस फंड में चंदा दें (QR / UPI)", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Donations, Expenses, Projects
                TabRow(selectedTabIndex = activeTab) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("दान इतिहास (${fundDonations.size})", style = MaterialTheme.typography.labelSmall) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("खर्च इतिहास (${fundExpenses.size})", style = MaterialTheme.typography.labelSmall) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("परियोजनाएँ (${fundProjects.size})", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        0 -> {
                            if (fundDonations.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("इस फंड में अभी कोई दान दर्ज नहीं है।", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(fundDonations, key = { it.id }) { don ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().clickable { onViewReceipt(don) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(if (don.isAnonymous) "गुमनाम दानदाता" else don.donorName, fontWeight = FontWeight.Bold)
                                                    Text("रसीद: ${don.receiptNumber} • ${don.date}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(formatRupee(don.amount), fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
                                                    StatusBadge(if (don.isVerified) "सत्यापित" else if (don.status == "rejected") "अस्वीकृत" else "Pending")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            if (fundExpenses.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("इस फंड से अभी कोई खर्च दर्ज नहीं है।", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(fundExpenses, key = { it.id }) { exp ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().clickable { onViewVoucher(exp) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(exp.purpose, fontWeight = FontWeight.Bold)
                                                    Text("वाउचर: ${exp.voucherNumber} • ${exp.date}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(formatRupee(exp.amount), fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                                    StatusBadge(exp.status)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (fundProjects.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("इस फंड से संबद्ध कोई विकास परियोजना नहीं है।", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(fundProjects, key = { it.id }) { proj ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(proj.name, fontWeight = FontWeight.Bold)
                                                Text(proj.description, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("स्वीकृत बजट: ${formatRupee(proj.approvedBudget)} • स्थिति: ${proj.status}", style = MaterialTheme.typography.labelSmall, color = ForestGreenPrimary)
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsBottomSheet(
    notifications: List<NotificationItem>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = ForestGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "गाँव कोष सूचनाएँ (${notifications.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("वर्तमान में कोई नई सूचना उपलब्ध नहीं है।", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (notif.type) {
                                                "DONATION_VERIFIED" -> StatusVerified.copy(alpha = 0.2f)
                                                "DONATION_REJECTED" -> StatusRejected.copy(alpha = 0.2f)
                                                "DONATION_SUBMITTED" -> Color(0xFFFFF3E0)
                                                else -> ForestGreenPrimary.copy(alpha = 0.15f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (notif.type) {
                                            "DONATION_VERIFIED" -> Icons.Default.CheckCircle
                                            "DONATION_REJECTED" -> Icons.Default.Cancel
                                            "DONATION_SUBMITTED" -> Icons.Default.VolunteerActivism
                                            else -> Icons.Default.Notifications
                                        },
                                        contentDescription = null,
                                        tint = when (notif.type) {
                                            "DONATION_VERIFIED" -> StatusVerified
                                            "DONATION_REJECTED" -> StatusRejected
                                            "DONATION_SUBMITTED" -> Color(0xFFE65100)
                                            else -> ForestGreenPrimary
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(notif.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(notif.message, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoucherDetailModal(
    expense: Expense,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("voucher_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF3E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "वाउचर",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "गाँव खर्च वाउचर विवरण",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
                Text(
                    text = "भदेसर ग्राम विकास कोष",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                ReceiptRow("वाउचर संख्या:", expense.voucherNumber)
                ReceiptRow("खर्च राशि:", formatRupee(expense.amount), isAmount = true)
                ReceiptRow("फंड श्रेणी:", expense.categoryName)
                ReceiptRow("उद्देश्य:", expense.purpose)
                ReceiptRow("विक्रेता / प्राप्तकर्ता:", expense.vendorName.ifBlank { "लागू नहीं" })
                if (expense.billNumber.isNotBlank()) {
                    ReceiptRow("बिल / रसीद संख्या:", expense.billNumber)
                }
                ReceiptRow("दिनांक:", expense.date)

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("वर्तमान स्थिति:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    StatusBadge(status = expense.status)
                }
                if (expense.rejectionReason.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "अस्वीकृति कारण: ${expense.rejectionReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusRejected
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag("close_voucher_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Text("वाउचर बंद करें")
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(
    label: String,
    value: String,
    isAmount: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = if (isAmount) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
            else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.55f)
        )
    }
}


