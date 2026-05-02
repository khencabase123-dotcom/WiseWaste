package com.example.wisewaste

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

// ==================== Login Screen with Role Selection ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (role: String) -> Unit) {
    val authManager = remember { AuthManager() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRegistering by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("RESIDENT") }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(Color(0xFF4CAF50), Color(0xFF2E7D32)))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(modifier = Modifier.size(100.dp), shape = RoundedCornerShape(50.dp), color = Color.White) {
                Box(contentAlignment = Alignment.Center) { Text("♻️", fontSize = 50.sp) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("WiseWaste", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Waste Segregation Awareness Platform", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Select Account Type", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RoleCard(
                            title = "Resident",
                            icon = "👤",
                            accentColor = Color(0xFF4CAF50),
                            isSelected = selectedRole == "RESIDENT",
                            onClick = { selectedRole = "RESIDENT"; isRegistering = false; errorMessage = null },
                            modifier = Modifier.weight(1f)
                        )
                        RoleCard(
                            title = "Authority",
                            icon = "🏛️",
                            accentColor = Color(0xFF1565C0),
                            isSelected = selectedRole == "AUTHORITY",
                            onClick = { selectedRole = "AUTHORITY"; isRegistering = false; errorMessage = null },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (isRegistering) "Create Account" else "Welcome Back!",
                        fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    when {
                        selectedRole == "AUTHORITY" -> {
                            // Authority always shows login only — no sign-up path
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔒", fontSize = 20.sp)
                                    Text(
                                        "Authority accounts are managed by administrators. Login with your assigned credentials.",
                                        fontSize = 13.sp, color = Color(0xFF1565C0)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        isRegistering && selectedRole == "RESIDENT" -> {
                            Text("Sign up as a Resident", fontSize = 14.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        else -> {
                            Text("Login to continue", fontSize = 14.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Email — trimmed on input to prevent "badly formatted" Firebase errors
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = errorMessage != null
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.width(48.dp)) {
                                Text(if (passwordVisible) "🙈" else "👁️", fontSize = 20.sp)
                            }
                        },
                        isError = errorMessage != null
                    )

                    if (errorMessage != null) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val accentColor = if (selectedRole == "AUTHORITY") Color(0xFF1565C0) else Color(0xFF4CAF50)
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                val result = if (isRegistering && selectedRole == "RESIDENT") {
                                    if (username.isBlank()) Result.failure(Exception("Username is required"))
                                    else authManager.register(email, password, username.trim(), "RESIDENT")
                                } else {
                                    authManager.login(email, password, selectedRole)
                                }
                                if (result.isSuccess) {
                                    onLoginSuccess(if (isRegistering) "RESIDENT" else selectedRole)
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Authentication failed"
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        else Text(
                            when {
                                isRegistering && selectedRole == "RESIDENT" -> "Sign Up"
                                selectedRole == "AUTHORITY" -> "Login as Authority"
                                else -> "Login"
                            },
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sign up / Login toggle — residents only
                    if (selectedRole == "RESIDENT") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isRegistering) "Already have an account? " else "Don't have an account? ",
                                color = Color.Gray, fontSize = 14.sp
                            )
                            TextButton(
                                onClick = {
                                    isRegistering = !isRegistering
                                    errorMessage = null; username = ""; email = ""; password = ""
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (isRegistering) "Login" else "Sign Up", color = Color(0xFF4CAF50), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    icon: String,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(80.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor else Color.White,
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ==================== Resident Dashboard ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResidentDashboardScreen(onLogout: () -> Unit) {
    val authManager = remember { AuthManager() }
    val db = remember { FirebaseHelper() }
    val scope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf<User?>(null) }
    var userPoints by remember { mutableStateOf(0) }
    var reportCount by remember { mutableStateOf(0) }
    var showPointsDialog by remember { mutableStateOf(false) }
    var selectedMenuItem by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    val userId = authManager.getCurrentUserId()

    // Refresh whenever refreshKey changes OR whenever the user closes a sub-screen
    // (selectedMenuItem goes back to null). This ensures points awarded by the
    // authority (COMPLETED status) are reflected immediately when the resident
    // returns to the dashboard — even without a manual pull-to-refresh.
    LaunchedEffect(userId, refreshKey, selectedMenuItem) {
        if (userId != null && selectedMenuItem == null) {
            reportCount = db.getUserReports(userId).size
            currentUser = db.getUser(userId)
            // Read totalPoints directly from Firestore so campaign and education
            // points are included — not just waste report points
            userPoints = currentUser?.totalPoints ?: 0
        }
    }

    // Resident-only menu — authority items are NOT present here
    val menuItems = listOf(
        DashboardMenuItem("Campaigns",           "🌍", Color(0xFFFF9800), "campaigns"),
        DashboardMenuItem("Notifications",       "🔔", Color(0xFFE53935), "notifications"),
        DashboardMenuItem("Guidelines",          "📖", Color(0xFF1976D2), "guidelines"),
        DashboardMenuItem("Collection Schedule", "🗓️", Color(0xFF00897B), "schedule"),
        DashboardMenuItem("Report Waste Issue",  "📝", Color(0xFF4CAF50), "report"),
        DashboardMenuItem("My Report Status",    "📋", Color(0xFF607D8B), "myreports"),
        DashboardMenuItem("Learn & Earn",        "📚", Color(0xFF2196F3), "education"),
        DashboardMenuItem("Leaderboard",         "🏆", Color(0xFF9C27B0), "leaderboard")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("WiseWaste", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        currentUser?.let {
                            Text("Welcome, ${it.username}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                },
                actions = { TextButton(onClick = onLogout) { Text("🚪 Logout", fontSize = 14.sp) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Points banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable { showPointsDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Community Member", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                            Text("$userPoints pts", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            Text("Tap to see how to earn more", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                        }
                        Text("⭐", fontSize = 48.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("Reports", "$reportCount", Color(0xFF2196F3), Modifier.weight(1f))
                    StatCard("Points",  "$userPoints",  Color(0xFF4CAF50), Modifier.weight(1f))
                    StatCard("Role",    "Resident",     Color(0xFF9C27B0), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(menuItems) { item ->
                        MenuCard(item = item, onClick = { selectedMenuItem = item.screen })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    when (selectedMenuItem) {
        "campaigns"     -> CampaignScreen(onClose        = { selectedMenuItem = null })
        "notifications" -> NotificationsScreen(onClose   = { selectedMenuItem = null })
        "guidelines"    -> GuidelinesScreen(onClose      = { selectedMenuItem = null })
        "schedule"      -> ResidentScheduleScreen(onClose = { selectedMenuItem = null })
        "report"        -> ReportScreen(
            onClose = { selectedMenuItem = null },
            onReported = {
                refreshKey++                     // refresh dashboard stats
                selectedMenuItem = null          // tear down ReportScreen first
                scope.launch {
                    delay(50)                    // let Compose recompose without ReportScreen
                    selectedMenuItem = "myreports"
                }
            }
        )
        "myreports"     -> MyReportsScreen(refreshKey = refreshKey, onClose = { refreshKey++; selectedMenuItem = null })
        "education"     -> EducationScreen(onClose = { selectedMenuItem = null }, onPointsEarned = { refreshKey++ })
        "leaderboard"   -> LeaderboardScreen(onClose     = { selectedMenuItem = null })
    }

    if (showPointsDialog) {
        AlertDialog(
            onDismissRequest = { showPointsDialog = false },
            title = { Text("How to Earn Points") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📝 Report a waste issue: +10–25 pts")
                    Text("📚 Complete educational content: +10 pts each")
                    Text("🌍 Join a campaign: +50 pts per campaign")
                    Text("🔝 Higher-ranked waste types earn more pts/kg")
                }
            },
            confirmButton = { TextButton(onClick = { showPointsDialog = false }) { Text("Got it!") } }
        )
    }
}

// ==================== Shared UI Components (used by both dashboards) ====================

@Composable
fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1,
                softWrap = false)
        }
    }
}

@Composable
fun MenuCard(item: DashboardMenuItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(120.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = item.color.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) { Text(item.icon, fontSize = 26.sp) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
        }
    }
}

// ==================== Notifications Screen ====================
@Composable
fun NotificationsScreen(onClose: () -> Unit) {
    val db = remember { FirebaseHelper() }
    val scope = rememberCoroutineScope()
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var deletingId by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(userId, refreshKey) {
        isLoading = true
        if (userId != null) {
            val fetched = db.getUserNotifications(userId)
            val unread = fetched.filter { !it.isRead }
            if (unread.isNotEmpty()) {
                unread.forEach { db.markNotificationRead(it.notificationId) }
            }
            notifications = fetched.map { it.copy(isRead = true) }
        }
        isLoading = false
    }

    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🔔 Notifications", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Announcements and status updates", fontSize = 13.sp, color = Color.Gray)
                    }
                    IconButton(onClick = { refreshKey++ }) {
                        Text("🔄", fontSize = 20.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (notifications.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔕", fontSize = 64.sp)
                            Text("No notifications yet", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications, key = { it.notificationId }) { notif ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        when (notif.type) { "STATUS_UPDATE" -> "🔄"; "ANNOUNCEMENT" -> "📢"; else -> "📬" },
                                        fontSize = 28.sp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(notif.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            if (!notif.isRead) Surface(shape = RoundedCornerShape(50), color = Color(0xFF4CAF50)) {
                                                Text("NEW", fontSize = 7.sp, color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                        Text(notif.message, fontSize = 13.sp, color = Color.Gray)
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                deletingId = notif.notificationId
                                                db.deleteNotification(notif.notificationId)
                                                notifications = notifications.filter {
                                                    it.notificationId != notif.notificationId
                                                }
                                                deletingId = null
                                            }
                                        },
                                        enabled = deletingId != notif.notificationId
                                    ) {
                                        if (deletingId == notif.notificationId) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = Color(0xFFD32F2F),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete notification",
                                                tint = Color(0xFFBDBDBD),
                                                modifier = Modifier.size(20.dp)
                                            )
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

// ==================== Guidelines Screen ====================
@Composable
fun GuidelinesScreen(onClose: () -> Unit) {
    val db = remember { FirebaseHelper() }
    var guidelines by remember { mutableStateOf<List<Guideline>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var expandedId by remember { mutableStateOf<String?>(null) }

    val fallback = listOf(
        Guideline("g1", "Biodegradable Waste", "Includes kitchen scraps, food waste, garden trimmings. Place in GREEN bin. Collected every Monday and Thursday.", "Segregation"),
        Guideline("g2", "Recyclable Waste", "Paper, plastic bottles, glass, metals. Rinse before disposal. Place in BLUE bin. Collected every Wednesday.", "Segregation"),
        Guideline("g3", "Residual / Non-Recyclable", "Items that cannot be recycled or composted. Place in BLACK bag. Collected every Tuesday and Friday.", "Segregation"),
        Guideline("g4", "E-Waste Disposal", "Old electronics, batteries, bulbs. Never mix with other waste. Bring to designated E-Waste drop-off points.", "Special Waste"),
        Guideline("g5", "Hazardous Waste", "Paints, chemicals, expired medicines. Contact local authority for proper disposal guidance.", "Special Waste"),
        Guideline("g6", "Bulky Waste", "Furniture, appliances. Schedule a special collection by contacting the barangay office.", "Special Waste")
    )

    LaunchedEffect(Unit) {
        val loaded = db.getGuidelines()
        guidelines = loaded.ifEmpty { fallback }
        isLoading = false
    }

    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("📖 Waste Guidelines", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Proper waste segregation guide", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(guidelines) { g ->
                            val expanded = expandedId == g.guidelineId
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    expandedId = if (expanded) null else g.guidelineId
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = if (expanded) Color(0xFFE8F5E9) else Color.White)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(g.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text(g.category, fontSize = 11.sp, color = Color(0xFF4CAF50))
                                        }
                                        Text(if (expanded) "▲" else "▼", color = Color.Gray, fontSize = 14.sp)
                                    }
                                    if (expanded) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider()
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(g.content, fontSize = 13.sp, color = Color(0xFF424242))
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

// ==================== Collection Schedule Screen (Resident — read only) ====================
@Composable
fun ResidentScheduleScreen(onClose: () -> Unit) {
    val db = remember { FirebaseHelper() }
    var schedules by remember { mutableStateOf<List<CollectionSchedule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val fallback = listOf(
        CollectionSchedule("s1", "All Areas",          "Biodegradable", "Monday & Thursday", "6:00 AM – 10:00 AM"),
        CollectionSchedule("s2", "All Areas",          "Recyclable",    "Wednesday",         "6:00 AM – 10:00 AM"),
        CollectionSchedule("s3", "All Areas",          "Residual",      "Tuesday & Friday",  "6:00 AM – 10:00 AM"),
        CollectionSchedule("s4", "Designated Drop-off","E-Waste",       "Saturday",          "8:00 AM – 12:00 PM", "Bring to barangay hall")
    )

    LaunchedEffect(Unit) {
        val loaded = db.getCollectionSchedules()
        schedules = loaded.ifEmpty { fallback }
        isLoading = false
    }

    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("🗓️ Collection Schedules", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("When and where waste is collected", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(schedules) { s ->
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                                Row(modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF4CAF50).copy(alpha = 0.12f)) {
                                        Text(s.dayOfWeek.take(3),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("♻️ ${s.wasteType}", fontWeight = FontWeight.SemiBold)
                                        Text("📍 ${s.area}", fontSize = 12.sp, color = Color.Gray)
                                        Text("🕐 ${s.time}", fontSize = 12.sp, color = Color.Gray)
                                        if (!s.notes.isNullOrBlank()) Text("📝 ${s.notes}", fontSize = 11.sp, color = Color(0xFF795548))
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

// ==================== Report Waste Screen ====================
@Composable
fun ReportScreen(onClose: () -> Unit, onReported: (() -> Unit) = onClose) {
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseHelper() }
    var selectedCategory by remember { mutableStateOf<WasteCategory?>(null) }
    var weightInput by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<WasteCategory>>(emptyList()) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val loaded = db.getWasteCategories()
        categories = loaded.ifEmpty { listOf(
            WasteCategory("1", "Biodegradable", pointsPerKg = 10),
            WasteCategory("2", "Recyclable",    pointsPerKg = 15),
            WasteCategory("3", "E-Waste",       pointsPerKg = 20),
            WasteCategory("4", "Residual",      pointsPerKg = 5)
        ) }
    }

    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text("📝 Report Waste Issue", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Help us track and manage waste in your area", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Select Waste Type", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                if (categories.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        categories.forEach { cat ->
                            val selected = selectedCategory?.categoryId == cat.categoryId
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedCategory = cat },
                                colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF4CAF50) else Color.White)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(cat.categoryName, color = if (selected) Color.White else Color.Black,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                    Text("${cat.pointsPerKg} pts/kg", color = if (selected) Color.White else Color.Gray, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val weightError = weightInput.isNotBlank() && (weightInput.toDoubleOrNull() == null || weightInput.toDoubleOrNull()!! <= 0.0)
                OutlinedTextField(
                    value = weightInput, onValueChange = { weightInput = it },
                    label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), singleLine = true, isError = weightError
                )
                if (weightError) Text("Enter a valid positive number", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), minLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))
                if (errorMsg != null) Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                    Button(
                        onClick = {
                            scope.launch {
                                if (selectedCategory == null) { errorMsg = "Please select a waste type"; return@launch }
                                val w = weightInput.toDoubleOrNull()
                                if (w == null || w <= 0.0) { errorMsg = "Please enter a valid weight > 0"; return@launch }
                                isLoading = true; errorMsg = null
                                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                val uname = db.getUser(uid)?.username ?: ""
                                val report = WasteReport(
                                    reportId = UUID.randomUUID().toString(),
                                    userId = uid, username = uname,
                                    wasteType = selectedCategory!!.categoryName,
                                    weightKg = w,
                                    pointsAwarded = (w * selectedCategory!!.pointsPerKg).toInt(),
                                    description = description.ifBlank { null }
                                )
                                if (db.submitReport(report)) showSuccess = true
                                else errorMsg = "Failed to submit. Check Firestore rules."
                                isLoading = false
                            }
                        },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        else Text("Submit Report")
                    }
                }
            }
        }
    }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { showSuccess = false; onReported() },
            title = { Text("Report Submitted! ✅") },
            text = { Text("Your waste report is pending review by the authority.") },
            confirmButton = {
                TextButton(onClick = { showSuccess = false; onReported() }) { Text("OK") }
            }
        )
    }
}

// ==================== My Report Status Screen ====================
@Composable
fun MyReportsScreen(onClose: () -> Unit, refreshKey: Int = 0) {
    val db = remember { FirebaseHelper() }
    var reports by remember { mutableStateOf<List<WasteReport>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(userId, refreshKey) {
        isLoading = true
        if (userId != null) reports = db.getUserReports(userId)
        isLoading = false
    }

    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("📋 My Report Status", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Track your waste reporting history", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (reports.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📝", fontSize = 64.sp)
                            Text("No reports yet", color = Color.Gray)
                            Text("Start reporting waste to earn points!", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(reports) { report ->
                            val statusColor = when (report.status) {
                                "PENDING"   -> Color(0xFFFF9800)
                                "APPROVED"  -> Color(0xFF4CAF50)
                                "COMPLETED" -> Color(0xFF2196F3)
                                "REJECTED"  -> Color(0xFFD32F2F)
                                else        -> Color.Gray
                            }
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(1.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Text(report.wasteType, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.15f)) {
                                            Text(report.status,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("⚖️ ${report.weightKg} kg", fontSize = 12.sp, color = Color.Gray)
                                        Text("⭐ +${report.pointsAwarded} pts", fontSize = 12.sp, color = Color(0xFF4CAF50))
                                    }
                                    if (!report.description.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(report.description ?: "", fontSize = 12.sp, color = Color.Gray, maxLines = 2)
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

// ==================== Quiz data holder ====================
data class QuizQuestion(
    val question: String,
    val choices: List<String>,
    val correctIndex: Int
)

// Maps each content ID to its quiz question
private val quizBank = mapOf(
    "e1" to QuizQuestion(
        "What is the main purpose of waste segregation?",
        listOf(
            "To make trash look organized",
            "To separate waste so each type is handled properly",
            "To reduce the number of garbage collectors",
            "To fill up landfills faster"
        ), correctIndex = 1
    ),
    "e2" to QuizQuestion(
        "Which bin should biodegradable waste go into?",
        listOf("Blue bin", "Black bin", "Green bin", "Red bin"),
        correctIndex = 2
    ),
    "e3" to QuizQuestion(
        "Where should you bring old batteries and broken electronics?",
        listOf(
            "Throw them in the regular trash bin",
            "Burn them in the backyard",
            "Flush them down the drain",
            "Bring them to a designated e-waste drop-off point"
        ), correctIndex = 3
    ),
    "e4" to QuizQuestion(
        "What does the second R in '3Rs' stand for?",
        listOf("Reduce", "Recycle", "Reuse", "Refuse"),
        correctIndex = 2
    ),
    "e5" to QuizQuestion(
        "Which item should NOT be added to a compost bin?",
        listOf("Fruit peels", "Dried leaves", "Meat and dairy", "Coffee grounds"),
        correctIndex = 2
    ),
    "e6" to QuizQuestion(
        "Why is open burning of garbage dangerous?",
        listOf(
            "It takes too long",
            "It produces too much ash",
            "It releases toxic gases harmful to health and environment",
            "It attracts insects"
        ), correctIndex = 2
    ),
    "e7" to QuizQuestion(
        "What should you do with expired medicines and paint?",
        listOf(
            "Pour them down the drain",
            "Throw them in regular bins",
            "Burn them at home",
            "Contact the barangay for hazardous waste collection"
        ), correctIndex = 3
    ),
    "e8" to QuizQuestion(
        "How long can plastics take to decompose?",
        listOf("10–20 years", "50–100 years", "400–1000 years", "1–5 years"),
        correctIndex = 2
    )
)

// ==================== Education Screen ====================
@Composable
fun EducationScreen(onClose: () -> Unit, onPointsEarned: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseHelper() }
    var contents by remember { mutableStateOf<List<EducationalContent>>(emptyList()) }
    var selectedContent by remember { mutableStateOf<EducationalContent?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    // Persisted completion state — loaded from Firestore so it survives across sessions
    var completedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val fallbackContent = listOf(
        EducationalContent("e1", "What is Waste Segregation?", "ARTICLE", pointsAwarded = 5,
            content = "Waste segregation is the process of separating different types of waste at the source. It helps reduce pollution, makes recycling easier, and protects the environment. Segregating waste into biodegradable, recyclable, and residual categories ensures each type is handled and disposed of properly."),
        EducationalContent("e2", "Biodegradable vs Non-Biodegradable", "ARTICLE", pointsAwarded = 5,
            content = "Biodegradable waste (food scraps, leaves, paper) can be broken down naturally by microorganisms. Non-biodegradable waste (plastics, metals, glass) cannot decompose easily and must be recycled or specially disposed of. Always place biodegradable waste in the GREEN bin and recyclables in the BLUE bin."),
        EducationalContent("e3", "How to Properly Dispose of E-Waste", "ARTICLE", pointsAwarded = 5,
            content = "E-waste includes old phones, batteries, chargers, and broken appliances. Never throw e-waste in regular bins — it contains toxic chemicals like lead and mercury that can contaminate soil and water. Bring e-waste to designated drop-off points or barangay collection drives."),
        EducationalContent("e4", "The 3Rs: Reduce, Reuse, Recycle", "ARTICLE", pointsAwarded = 5,
            content = "REDUCE: Buy only what you need and avoid single-use plastics. REUSE: Repurpose containers, bags, and materials instead of throwing them away. RECYCLE: Clean and sort recyclables so they can be processed into new products. Practicing the 3Rs significantly lowers the volume of waste that ends up in landfills."),
        EducationalContent("e5", "Composting at Home", "ARTICLE", pointsAwarded = 5,
            content = "Composting turns kitchen and garden waste into nutrient-rich soil. You can start with a simple compost bin using fruit peels, vegetable scraps, dried leaves, and coffee grounds. Avoid adding meat, dairy, or oily food. Compost is ready in 6–8 weeks and can be used for home gardening."),
        EducationalContent("e6", "Dangers of Open Burning of Waste", "ARTICLE", pointsAwarded = 5,
            content = "Burning garbage releases toxic gases like carbon monoxide, dioxins, and furans which are harmful to human health and the environment. Open burning is illegal under the Philippine Clean Air Act. Instead of burning, report bulk waste to your barangay for proper collection and disposal."),
        EducationalContent("e7", "Proper Disposal of Hazardous Household Waste", "ARTICLE", pointsAwarded = 5,
            content = "Hazardous household waste includes expired medicines, paint, insecticides, and cleaning chemicals. Never pour these down the drain or throw them in regular bins. Contact your local barangay or LGU for scheduled hazardous waste collection events. Proper disposal prevents water and soil contamination."),
        EducationalContent("e8", "Why Plastic Pollution is a Crisis", "ARTICLE", pointsAwarded = 5,
            content = "Plastics take 400–1000 years to decompose. Single-use plastics like bags, straws, and cups are the top contributors to ocean and waterway pollution. Microplastics from broken-down plastic enter our food and water supply. Switching to reusable alternatives and properly disposing of plastics makes a real difference.")
    )

    LaunchedEffect(Unit) {
        val loaded = db.getEducationalContent()
        contents = loaded.ifEmpty { fallbackContent }
        // Load persisted completion state so completed lessons stay marked across sessions
        if (userId != null) {
            completedIds = db.getCompletedContentIds(userId)
        }
        isLoading = false
    }

    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("📚 Learn & Earn", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Read each lesson then answer the quiz to earn points", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (contents.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 64.sp)
                            Text("No content available yet", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(contents) { c ->
                            val done = completedIds.contains(c.contentId)
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = !done) { selectedContent = c },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (done) Color(0xFFE8F5E9) else Color.White
                                ),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Text(if (done) "✅" else "📖", fontSize = 22.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(c.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(
                                                if (done) "Completed" else "Tap to read & quiz",
                                                fontSize = 11.sp,
                                                color = if (done) Color(0xFF4CAF50) else Color.Gray
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (done) Color(0xFF4CAF50).copy(alpha = 0.15f)
                                        else Color(0xFF4CAF50).copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            if (done) "✓ +${c.pointsAwarded} pts" else "+${c.pointsAwarded} pts",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 12.sp,
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Lesson + Quiz dialog ----
    selectedContent?.let { c ->
        val quiz = quizBank[c.contentId]
        var showQuiz by remember { mutableStateOf(false) }
        var selectedChoice by remember { mutableStateOf<Int?>(null) }
        var quizSubmitted by remember { mutableStateOf(false) }
        var isAwarding by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { selectedContent = null; showQuiz = false; selectedChoice = null; quizSubmitted = false }) {
            Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(Color(0xFF4CAF50))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(if (showQuiz) "❓ Quiz Time!" else "📖 ${c.title}",
                                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(if (showQuiz) "Choose the correct answer" else "Read the lesson below",
                                fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)
                    ) {
                        if (!showQuiz) {
                            // ---- Article view ----
                            Text(c.content, fontSize = 14.sp, color = Color(0xFF333333), lineHeight = 22.sp)
                            Spacer(modifier = Modifier.height(20.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFF8E1)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("💡", fontSize = 18.sp)
                                    Text("After reading, take the quiz to earn +${c.pointsAwarded} pts!",
                                        fontSize = 13.sp, color = Color(0xFF795548))
                                }
                            }
                        } else {
                            // ---- Quiz view ----
                            if (quiz != null) {
                                Text(quiz.question, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF212121))
                                Spacer(modifier = Modifier.height(16.dp))
                                quiz.choices.forEachIndexed { index, choice ->
                                    val isSelected = selectedChoice == index
                                    val isCorrect = index == quiz.correctIndex
                                    val bgColor = when {
                                        !quizSubmitted && isSelected -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                        !quizSubmitted -> Color(0xFFF5F5F5)
                                        isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                        isSelected -> Color(0xFFFFCDD2)   // submitted, not correct, was selected
                                        else -> Color(0xFFF5F5F5)
                                    }
                                    val borderColor = when {
                                        !quizSubmitted && isSelected -> Color(0xFF4CAF50)
                                        !quizSubmitted -> Color(0xFFE0E0E0)
                                        isCorrect -> Color(0xFF4CAF50)
                                        isSelected -> Color(0xFFD32F2F)   // submitted, not correct, was selected
                                        else -> Color(0xFFE0E0E0)
                                    }
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                                            .clickable(enabled = !quizSubmitted) { selectedChoice = index },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = bgColor),
                                        border = BorderStroke(1.5.dp, borderColor)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = borderColor.copy(alpha = 0.15f),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        when {
                                                            quizSubmitted && isCorrect -> "✓"
                                                            quizSubmitted && isSelected -> "✗"
                                                            else -> listOf("A","B","C","D")[index]
                                                        },
                                                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                                        color = borderColor
                                                    )
                                                }
                                            }
                                            Text(choice, fontSize = 14.sp, color = Color(0xFF212121))
                                        }
                                    }
                                }

                                if (quizSubmitted) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    val correct = selectedChoice == quiz.correctIndex
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (correct) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                    ) {
                                        Row(modifier = Modifier.padding(14.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            Text(if (correct) "🎉" else "❌", fontSize = 24.sp)
                                            Column {
                                                Text(
                                                    if (correct) "Correct! +${c.pointsAwarded} pts earned!" else "Wrong answer!",
                                                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                                    color = if (correct) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                                )
                                                if (!correct) Text(
                                                    "The correct answer is: ${quiz.choices[quiz.correctIndex]}",
                                                    fontSize = 12.sp, color = Color(0xFF424242)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom action buttons
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedContent = null; showQuiz = false; selectedChoice = null; quizSubmitted = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("Close") }

                        if (!showQuiz) {
                            Button(
                                onClick = { showQuiz = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) { Text("Take Quiz →") }
                        } else if (!quizSubmitted) {
                            Button(
                                onClick = { if (selectedChoice != null) quizSubmitted = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                enabled = selectedChoice != null
                            ) { Text("Submit Answer") }
                        } else {
                            // After submitting — award points only if correct
                            val correct = quiz != null && selectedChoice == quiz.correctIndex
                            Button(
                                onClick = {
                                    if (correct && !completedIds.contains(c.contentId)) {
                                        scope.launch {
                                            isAwarding = true
                                            if (userId != null) {
                                                // markContentCompleted persists to Firestore AND awards points atomically
                                                db.markContentCompleted(userId, c.contentId, c.pointsAwarded)
                                            }
                                            completedIds = completedIds + c.contentId
                                            isAwarding = false
                                            onPointsEarned()           // refresh dashboard banner
                                            selectedContent = null
                                            showQuiz = false
                                            selectedChoice = null
                                            quizSubmitted = false
                                        }
                                    } else {
                                        // Wrong — let them retry
                                        selectedChoice = null
                                        quizSubmitted = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (correct) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                ),
                                enabled = !isAwarding
                            ) {
                                if (isAwarding) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                else Text(if (correct) "Claim Points ⭐" else "Try Again")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== Campaigns Screen ====================
@Composable
fun CampaignScreen(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseHelper() }
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var campaigns by remember { mutableStateOf<List<Campaign>>(emptyList()) }
    var joinedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var joiningId by remember { mutableStateOf<String?>(null) }
    var selectedCampaign by remember { mutableStateOf<Campaign?>(null) }

    LaunchedEffect(Unit) {
        campaigns = db.getCampaigns()
        // Load which campaigns this user has already joined
        val joined = mutableSetOf<String>()
        campaigns.forEach { camp ->
            if (db.hasJoinedCampaign(camp.campaignId, userId)) joined.add(camp.campaignId)
        }
        joinedIds = joined
        isLoading = false
    }

    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("🌍 Environmental Campaigns", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Join campaigns to make a difference", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (campaigns.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 64.sp)
                            Text("No campaigns available", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(campaigns, key = { it.campaignId }) { camp ->
                            val isAnnouncement = camp.pointsReward == 0 && camp.postedBy.isNotBlank()
                            val hasJoined = joinedIds.contains(camp.campaignId)
                            val isCompleted = camp.status == "COMPLETED"
                            val isJoining = joiningId == camp.campaignId

                            val statusColor = when (camp.status) {
                                "ACTIVE"    -> Color(0xFF4CAF50)
                                "COMPLETED" -> Color(0xFF2196F3)
                                "UPCOMING"  -> Color(0xFFFF9800)
                                else        -> Color(0xFF607D8B)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCampaign = camp },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (hasJoined) Color(0xFFE8F5E9) else Color.White
                                ),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Title + status badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            camp.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = statusColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                camp.status,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                fontSize = 10.sp,
                                                color = statusColor,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(camp.description, fontSize = 13.sp, color = Color.Gray, maxLines = 2)

                                    if (!camp.location.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("📍 ${camp.location}", fontSize = 12.sp, color = Color(0xFF1565C0))
                                    }

                                    if (camp.postedBy.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("🏛️ ${camp.postedBy}", fontSize = 11.sp, color = Color.Gray)
                                    }

                                    // Dates — locale read observably so Compose recomposes on locale change
                                    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                                    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", locale)
                                    val startStr = sdf.format(camp.startDate.toDate())
                                    val endStr   = sdf.format(camp.endDate.toDate())
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("📅 $startStr → $endStr", fontSize = 11.sp, color = Color.Gray)

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = Color(0xFFF0F0F0))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Footer: points chip + join status
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!isAnnouncement && camp.pointsReward > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFFF9800).copy(alpha = 0.1f)
                                            ) {
                                                Text(
                                                    "⭐ +${camp.pointsReward} pts",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFFF9800),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF1565C0).copy(alpha = 0.1f)
                                            ) {
                                                Text(
                                                    "📢 Announcement",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF1565C0),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        when {
                                            isAnnouncement -> {}
                                            isCompleted && hasJoined -> Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF2196F3).copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    "✓ Points Awarded",
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF2196F3),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            hasJoined -> Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF4CAF50).copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    "✓ Joined",
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF4CAF50),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            isCompleted -> Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.Gray.copy(alpha = 0.1f)
                                            ) {
                                                Text(
                                                    "Closed",
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            else -> Button(
                                                onClick = {
                                                    scope.launch {
                                                        joiningId = camp.campaignId
                                                        if (db.joinCampaign(camp.campaignId, userId)) {
                                                            joinedIds = joinedIds + camp.campaignId
                                                        }
                                                        joiningId = null
                                                    }
                                                },
                                                enabled = !isJoining,
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF4CAF50)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                if (isJoining) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(14.dp),
                                                        color = Color.White,
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Text("Join", fontSize = 12.sp)
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
    // Campaign detail dialog
    selectedCampaign?.let { camp ->
        val hasJoined = joinedIds.contains(camp.campaignId)
        val isCompleted = camp.status == "COMPLETED"
        val isJoining = joiningId == camp.campaignId
        val isAnnouncement = camp.pointsReward == 0 && camp.postedBy.isNotBlank()
        val statusColor = when (camp.status) {
            "ACTIVE"    -> Color(0xFF4CAF50)
            "COMPLETED" -> Color(0xFF2196F3)
            "UPCOMING"  -> Color(0xFFFF9800)
            else        -> Color(0xFF607D8B)
        }
        val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", locale)

        Dialog(onDismissRequest = { selectedCampaign = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            camp.status,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(camp.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(camp.description, fontSize = 14.sp, color = Color(0xFF444444), lineHeight = 20.sp)

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (!camp.location.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📍", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(camp.location, fontSize = 13.sp, color = Color(0xFF1565C0))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📅", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${sdf.format(camp.startDate.toDate())} → ${sdf.format(camp.endDate.toDate())}",
                            fontSize = 13.sp, color = Color.Gray
                        )
                    }

                    if (!isAnnouncement && camp.pointsReward > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⭐", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+${camp.pointsReward} points awarded on completion",
                                fontSize = 13.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Medium)
                        }
                    }

                    if (camp.postedBy.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🏛️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Posted by ${camp.postedBy}", fontSize = 13.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons
                    if (!isAnnouncement) {
                        when {
                            isCompleted && hasJoined -> Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF2196F3).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    "✓ Points Awarded — Thank you for participating!",
                                    modifier = Modifier.padding(16.dp),
                                    fontSize = 14.sp,
                                    color = Color(0xFF2196F3),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            hasJoined -> Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF4CAF50).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    "✓ You've joined this campaign",
                                    modifier = Modifier.padding(16.dp),
                                    fontSize = 14.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            isCompleted -> Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Gray.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    "This campaign has ended",
                                    modifier = Modifier.padding(16.dp),
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                            else -> Button(
                                onClick = {
                                    scope.launch {
                                        joiningId = camp.campaignId
                                        if (db.joinCampaign(camp.campaignId, userId)) {
                                            joinedIds = joinedIds + camp.campaignId
                                        }
                                        joiningId = null
                                    }
                                },
                                enabled = !isJoining,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                if (isJoining) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Join Campaign", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedButton(
                        onClick = { selectedCampaign = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Close") }
                }
            }
        }
    }
}
@Composable
fun LeaderboardScreen(onClose: () -> Unit) {
    val db = remember { FirebaseHelper() }
    var leaderboard by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) { leaderboard = db.getLeaderboard(); isLoading = false }

    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("🏆 Leaderboard", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Top environmental champions", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(leaderboard) { entry ->
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = when (entry.rank) {
                                    1 -> Color(0xFFFFD700).copy(alpha = 0.12f)
                                    2 -> Color(0xFFC0C0C0).copy(alpha = 0.12f)
                                    3 -> Color(0xFFCD7F32).copy(alpha = 0.12f)
                                    else -> Color.White
                                })) {
                                Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            when (entry.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#${entry.rank}" },
                                            fontSize = if (entry.rank <= 3) 22.sp else 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (entry.rank) {
                                                1 -> Color(0xFFFFD700); 2 -> Color(0xFFC0C0C0)
                                                3 -> Color(0xFFCD7F32); else -> Color.Gray
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(entry.username, fontWeight = FontWeight.Medium)
                                    }
                                    Text("${entry.totalPoints} pts", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}