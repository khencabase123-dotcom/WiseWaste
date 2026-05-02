package com.example.wisewaste

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.UUID

// ==================== Authority Dashboard ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorityDashboardScreen(onLogout: () -> Unit) {
    val authManager = remember { AuthManager() }
    val db = remember { FirebaseHelper() }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var totalReports by remember { mutableStateOf(0) }
    var pendingCount by remember { mutableStateOf(0) }
    var selectedMenuItem by remember { mutableStateOf<String?>(null) }
    val userId = authManager.getCurrentUserId()

    // Refresh stats every time user returns from a menu screen (selectedMenuItem becomes null)
    LaunchedEffect(userId, selectedMenuItem) {
        if (userId != null && selectedMenuItem == null) {
            currentUser = db.getUser(userId)
            val all = db.getAllReports()
            totalReports = all.size
            pendingCount = all.count { it.status == "PENDING" }
        }
    }

    // Authority-only menu — no resident features here
    val menuItems = listOf(
        DashboardMenuItem("Post Announcement",    "📢", Color(0xFF1565C0), "announcement"),
        DashboardMenuItem("Review Submissions",   "📋", Color(0xFF2E7D32), "review"),
        DashboardMenuItem("Update Report Status", "🔄", Color(0xFFE65100), "update_status"),
        DashboardMenuItem("Filter Reports",       "🔍", Color(0xFF6A1B9A), "filter"),
        DashboardMenuItem("Collection Schedules", "🗓️", Color(0xFF00695C), "schedules"),
        DashboardMenuItem("Leaderboard",          "🏆", Color(0xFF4527A0), "leaderboard")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("WiseWaste Authority", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        currentUser?.let {
                            Text("${it.username} · Admin Panel", fontSize = 12.sp, color = Color.Gray)
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

                // Authority banner with live stats
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Authority Panel", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                            Text("Manage waste & community", color = Color.White, fontSize = 16.sp,
                                fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                    Text("$totalReports", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Text("Total Reports", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                }
                                Column {
                                    Text("$pendingCount", color = Color(0xFFFFCC02), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Text("Pending", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                }
                            }
                        }
                        Text("🏛️", fontSize = 48.sp)
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(menuItems) { item ->
                        AuthorityMenuCard(item = item, onClick = { selectedMenuItem = item.screen })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    when (selectedMenuItem) {
        "announcement"  -> PostAnnouncementScreen(onClose   = { selectedMenuItem = null })
        "review"        -> ReviewSubmissionsScreen(statusFilter = null, onClose = { selectedMenuItem = null })
        "update_status" -> UpdateReportStatusScreen(onClose = { selectedMenuItem = null })
        "filter"        -> FilterReportsScreen(onClose      = { selectedMenuItem = null })
        "schedules"     -> CollectionSchedulesScreen(onClose = { selectedMenuItem = null })
        "leaderboard"   -> LeaderboardScreen(onClose        = { selectedMenuItem = null })
    }
}

@Composable
fun AuthorityMenuCard(item: DashboardMenuItem, onClick: () -> Unit) {
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

// ==================== Post Announcement Screen ====================

@Composable
fun PostAnnouncementScreen(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseHelper() }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("NORMAL") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Fetch the authority's username so it can be stored with the announcement and campaign
    var authorityName by remember { mutableStateOf("") }
    LaunchedEffect(userId) {
        authorityName = db.getUser(userId)?.username ?: "Authority"
    }

    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f), shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
            ) {
                Text("📢 Post Announcement", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Broadcast a message to all community members", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                // Show who is posting
                if (authorityName.isNotBlank()) {
                    Text("Posting as: $authorityName", fontSize = 12.sp, color = Color(0xFF1565C0))
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = message, onValueChange = { message = it },
                    label = { Text("Message") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), minLines = 4
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Priority", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("NORMAL", "HIGH", "URGENT").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (p) {
                                    "URGENT" -> Color(0xFFD32F2F)
                                    "HIGH"   -> Color(0xFFFF9800)
                                    else     -> Color(0xFF1565C0)
                                },
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (title.isBlank()) { errorMsg = "Title is required"; return@Button }
                            if (message.isBlank()) { errorMsg = "Message is required"; return@Button }
                            scope.launch {
                                isLoading = true; errorMsg = null
                                val announcement = Announcement(
                                    announcementId = UUID.randomUUID().toString(),
                                    authorityId = userId,
                                    authorityName = authorityName,   // ✅ include name
                                    title = title.trim(),
                                    message = message.trim(),
                                    priority = priority
                                )
                                if (db.postAnnouncement(announcement)) showSuccess = true
                                else errorMsg = "Failed to post. Check Firestore permissions."
                                isLoading = false
                            }
                        },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        else Text("Post")
                    }
                }
            }
        }
    }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { showSuccess = false; onClose() },
            title = { Text("Announcement Posted! ✅") },
            text = { Text("All community members have been notified and it now appears in Campaigns.") },
            confirmButton = { TextButton(onClick = { showSuccess = false; onClose() }) { Text("OK") } }
        )
    }
}

// ==================== Review Submissions Screen ====================

@Composable
fun ReviewSubmissionsScreen(statusFilter: String?, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseHelper() }
    var reports by remember { mutableStateOf<List<WasteReport>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedReport by remember { mutableStateOf<WasteReport?>(null) }

    LaunchedEffect(statusFilter) {
        val all = db.getAllReports(statusFilter)
        // Review screen only shows actionable reports — COMPLETED and REJECTED
        // are final states and are only visible in Filter Reports
        reports = all.filter { it.status != "COMPLETED" && it.status != "REJECTED" }
        isLoading = false
    }

    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    if (statusFilter != null) "Reports — $statusFilter" else "All Submissions",
                    fontSize = 22.sp, fontWeight = FontWeight.Bold
                )
                Text("Tap a report to update its status", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (reports.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 64.sp); Text("No reports found", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(reports) { report ->
                            ReportAdminCard(report = report, onClick = { selectedReport = report })
                        }
                    }
                }
            }
        }
    }

    selectedReport?.let { report ->
        UpdateStatusDialog(
            report = report,
            onDismiss = { selectedReport = null },
            onStatusUpdated = { newStatus ->
                scope.launch {
                    val success = db.updateReportStatus(report.reportId, newStatus, report.userId, report.pointsAwarded)
                    if (success) {
                        val all = db.getAllReports(statusFilter)
                        reports = all.filter { it.status != "COMPLETED" && it.status != "REJECTED" }
                    }
                    selectedReport = null
                }
            }
        )
    }
}

// ==================== Update Report Status Screen ====================

@Composable
fun UpdateReportStatusScreen(onClose: () -> Unit) {
    // Reuses ReviewSubmissionsScreen focused on PENDING reports
    ReviewSubmissionsScreen(statusFilter = "PENDING", onClose = onClose)
}

// ==================== Filter Reports Screen ====================

@Composable
fun FilterReportsScreen(onClose: () -> Unit) {
    val db = remember { FirebaseHelper() }
    val scope = rememberCoroutineScope()
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    var reports by remember { mutableStateOf<List<WasteReport>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedReport by remember { mutableStateOf<WasteReport?>(null) }

    val filters = listOf("ALL", "PENDING", "APPROVED", "COMPLETED", "REJECTED")

    // Load all reports immediately on open — no need to press a filter first
    LaunchedEffect(Unit) {
        reports = db.getAllReports(null)
        isLoading = false
    }

    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("🔍 Filter Reports", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("View reports by status", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        val isSelected = selectedFilter == filter || (filter == "ALL" && selectedFilter == null)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newFilter = if (filter == "ALL") null else filter
                                selectedFilter = newFilter
                                scope.launch {
                                    isLoading = true
                                    reports = db.getAllReports(newFilter)
                                    isLoading = false
                                }
                            },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = statusColor(filter),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (reports.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 48.sp)
                            Text("No reports found", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(reports) { report ->
                            // Tapping a report from filter view also opens status update dialog
                            ReportAdminCard(report = report, onClick = { selectedReport = report })
                        }
                    }
                }
            }
        }
    }

    // Allow status update directly from filter view
    selectedReport?.let { report ->
        val scope2 = rememberCoroutineScope()
        UpdateStatusDialog(
            report = report,
            onDismiss = { selectedReport = null },
            onStatusUpdated = { newStatus ->
                scope2.launch {
                    db.updateReportStatus(report.reportId, newStatus, report.userId, report.pointsAwarded)
                    reports = db.getAllReports(selectedFilter)
                    selectedReport = null
                }
            }
        )
    }
}

// ==================== Collection Schedules Screen (Authority — read + create) ====================

@Composable
fun CollectionSchedulesScreen(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseHelper() }
    var schedules by remember { mutableStateOf<List<CollectionSchedule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<CollectionSchedule?>(null) }

    LaunchedEffect(Unit) { schedules = db.getCollectionSchedules(); isLoading = false }

    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🗓️ Collection Schedules", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Manage pickup schedules", fontSize = 13.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text("+ Add", maxLines = 1) }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (schedules.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🗓️", fontSize = 64.sp)
                            Text("No schedules yet", color = Color.Gray)
                            Text("Tap '+ Add' to create one", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(schedules) { s ->
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(1.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(s.area, fontWeight = FontWeight.Bold)
                                            Text(s.dayOfWeek, color = Color(0xFF00695C), fontSize = 13.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { editingSchedule = s },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) { Text("✏️ Edit", fontSize = 12.sp, maxLines = 1) }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("♻️ ${s.wasteType} · 🕐 ${s.time}", fontSize = 13.sp, color = Color.Gray)
                                    if (!s.notes.isNullOrBlank()) Text("📝 ${s.notes}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddScheduleDialog(
            onDismiss = { showAddDialog = false },
            onSaved = { newSchedule ->
                scope.launch {
                    if (db.saveCollectionSchedule(newSchedule)) schedules = db.getCollectionSchedules()
                    showAddDialog = false
                }
            }
        )
    }

    editingSchedule?.let { schedule ->
        AddScheduleDialog(
            existing = schedule,
            onDismiss = { editingSchedule = null },
            onSaved = { updated ->
                scope.launch {
                    if (db.updateCollectionSchedule(updated)) schedules = db.getCollectionSchedules()
                    editingSchedule = null
                }
            }
        )
    }
}

@Composable
fun AddScheduleDialog(
    existing: CollectionSchedule? = null,
    onDismiss: () -> Unit,
    onSaved: (CollectionSchedule) -> Unit
) {
    val isEditing = existing != null
    var area by remember { mutableStateOf(existing?.area ?: "") }
    var wasteType by remember { mutableStateOf(existing?.wasteType ?: "") }
    var dayOfWeek by remember { mutableStateOf(existing?.dayOfWeek ?: "Monday") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    // Parse existing time into components if editing
    val parsedHour = existing?.time?.split(":")?.getOrNull(0)?.trim()?.toIntOrNull() ?: 7
    val parsedMinute = existing?.time?.split(":")?.getOrNull(1)?.trim()?.split(" ")?.getOrNull(0)?.toIntOrNull() ?: 0
    val parsedAmPm = if (existing?.time?.contains("PM") == true) "PM" else "AM"

    var selectedHour by remember { mutableStateOf(parsedHour) }
    var selectedMinute by remember { mutableStateOf(parsedMinute) }
    var selectedAmPm by remember { mutableStateOf(parsedAmPm) }
    var hourExpanded by remember { mutableStateOf(false) }
    var minuteExpanded by remember { mutableStateOf(false) }
    var amPmExpanded by remember { mutableStateOf(false) }

    val hours = (1..12).toList()
    val minutes = (0..59).toList()
    val formattedTime = "%02d:%02d %s".format(selectedHour, selectedMinute, selectedAmPm)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Collection Schedule" else "Add Collection Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = area, onValueChange = { area = it },
                    label = { Text("Area / Barangay") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp), singleLine = true)
                OutlinedTextField(value = wasteType, onValueChange = { wasteType = it },
                    label = { Text("Waste Type") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp), singleLine = true)
                Text("Day of Week", fontSize = 13.sp, color = Color.Gray)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    days.forEach { day ->
                        FilterChip(selected = dayOfWeek == day, onClick = { dayOfWeek = day },
                            label = { Text(day.take(3)) })
                    }
                }

                // Time picker — Hour / Minute / AM-PM dropdowns
                Text("Time", fontSize = 13.sp, color = Color.Gray)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Hour dropdown (1–12, scrollable)
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { hourExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFBDBDBD))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Hr", fontSize = 10.sp, color = Color.Gray)
                                    Text("%02d".format(selectedHour), fontSize = 14.sp)
                                }
                                Text("▾", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                        DropdownMenu(
                            expanded = hourExpanded,
                            onDismissRequest = { hourExpanded = false },
                            modifier = Modifier.height(200.dp)
                        ) {
                            hours.forEach { h ->
                                DropdownMenuItem(
                                    text = { Text("%02d".format(h)) },
                                    onClick = { selectedHour = h; hourExpanded = false }
                                )
                            }
                        }
                    }

                    Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    // Minute dropdown (00–59, scrollable)
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { minuteExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFBDBDBD))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Min", fontSize = 10.sp, color = Color.Gray)
                                    Text("%02d".format(selectedMinute), fontSize = 14.sp)
                                }
                                Text("▾", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                        DropdownMenu(
                            expanded = minuteExpanded,
                            onDismissRequest = { minuteExpanded = false },
                            modifier = Modifier.height(200.dp)
                        ) {
                            minutes.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text("%02d".format(m)) },
                                    onClick = { selectedMinute = m; minuteExpanded = false }
                                )
                            }
                        }
                    }

                    // AM/PM dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { amPmExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFBDBDBD))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedAmPm, fontSize = 14.sp)
                                Text("▾", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                        DropdownMenu(expanded = amPmExpanded, onDismissRequest = { amPmExpanded = false }) {
                            listOf("AM", "PM").forEach { ap ->
                                DropdownMenuItem(
                                    text = { Text(ap) },
                                    onClick = { selectedAmPm = ap; amPmExpanded = false }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp))
                if (errorMsg != null) Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (area.isBlank()) { errorMsg = "Area is required"; return@TextButton }
                if (wasteType.isBlank()) { errorMsg = "Waste type is required"; return@TextButton }
                onSaved(CollectionSchedule(
                    scheduleId = existing?.scheduleId ?: UUID.randomUUID().toString(),
                    area = area.trim(), wasteType = wasteType.trim(),
                    dayOfWeek = dayOfWeek, time = formattedTime,
                    notes = notes.ifBlank { null }, createdBy = userId
                ))
            }) { Text(if (isEditing) "Update" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ==================== Shared Admin Composables ====================

@Composable
fun ReportAdminCard(report: WasteReport, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(report.wasteType, fontWeight = FontWeight.Bold)
                    val displayUser = report.username.ifBlank { report.userId.take(8) + "…" }
                    Text("By: $displayUser", fontSize = 12.sp, color = Color.Gray)
                }
                StatusBadge(report.status)
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

@Composable
fun StatusBadge(status: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = statusColor(status).copy(alpha = 0.15f)) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp, color = statusColor(status), fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun UpdateStatusDialog(report: WasteReport, onDismiss: () -> Unit, onStatusUpdated: (String) -> Unit) {
    val statuses = listOf("PENDING", "APPROVED", "COMPLETED", "REJECTED")
    var selected by remember { mutableStateOf(report.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Report Status") },
        text = {
            Column {
                Text("Waste Type: ${report.wasteType}", fontWeight = FontWeight.Medium)
                Text("Reported by: ${report.username.ifBlank { "Unknown" }}", fontSize = 13.sp, color = Color.Gray)
                Text("Weight: ${report.weightKg} kg", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Select new status:", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                statuses.forEach { status ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selected = status }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == status,
                            onClick = { selected = status },
                            colors = RadioButtonDefaults.colors(selectedColor = statusColor(status))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(status, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(status)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onStatusUpdated(selected) }) { Text("Update") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun statusColor(status: String): Color = when (status) {
    "PENDING"   -> Color(0xFFFF9800)
    "APPROVED"  -> Color(0xFF4CAF50)
    "COMPLETED" -> Color(0xFF2196F3)
    "REJECTED"  -> Color(0xFFD32F2F)
    "HIGH"      -> Color(0xFFFF9800)
    "URGENT"    -> Color(0xFFD32F2F)
    "ALL"       -> Color(0xFF607D8B)
    else        -> Color(0xFF607D8B)
}