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
import com.example.data.model.FundCategory
import com.example.data.model.ProjectItem
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.formatRupee
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GoldenTreasury
import com.example.ui.theme.StatusVerified
import com.example.ui.viewmodel.GaonFundViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsScreen(
    viewModel: GaonFundViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isAdmin by GoogleAuthHelper.isAdmin.collectAsState()

    var showAddProjectDialog by remember { mutableStateOf(false) }
    var projectToUpdate by remember { mutableStateOf<ProjectItem?>(null) }

    Scaffold(
        modifier = modifier.testTag("projects_screen"),
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showAddProjectDialog = true },
                    containerColor = ForestGreenPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_project_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "नई परियोजना जोड़ें")
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

            // Projects Overview Header
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
                            .background(GoldenTreasury),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Engineering, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "भदेसर गाँव विकास परियोजनाएँ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "सड़क, नाली, सोलर लाइट, मंदिर एवं स्वच्छता विकास कार्य।",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "परियोजना सूची (${projects.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Construction,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "वर्तमान में कोई सक्रिय परियोजना दर्ज नहीं है।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(projects, key = { it.id }) { proj ->
                        ProjectItemCard(
                            project = proj,
                            isAdmin = isAdmin,
                            onUpdate = { projectToUpdate = proj }
                        )
                    }
                }
            }
        }
    }

    if (showAddProjectDialog) {
        AddProjectDialog(
            categories = categories.filter { it.isActive },
            onDismiss = { showAddProjectDialog = false },
            onSave = { name, desc, catId, est, app, start, end, stat ->
                viewModel.addProject(
                    name = name,
                    description = desc,
                    categoryId = catId,
                    estimatedBudget = est,
                    approvedBudget = app,
                    startDate = start,
                    expectedCompletionDate = end,
                    status = stat,
                    onSuccess = { showAddProjectDialog = false }
                )
            }
        )
    }

    projectToUpdate?.let { proj ->
        UpdateProjectProgressDialog(
            project = proj,
            onDismiss = { projectToUpdate = null },
            onSave = { progress, stat, note, delta ->
                viewModel.updateProjectProgress(
                    projectId = proj.id,
                    progressPercent = progress,
                    status = stat,
                    updateNote = note,
                    spentDelta = delta,
                    onSuccess = { projectToUpdate = null }
                )
            }
        )
    }
}

@Composable
private fun ProjectItemCard(
    project: ProjectItem,
    isAdmin: Boolean,
    onUpdate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}"),
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
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (project.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = project.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }
                Surface(
                    color = when (project.status) {
                        "पूर्ण" -> StatusVerified.copy(alpha = 0.15f)
                        "प्रगति पर" -> GoldenTreasury.copy(alpha = 0.15f)
                        else -> Color.LightGray.copy(alpha = 0.3f)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = project.status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (project.status) {
                            "पूर्ण" -> StatusVerified
                            "प्रगति पर" -> GoldenTreasury
                            else -> Color.DarkGray
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("कार्य प्रगति:", style = MaterialTheme.typography.labelSmall)
                Text("${project.progressPercent}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (project.progressPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = ForestGreenPrimary,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Budget vs Spent details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("स्वीकृत बजट:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(formatRupee(project.approvedBudget), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("सत्यापित खर्च:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(formatRupee(project.spentAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
                }
            }

            if (project.updates.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ताज़ा अपडेट: ${project.updates}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
            }

            if (isAdmin) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = onUpdate,
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("प्रगति अपडेट करें", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProjectDialog(
    categories: List<FundCategory>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        description: String,
        categoryId: String,
        estimatedBudget: Double,
        approvedBudget: Double,
        startDate: String,
        expectedCompletionDate: String,
        status: String
    ) -> Unit
) {
    val predefinedProjects = listOf(
        "मंदिर निर्माण एवं सौंदर्यीकरण",
        "गाँव की मुख्य सड़क का खड़ंजा व डामरीकरण",
        "जल निकासी हेतु पक्की नाली निर्माण",
        "गाँव के प्रमुख चौराहों पर सोलर लाइट",
        "शुद्ध पेयजल व्यवस्था एवं बोरवेल",
        "सामुदायिक भवन निर्माण",
        "युवा खेलकूद मैदान समतलीकरण",
        "स्वच्छता अभियान एवं कचरा निस्तारण"
    )

    val currentDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    var isCategoryDropdownOpen by remember { mutableStateOf(false) }
    var approvedBudgetText by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(currentDate) }
    var expectedCompletionDate by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("प्रगति पर") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag("add_project_dialog")
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "नई विकास परियोजना शुरू करें",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimary
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
                        label = { Text("परियोजना का नाम *") },
                        modifier = Modifier.fillMaxWidth().testTag("project_name_input"),
                        singleLine = true
                    )
                }

                // Quick presets
                item {
                    Text("शीघ्र चयन:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        predefinedProjects.take(3).forEach { p ->
                            FilterChip(
                                selected = name == p,
                                onClick = { name = p },
                                label = { Text(p.take(12) + "...", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("परियोजना विवरण एवं कार्यक्षेत्र") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                item {
                    OutlinedTextField(
                        value = approvedBudgetText,
                        onValueChange = { approvedBudgetText = it },
                        label = { Text("स्वीकृत बजट (₹) *") },
                        modifier = Modifier.fillMaxWidth().testTag("project_budget_input"),
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
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("प्रारंभ तिथि (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = expectedCompletionDate,
                        onValueChange = { expectedCompletionDate = it },
                        label = { Text("अनुमानित पूर्णता तिथि (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("रद्द करें") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val budget = approvedBudgetText.toDoubleOrNull()
                                if (budget == null || budget <= 0) {
                                    errorMessage = "कृपया वैध स्वीकृत बजट दर्ज करें।"
                                    return@Button
                                }
                                if (name.isBlank()) {
                                    errorMessage = "कृपया परियोजना का नाम दर्ज करें।"
                                    return@Button
                                }
                                onSave(
                                    name,
                                    description,
                                    selectedCategory?.id ?: "general",
                                    budget,
                                    budget,
                                    startDate,
                                    expectedCompletionDate,
                                    status
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                            modifier = Modifier.testTag("save_project_button")
                        ) {
                            Text("परियोजना दर्ज करें")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateProjectProgressDialog(
    project: ProjectItem,
    onDismiss: () -> Unit,
    onSave: (progress: Int, status: String, updateNote: String, spentDelta: Double) -> Unit
) {
    var progressSlider by remember { mutableStateOf(project.progressPercent.toFloat()) }
    var status by remember { mutableStateOf(project.status) }
    var updateNote by remember { mutableStateOf("") }
    var spentDeltaText by remember { mutableStateOf("") }

    val statusList = listOf("नियोजित", "प्रगति पर", "पूर्ण", "स्थगित")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "परियोजना प्रगति अपडेट",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenPrimary
                )
                Text(project.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

                Text("कार्य पूर्णता: ${progressSlider.toInt()}%", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = progressSlider,
                    onValueChange = { progressSlider = it },
                    valueRange = 0f..100f
                )

                Text("वर्तमान स्थिति:", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    statusList.forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = {
                                status = s
                                if (s == "पूर्ण") progressSlider = 100f
                            },
                            label = { Text(s, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = spentDeltaText,
                    onValueChange = { spentDeltaText = it },
                    label = { Text("अतिरिक्त खर्च राशि (यदि हुई हो)") },
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = updateNote,
                    onValueChange = { updateNote = it },
                    label = { Text("ताज़ा प्रगति विवरण / टिप्पणी") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("वापस") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val delta = spentDeltaText.toDoubleOrNull() ?: 0.0
                            onSave(progressSlider.toInt(), status, updateNote, delta)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                    ) {
                        Text("अपडेट सहेजें")
                    }
                }
            }
        }
    }
}
