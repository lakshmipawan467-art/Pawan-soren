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
import com.example.data.model.NoticeItem
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenTreasury
import com.example.ui.viewmodel.GaonFundViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoticesScreen(
    viewModel: GaonFundViewModel,
    modifier: Modifier = Modifier
) {
    val notices by viewModel.notices.collectAsState()
    val isAdmin by GoogleAuthHelper.isAdmin.collectAsState()

    var showAddNoticeDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag("notices_screen"),
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showAddNoticeDialog = true },
                    containerColor = ForestGreenPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_notice_fab")
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = "सूचना जारी करें")
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

            // Notice Board Header
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
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "गाँव सूचना पट्ट (Notice Board)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ग्राम सभा, मासिक फंड समीक्षा एवं बैठकों की आधिकारिक सूचना।",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "सूचनाएँ (${notices.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (notices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "वर्तमान में कोई नई सूचना प्रकाशित नहीं है।",
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
                    items(notices, key = { it.id }) { notice ->
                        NoticeItemCard(
                            notice = notice,
                            isAdmin = isAdmin,
                            onDelete = { viewModel.deleteNotice(notice.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddNoticeDialog) {
        AddNoticeDialog(
            onDismiss = { showAddNoticeDialog = false },
            onSave = { title, content, cat, date, isPinned ->
                viewModel.addNotice(
                    title = title,
                    content = content,
                    category = cat,
                    publishedDate = date,
                    isPinned = isPinned,
                    onSuccess = { showAddNoticeDialog = false }
                )
            }
        )
    }
}

@Composable
private fun NoticeItemCard(
    notice: NoticeItem,
    isAdmin: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notice_card_${notice.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notice.isPinned) Color(0xFFFFFDE7) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (notice.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = GoldenTreasury,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = notice.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (notice.isPinned) Color(0xFF795548) else MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    color = ForestGreenPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = notice.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = ForestGreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notice.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "प्रकाशक: ${notice.authorName} • दिनांक: ${notice.publishedDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                if (isAdmin) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "हटाएं",
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddNoticeDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String, date: String, isPinned: Boolean) -> Unit
) {
    val currentDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("ग्राम सभा बैठक") }
    var date by remember { mutableStateOf(currentDate) }
    var isPinned by remember { mutableStateOf(false) }

    val categories = listOf("ग्राम सभा बैठक", "मासिक फंड सारांश", "परियोजना अपडेट", "सांस्कृतिक पर्व", "सामान्य सूचना")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("add_notice_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "नई सूचना प्रकाशित करें",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenPrimary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("सूचना का शीर्षक *") },
                    modifier = Modifier.fillMaxWidth().testTag("notice_title_input"),
                    singleLine = true
                )

                Text("सूचना श्रेणी:", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.take(3).forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c.take(10), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("सूचना का पूर्ण विवरण *") },
                    modifier = Modifier.fillMaxWidth().testTag("notice_content_input"),
                    minLines = 3
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("दिनांक (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPinned, onCheckedChange = { isPinned = it })
                    Text("सूचना पट्ट पर सबसे ऊपर पिन करें")
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("रद्द करें") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && content.isNotBlank()) {
                                onSave(title, content, category, date, isPinned)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        modifier = Modifier.testTag("publish_notice_button")
                    ) {
                        Text("प्रकाशित करें")
                    }
                }
            }
        }
    }
}
