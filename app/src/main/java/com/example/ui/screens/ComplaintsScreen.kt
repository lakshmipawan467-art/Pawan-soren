package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ComplaintItem
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.StatusBadge
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.StatusVerified
import com.example.ui.viewmodel.GaonFundViewModel

@Composable
fun ComplaintsScreen(
    viewModel: GaonFundViewModel,
    modifier: Modifier = Modifier
) {
    val complaints by viewModel.complaints.collectAsState()
    val isAdmin by GoogleAuthHelper.isAdmin.collectAsState()

    var showSubmitDialog by remember { mutableStateOf(false) }
    var complaintToRespond by remember { mutableStateOf<ComplaintItem?>(null) }

    Scaffold(
        modifier = modifier.testTag("complaints_screen"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSubmitDialog = true },
                containerColor = ForestGreenPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.QuestionAnswer, contentDescription = null) },
                text = { Text("प्रश्न / शिकायत दर्ज करें") },
                modifier = Modifier.testTag("submit_complaint_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Header Banner
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
                            .background(Color(0xFFE65100)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "ग्रामवासी प्रश्न एवं शिकायत निवारण",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "रसीद गुम, प्रविष्टि त्रुटि या खर्च स्पष्टीकरण हेतु सीधा संवाद मंच।",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "दर्ज प्रश्न व शिकायतें (${complaints.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (complaints.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "वर्तमान में कोई लंबित शिकायत या प्रश्न नहीं है।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(complaints, key = { it.id }) { item ->
                        ComplaintCard(
                            complaint = item,
                            isAdmin = isAdmin,
                            onRespond = { complaintToRespond = item }
                        )
                    }
                }
            }
        }
    }

    if (showSubmitDialog) {
        SubmitComplaintDialog(
            onDismiss = { showSubmitDialog = false },
            onSubmit = { name, contact, type, subject, details ->
                viewModel.submitComplaint(
                    senderName = name,
                    senderContact = contact,
                    type = type,
                    subject = subject,
                    details = details,
                    onSuccess = { showSubmitDialog = false }
                )
            }
        )
    }

    complaintToRespond?.let { item ->
        RespondComplaintDialog(
            complaint = item,
            onDismiss = { complaintToRespond = null },
            onSave = { response, status ->
                viewModel.respondToComplaint(item.id, response, status, onSuccess = {
                    complaintToRespond = null
                })
            }
        )
    }
}

@Composable
private fun ComplaintCard(
    complaint: ComplaintItem,
    isAdmin: Boolean,
    onRespond: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("complaint_card_${complaint.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = complaint.subject,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "प्रकार: ${complaint.type} • प्रेषक: ${complaint.senderName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }
                StatusBadge(status = complaint.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = complaint.details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Admin response if available
            if (complaint.adminResponse.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = ForestGreenPrimary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "व्यवस्थापक / समिति का आधिकारिक उत्तर:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = complaint.adminResponse,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (isAdmin) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onRespond,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("उत्तर दें / स्थिति बदलें", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmitComplaintDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, contact: String, type: String, subject: String, details: String) -> Unit
) {
    val types = listOf(
        "रसीद संबंधी",
        "प्रविष्टि त्रुटि",
        "दोहराव रिकॉर्ड",
        "खर्च स्पष्टीकरण",
        "अन्य वित्तीय प्रश्न"
    )

    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(types[0]) }
    var subject by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("submit_complaint_dialog")
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "प्रश्न / शिकायत दर्ज करें",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimary
                    )
                    Text(
                        text = "गाँव कोष की पारदर्शिता बनाए रखने में आपका सहयोग अनिवार्य है।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                if (errorMessage != null) {
                    item {
                        Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("आपका नाम *") },
                        modifier = Modifier.fillMaxWidth().testTag("complaint_name_input"),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("संपर्क विवरण (केवल व्यवस्थापक को दिखेगा)") },
                        placeholder = { Text("वैकल्पिक: फोन नंबर या ईमेल") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Text("शिकायत / प्रश्न का प्रकार:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        types.take(3).forEach { t ->
                            FilterChip(
                                selected = selectedType == t,
                                onClick = { selectedType = t },
                                label = { Text(t.take(10), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("विषय *") },
                        placeholder = { Text("जैसे: 28 अक्टूबर के दान की रसीद नहीं दिख रही") },
                        modifier = Modifier.fillMaxWidth().testTag("complaint_subject_input"),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("विस्तृत विवरण *") },
                        modifier = Modifier.fillMaxWidth().testTag("complaint_details_input"),
                        minLines = 3
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("रद्द करें") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isBlank() || subject.isBlank() || details.isBlank()) {
                                    errorMessage = "कृपया सभी आवश्यक फ़ील्ड (*) भरें।"
                                    return@Button
                                }
                                onSubmit(name, contact, selectedType, subject, details)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                            modifier = Modifier.testTag("send_complaint_button")
                        ) {
                            Text("दर्ज करें")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RespondComplaintDialog(
    complaint: ComplaintItem,
    onDismiss: () -> Unit,
    onSave: (response: String, status: String) -> Unit
) {
    var response by remember { mutableStateOf(complaint.adminResponse) }
    var status by remember { mutableStateOf("Resolved") }
    val statusList = listOf("In Progress", "Resolved")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "शिकायत का आधिकारिक उत्तर दें",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenPrimary
                )
                Text(complaint.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(complaint.details, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)

                Text("नया स्थिति स्तर:", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    statusList.forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(if (s == "Resolved") "हल हो गया (Resolved)" else "प्रक्रियाधीन (In Progress)") }
                        )
                    }
                }

                OutlinedTextField(
                    value = response,
                    onValueChange = { response = it },
                    label = { Text("व्यवस्थापक का उत्तर *") },
                    placeholder = { Text("जैसे: जाँच कर रसीद क्रमांक GF-BH-XXXX जारी कर दिया गया है।") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("वापस") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (response.isNotBlank()) {
                                onSave(response, status)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                    ) {
                        Text("उत्तर दर्ज करें")
                    }
                }
            }
        }
    }
}
