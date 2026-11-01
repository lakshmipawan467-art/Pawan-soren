package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.GaonFundRepository
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.NotificationsBottomSheet
import com.example.ui.screens.*
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.GaonFundTheme
import com.example.ui.theme.GoldenTreasury
import com.example.ui.theme.LightGreenContainer
import com.example.ui.viewmodel.GaonFundViewModel
import com.example.ui.viewmodel.GaonFundViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Background silent auto sign-in check
        GoogleAuthHelper.attemptAutoSignIn(this, lifecycleScope)

        val repository = GaonFundRepository(this)

        setContent {
            GaonFundTheme {
                val viewModel: GaonFundViewModel = viewModel(
                    factory = GaonFundViewModelFactory(repository)
                )
                GaonFundApp(viewModel = viewModel)
            }
        }
    }
}

sealed class Screen(val index: Int, val title: String, val icon: ImageVector) {
    object Dashboard : Screen(0, "डैशबोर्ड", Icons.Default.Dashboard)
    object Donations : Screen(1, "जमा राशि", Icons.Default.VolunteerActivism)
    object Expenses : Screen(2, "गाँव खर्च", Icons.Default.Payments)
    object Projects : Screen(3, "परियोजनाएँ", Icons.Default.Engineering)
    object Notices : Screen(4, "सूचना पट्ट", Icons.Default.Campaign)
    object Complaints : Screen(5, "शिकायतें", Icons.Default.QuestionAnswer)
    object Reports : Screen(6, "रिपोर्ट्स", Icons.Default.Assessment)
    object Admin : Screen(7, "व्यवस्थापक", Icons.Default.AdminPanelSettings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaonFundApp(viewModel: GaonFundViewModel) {
    var currentScreenIndex by remember { mutableIntStateOf(0) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val notifications by viewModel.notifications.collectAsState()
    var showNotificationsSheet by remember { mutableStateOf(false) }

    val isAdmin by GoogleAuthHelper.isAdmin.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // Intercept back button to return to Dashboard tab
    BackHandler(enabled = currentScreenIndex != 0) {
        currentScreenIndex = 0
    }

    val primaryScreens = listOf(
        Screen.Dashboard,
        Screen.Donations,
        Screen.Expenses,
        Screen.Projects
    )

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("gaon_fund_app_root"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "GAON FUND",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "भदेसर गाँव • सार्वजनिक वित्तीय पारदर्शिता",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                actions = {
                    // Notification Bell Icon with Badge
                    IconButton(
                        onClick = { showNotificationsSheet = true },
                        modifier = Modifier.testTag("top_bar_notifications_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (notifications.isNotEmpty()) {
                                    Badge(
                                        containerColor = GoldenTreasury,
                                        contentColor = Color.White
                                    ) {
                                        Text("${notifications.take(9).size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "सूचनाएँ",
                                tint = Color.White
                            )
                        }
                    }

                    // Admin Quick Status Button
                    Surface(
                        color = if (isAdmin) GoldenTreasury else Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clickable { currentScreenIndex = Screen.Admin.index }
                            .padding(end = 8.dp)
                            .testTag("admin_top_bar_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Lock,
                                contentDescription = "Admin",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAdmin) "व्यवस्थापक" else "व्यवस्थापक लॉगिन",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ForestGreenPrimary)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                primaryScreens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreenIndex == screen.index,
                        onClick = { currentScreenIndex = screen.index },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreenPrimary,
                            selectedTextColor = ForestGreenPrimary,
                            indicatorColor = LightGreenContainer
                        )
                    )
                }

                // "More" Tab for Notices, Complaints, Reports, and Admin
                val isMoreSelected = currentScreenIndex >= 4
                NavigationBarItem(
                    selected = isMoreSelected,
                    onClick = { showMoreMenu = true },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "अन्य मेनू") },
                    label = { Text("अन्य", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ForestGreenPrimary,
                        selectedTextColor = ForestGreenPrimary,
                        indicatorColor = LightGreenContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreenIndex) {
                0 -> DashboardScreen(viewModel = viewModel, onNavigateToTab = { currentScreenIndex = it })
                1 -> DonationsScreen(viewModel = viewModel)
                2 -> ExpensesScreen(viewModel = viewModel)
                3 -> ProjectsScreen(viewModel = viewModel)
                4 -> NoticesScreen(viewModel = viewModel)
                5 -> ComplaintsScreen(viewModel = viewModel)
                6 -> ReportsScreen(viewModel = viewModel)
                7 -> AdminScreen(viewModel = viewModel)
            }
        }
    }

    // Modal Bottom Sheet for "More" sections
    if (showMoreMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMoreMenu = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "भदेसर गाँव कोष - अतिरिक्त सेवाएँ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))

                MoreMenuItem(
                    title = "सूचना पट्ट (Notice Board)",
                    subtitle = "ग्राम सभा, मासिक विवरण एवं बैठक सूचनाएँ",
                    icon = Icons.Default.Campaign,
                    isSelected = currentScreenIndex == Screen.Notices.index,
                    onClick = {
                        currentScreenIndex = Screen.Notices.index
                        showMoreMenu = false
                    }
                )

                MoreMenuItem(
                    title = "शिकायत / प्रश्न (Complaints & Queries)",
                    subtitle = "रसीद गुम, प्रविष्टि त्रुटि एवं खर्च स्पष्टीकरण",
                    icon = Icons.Default.QuestionAnswer,
                    isSelected = currentScreenIndex == Screen.Complaints.index,
                    onClick = {
                        currentScreenIndex = Screen.Complaints.index
                        showMoreMenu = false
                    }
                )

                MoreMenuItem(
                    title = "पारदर्शिता रिपोर्ट्स (Financial Reports)",
                    subtitle = "मासिक, वार्षिक एवं फंड-वार लेखा रिपोर्ट",
                    icon = Icons.Default.Assessment,
                    isSelected = currentScreenIndex == Screen.Reports.index,
                    onClick = {
                        currentScreenIndex = Screen.Reports.index
                        showMoreMenu = false
                    }
                )

                MoreMenuItem(
                    title = "व्यवस्थापक कक्ष (Admin Panel)",
                    subtitle = "फंड श्रेणियाँ प्रबंधन, अनुमोदन एवं ऑडिट लॉग्स",
                    icon = Icons.Default.AdminPanelSettings,
                    isSelected = currentScreenIndex == Screen.Admin.index,
                    onClick = {
                        currentScreenIndex = Screen.Admin.index
                        showMoreMenu = false
                    }
                )
            }
        }
    }

    if (showNotificationsSheet) {
        NotificationsBottomSheet(
            notifications = notifications,
            onDismiss = { showNotificationsSheet = false }
        )
    }
}

@Composable
private fun MoreMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) LightGreenContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) ForestGreenPrimary else Color.Gray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else ForestGreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) ForestGreenPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
