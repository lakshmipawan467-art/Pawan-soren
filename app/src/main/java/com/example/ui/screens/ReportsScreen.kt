package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Donation
import com.example.data.model.Expense
import com.example.data.model.FundCategory
import com.example.data.model.ProjectItem
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.formatRupee
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenTreasury
import com.example.ui.theme.StatusRejected
import com.example.ui.theme.StatusVerified
import com.example.ui.viewmodel.GaonFundViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: GaonFundViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val summary by viewModel.summary.collectAsState()
    val donations by viewModel.donations.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val isAdmin by GoogleAuthHelper.isAdmin.collectAsState()

    var selectedReportType by remember { mutableStateOf("फंड-वार सारांश") }
    var generatedReportText by remember { mutableStateOf<String?>(null) }

    val reportTypes = listOf(
        "फंड-वार सारांश",
        "मासिक रिपोर्ट",
        "वार्षिक रिपोर्ट",
        "दान सूची रिपोर्ट",
        "खर्च वाउचर रिपोर्ट",
        "परियोजना रिपोर्ट"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("reports_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ForestGreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "सार्वजनिक वित्तीय लेखा रिपोर्ट",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "भदेसर गाँव फंड का आधिकारिक मासिक, वार्षिक एवं फंड-वार लेखा-जोखा।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Report Type Selector
        Text("रिपोर्ट का प्रकार चुनें:", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            reportTypes.take(3).forEach { type ->
                FilterChip(
                    selected = selectedReportType == type,
                    onClick = { selectedReportType = type },
                    label = { Text(type, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            reportTypes.drop(3).forEach { type ->
                FilterChip(
                    selected = selectedReportType == type,
                    onClick = { selectedReportType = type },
                    label = { Text(type, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action: Generate & View Full Printable Statement
        Button(
            onClick = {
                generatedReportText = generateFormattedReport(
                    reportType = selectedReportType,
                    summary = summary,
                    categories = categories,
                    donations = donations,
                    expenses = expenses,
                    projects = projects
                )
            },
            modifier = Modifier.fillMaxWidth().testTag("generate_report_button"),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
        ) {
            Icon(Icons.Default.Description, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("पूर्ण रिपोर्ट तैयार करें एवं साझा करें")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Fund-wise Breakdown Table
        Text(
            text = "फंड-वार वर्तमान स्थिति तालिका",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(categories.filter { it.isActive }, key = { it.id }) { cat ->
                val fundDonations = donations.filter { it.categoryId == cat.id && it.isVerified }.sumOf { it.amount }
                val fundExpenses = expenses.filter { it.categoryId == cat.id && it.status == "Verified and Recorded" }.sumOf { it.amount }
                val fundBalance = fundDonations - fundExpenses

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cat.nameHindi,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "शेष: ${formatRupee(fundBalance)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (fundBalance >= 0) ForestGreenPrimary else StatusRejected
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "कुल जमा: ${formatRupee(fundDonations)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusVerified
                            )
                            Text(
                                text = "कुल खर्च: ${formatRupee(fundExpenses)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusRejected
                            )
                        }
                    }
                }
            }
        }
    }

    generatedReportText?.let { reportText ->
        Dialog(onDismissRequest = { generatedReportText = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "आधिकारिक वित्तीय रिपोर्ट",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 380.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn {
                            item {
                                Text(
                                    text = reportText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { generatedReportText = null }) {
                            Text("बंद करें")
                        }
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Gaon Fund Report", reportText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "रिपोर्ट कॉपी हो गई!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("रिपोर्ट कॉपी करें")
                        }
                    }
                }
            }
        }
    }
}

private fun generateFormattedReport(
    reportType: String,
    summary: com.example.data.model.DashboardSummary,
    categories: List<FundCategory>,
    donations: List<Donation>,
    expenses: List<Expense>,
    projects: List<ProjectItem>
): String {
    val dateStr = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("hi", "IN")).format(Date())
    val sb = StringBuilder()
    sb.appendLine("==========================================")
    sb.appendLine("   GAON FUND - भदेसर गाँव वित्तीय रिपोर्ट   ")
    sb.appendLine("   सार्वजनिक वित्तीय पारदर्शिता मंच        ")
    sb.appendLine("==========================================")
    sb.appendLine("रिपोर्ट प्रकार: $reportType")
    sb.appendLine("रिपोर्ट जारी दिनांक: $dateStr")
    sb.appendLine("------------------------------------------")
    sb.appendLine("सार्वजनिक वित्तीय संतुलन:")
    sb.appendLine("• कुल सत्यापित जमा राशि: ₹ ${summary.totalVerifiedDonations.toLong()}")
    sb.appendLine("• कुल सत्यापित खर्च:     ₹ ${summary.totalVerifiedExpenses.toLong()}")
    sb.appendLine("• वर्तमान बची राशि:      ₹ ${summary.currentBalance.toLong()}")
    sb.appendLine("------------------------------------------")
    sb.appendLine("फंड-वार स्थिति:")

    categories.filter { it.isActive }.forEach { cat ->
        val fundDon = donations.filter { it.categoryId == cat.id && it.isVerified }.sumOf { it.amount }
        val fundExp = expenses.filter { it.categoryId == cat.id && it.status == "Verified and Recorded" }.sumOf { it.amount }
        val bal = fundDon - fundExp
        sb.appendLine("${cat.nameHindi}:")
        sb.appendLine("  जमा: ₹ ${fundDon.toLong()} | खर्च: ₹ ${fundExp.toLong()} | शेष: ₹ ${bal.toLong()}")
    }

    sb.appendLine("------------------------------------------")
    sb.appendLine("विकास परियोजना प्रगति:")
    projects.forEach { p ->
        sb.appendLine("• ${p.name}: प्रगति ${p.progressPercent}% | स्वीकृत: ₹ ${p.approvedBudget.toLong()} | खर्च: ₹ ${p.spentAmount.toLong()}")
    }

    sb.appendLine("==========================================")
    sb.appendLine("नोट: GAON FUND केवल वित्तीय अभिलेख मंच है।")
    sb.appendLine("समस्त राशि अधिकृत ग्राम समिति खाते में सुरक्षित है।")
    return sb.toString()
}
