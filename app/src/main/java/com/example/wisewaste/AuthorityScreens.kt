package com.example.wisewaste

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
import androidx.compose.material.icons.filled.Edit
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
        DashboardMenuItem("Review Submissions",   "📋", Color(0xFF2E7D32), "review"),
        DashboardMenuItem("Update Report Status", "🔄", Color(0xFFE65100), "update_status"),
        DashboardMenuItem("Filter Reports",       "🔍", Color(0xFF6A1B9A), "filter"),
        DashboardMenuItem("Collection Schedules", "🗓️", Color(0xFF00695C), "schedules"),
        DashboardMenuItem("Campaigns",            "🎯", Color(0xFFC62828), "campaigns"),
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
        "review"        -> ReviewSubmissionsScreen(statusFilter = null, onClose = { selectedMenuItem = null })
        "update_status" -> UpdateReportStatusScreen(onClose = { selectedMenuItem = null })
        "filter"        -> FilterReportsScreen(onClose      = { selectedMenuItem = null })
        "schedules"     -> CollectionSchedulesScreen(onClose = { selectedMenuItem = null })
        "campaigns"     -> AuthorityCampaignScreen(onClose  = { selectedMenuItem = null })
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

// ==================== Review Submissions Screen ====================

@Composable
fun ReviewSubmissionsScreen(statusFilter: String?, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseHelper() }
    var reports by remember { mutableStateOf<List<WasteReport>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedReport by remember { mutableStateOf<WasteReport?>(null) }

    LaunchedEffect(statusFilter) {
        reports = db.getAllReports(statusFilter)
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
                    if (success) reports = db.getAllReports(statusFilter)
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
                    Column {
                        Text("🗓️ Collection Schedules", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Manage pickup schedules", fontSize = 13.sp, color = Color.Gray)
                    }
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.widthIn(min = 80.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) { Text("+ Add", fontSize = 13.sp, maxLines = 1) }
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
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) { Text("✏️ Edit", fontSize = 12.sp) }
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
    var time by remember { mutableStateOf(existing?.time ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

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
                OutlinedTextField(value = time, onValueChange = { time = it },
                    label = { Text("Time (e.g. 7:00 AM)") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp), singleLine = true)
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
                if (time.isBlank()) { errorMsg = "Time is required"; return@TextButton }
                onSaved(CollectionSchedule(
                    scheduleId = existing?.scheduleId ?: UUID.randomUUID().toString(),
                    area = area.trim(), wasteType = wasteType.trim(),
                    dayOfWeek = dayOfWeek, time = time.trim(),
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

// ==================== Authority Campaign Screen ====================

@Composable
fun AuthorityCampaignScreen(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseHelper() }
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var authorityName by remember { mutableStateOf("") }
    var campaigns by remember { mutableStateOf<List<Campaign>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCampaign by remember { mutableStateOf<Campaign?>(null) }
    var deletingId by remember { mutableStateOf<String?>(null) }
    var confirmDeleteCampaign by remember { mutableStateOf<Campaign?>(null) }
    var viewingParticipantsCampaign by remember { mutableStateOf<Campaign?>(null) }

    LaunchedEffect(userId) {
        authorityName = db.getUser(userId)?.username ?: "Authority"
        campaigns = db.getCampaigns()
        isLoading = false
    }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🎯 Campaigns", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Manage community campaigns", fontSize = 13.sp, color = Color.Gray)
                    }
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) { Text("+ Add", fontSize = 13.sp) }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFC62828))
                    }
                } else if (campaigns.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎯", fontSize = 64.sp)
                            Text("No campaigns yet", color = Color.Gray)
                            Text("Tap + Add to create one", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(campaigns, key = { it.campaignId }) { campaign ->
                            AuthorityCampaignCard(
                                campaign = campaign,
                                isDeleting = deletingId == campaign.campaignId,
                                onEdit = { editingCampaign = campaign },
                                onDelete = { confirmDeleteCampaign = campaign },
                                onViewParticipants = { viewingParticipantsCampaign = campaign },
                                onMarkCompleted = {
                                    if (campaign.status != "COMPLETED") {
                                        scope.launch {
                                            val pts = if (campaign.pointsReward > 0) campaign.pointsReward else 50
                                            val success = db.completeCampaignAndAwardPoints(campaign, pts)
                                            if (success) {
                                                campaigns = campaigns.map {
                                                    if (it.campaignId == campaign.campaignId)
                                                        it.copy(status = "COMPLETED") else it
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- Add Campaign dialog ----
    if (showAddDialog) {
        CampaignFormDialog(
            title = "Add Campaign",
            initial = null,
            authorityName = authorityName,
            onDismiss = { showAddDialog = false },
            onSave = { newCampaign ->
                scope.launch {
                    if (db.saveCampaign(newCampaign)) campaigns = db.getCampaigns()
                    showAddDialog = false
                }
            }
        )
    }

    // ---- Edit Campaign dialog ----
    editingCampaign?.let { camp ->
        CampaignFormDialog(
            title = "Edit Campaign",
            initial = camp,
            authorityName = authorityName,
            onDismiss = { editingCampaign = null },
            onSave = { updated ->
                scope.launch {
                    if (db.updateCampaign(updated)) {
                        campaigns = campaigns.map { if (it.campaignId == updated.campaignId) updated else it }
                    }
                    editingCampaign = null
                }
            }
        )
    }

    // ---- Confirm Delete dialog ----
    confirmDeleteCampaign?.let { camp ->
        AlertDialog(
            onDismissRequest = { confirmDeleteCampaign = null },
            title = { Text("Delete Campaign") },
            text = { Text("Are you sure you want to delete \"${camp.title}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        deletingId = camp.campaignId
                        if (db.deleteCampaign(camp.campaignId)) {
                            campaigns = campaigns.filter { it.campaignId != camp.campaignId }
                        }
                        deletingId = null
                        confirmDeleteCampaign = null
                    }
                }) { Text("Delete", color = Color(0xFFD32F2F)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteCampaign = null }) { Text("Cancel") }
            }
        )
    }

    // ---- View Participants dialog ----
    viewingParticipantsCampaign?.let { camp ->
        var participants by remember { mutableStateOf<List<User>>(emptyList()) }
        var loadingParticipants by remember { mutableStateOf(true) }

        LaunchedEffect(camp.campaignId) {
            val ids = db.getCampaignParticipants(camp.campaignId)
            participants = ids.mapNotNull { db.getUser(it) }
            loadingParticipants = false
        }

        AlertDialog(
            onDismissRequest = { viewingParticipantsCampaign = null },
            title = {
                Column {
                    Text("👥 Participants", fontWeight = FontWeight.Bold)
                    Text(camp.title, fontSize = 13.sp, color = Color.Gray)
                }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 320.dp)) {
                    if (loadingParticipants) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (participants.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("👤", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No one has joined yet", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                Text(
                                    "${participants.size} joined · ${camp.pointsReward.takeIf { it > 0 } ?: 50} pts each on completion",
                                    fontSize = 12.sp, color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            items(participants) { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                user.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(user.username, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Text(user.email, fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingParticipantsCampaign = null }) { Text("Close") }
            }
        )
    }
}

@Composable
fun AuthorityCampaignCard(
    campaign: Campaign,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewParticipants: () -> Unit,
    onMarkCompleted: () -> Unit
) {
    val statusColor = when (campaign.status) {
        "ACTIVE"    -> Color(0xFF4CAF50)
        "COMPLETED" -> Color(0xFF2196F3)
        "UPCOMING"  -> Color(0xFFFF9800)
        else        -> Color(0xFF607D8B)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Title row + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    campaign.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        campaign.status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(campaign.description, fontSize = 13.sp, color = Color.Gray, maxLines = 2)

            if (!campaign.location.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("📍 ${campaign.location}", fontSize = 12.sp, color = Color(0xFF1565C0))
            }

            if (campaign.pointsReward > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("⭐ +${campaign.pointsReward} pts reward", fontSize = 12.sp, color = Color(0xFF4CAF50))
            }

            if (campaign.postedBy.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("Posted by: ${campaign.postedBy}", fontSize = 11.sp, color = Color.LightGray)
            }

            // Dates — locale read observably so Compose recomposes on locale change
            val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", locale)
            val startStr = sdf.format(campaign.startDate.toDate())
            val endStr   = sdf.format(campaign.endDate.toDate())
            Spacer(modifier = Modifier.height(4.dp))
            Text("📅 $startStr → $endStr", fontSize = 11.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(8.dp))

            // Action row — Edit | Delete | Participants | Mark Completed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit button
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, Color(0xFF1565C0))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit",
                        tint = Color(0xFF1565C0), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Edit", fontSize = 11.sp, color = Color(0xFF1565C0))
                }

                // Delete button
                OutlinedButton(
                    onClick = onDelete,
                    enabled = !isDeleting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, Color(0xFFD32F2F))
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp),
                            color = Color(0xFFD32F2F), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Delete, contentDescription = "Delete",
                            tint = Color(0xFFD32F2F), modifier = Modifier.size(14.dp))
                    }
                }

                // Participants button
                OutlinedButton(
                    onClick = onViewParticipants,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, Color(0xFF00695C))
                ) {
                    Text("👥", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Joined", fontSize = 11.sp, color = Color(0xFF00695C))
                }

                // Mark Completed button (hidden if already completed)
                if (campaign.status != "COMPLETED") {
                    Button(
                        onClick = onMarkCompleted,
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Text("✓ Done", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignFormDialog(
    title: String,
    initial: Campaign?,
    authorityName: String,
    onDismiss: () -> Unit,
    onSave: (Campaign) -> Unit
) {
    var campaignTitle by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var pointsReward by remember { mutableStateOf(
        if ((initial?.pointsReward ?: 50) > 0) (initial?.pointsReward ?: 50).toString() else "50"
    ) }
    var status by remember { mutableStateOf(initial?.status ?: "UPCOMING") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Date picker state — initialise from existing campaign or today
    val todayMillis = System.currentTimeMillis()
    val initialStartMillis = initial?.startDate?.toDate()?.time ?: todayMillis
    val initialEndMillis = initial?.endDate?.toDate()?.time ?: todayMillis

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val startPickerState = rememberDatePickerState(initialSelectedDateMillis = initialStartMillis)
    val endPickerState   = rememberDatePickerState(initialSelectedDateMillis = initialEndMillis)

    // Formatted display strings — locale read observably so Compose recomposes on locale change
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    fun millisToDisplay(millis: Long?): String {
        if (millis == null) return "Not set"
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", locale)
        return sdf.format(java.util.Date(millis))
    }

    val statuses = listOf("UPCOMING", "ACTIVE", "COMPLETED")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Posted as: $authorityName", fontSize = 12.sp, color = Color(0xFFC62828))
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = campaignTitle, onValueChange = { campaignTitle = it },
                    label = { Text("Campaign Title *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), minLines = 3
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = location, onValueChange = { location = it },
                    label = { Text("Location (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pointsReward, onValueChange = { pointsReward = it },
                    label = { Text("Points Reward") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), singleLine = true
                )
                Spacer(modifier = Modifier.height(14.dp))

                // ---- Date Pickers ----
                Text("Campaign Dates", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                // Start Date
                OutlinedButton(
                    onClick = { showStartPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "📅 Start: ${millisToDisplay(startPickerState.selectedDateMillis)}",
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // End Date
                OutlinedButton(
                    onClick = { showEndPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "📅 End: ${millisToDisplay(endPickerState.selectedDateMillis)}",
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))

                // ---- Status ----
                Text("Status", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    statuses.forEach { s ->
                        val sColor = when (s) {
                            "ACTIVE"    -> Color(0xFF4CAF50)
                            "COMPLETED" -> Color(0xFF2196F3)
                            else        -> Color(0xFFFF9800)
                        }
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = sColor,
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
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Cancel") }

                    Button(
                        onClick = {
                            if (campaignTitle.isBlank()) { errorMsg = "Title is required"; return@Button }
                            if (description.isBlank()) { errorMsg = "Description is required"; return@Button }
                            val pts = pointsReward.trim().let {
                                if (it.isBlank()) 50 else it.toIntOrNull() ?: -1
                            }
                            if (pts < 0) { errorMsg = "Points must be a valid number"; return@Button }
                            val startTs = com.google.firebase.Timestamp(
                                java.util.Date(startPickerState.selectedDateMillis ?: todayMillis)
                            )
                            val endTs = com.google.firebase.Timestamp(
                                java.util.Date(endPickerState.selectedDateMillis ?: todayMillis)
                            )
                            isSaving = true
                            val campaign = Campaign(
                                campaignId   = initial?.campaignId ?: UUID.randomUUID().toString(),
                                title        = campaignTitle.trim(),
                                description  = description.trim(),
                                location     = location.trim().ifBlank { null },
                                pointsReward = pts,
                                status       = status,
                                postedBy     = authorityName,
                                startDate    = startTs,
                                endDate      = endTs
                            )
                            onSave(campaign)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        enabled = !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        else Text("Save")
                    }
                }
            }
        }
    }

    // ---- Start Date Picker Dialog ----
    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = startPickerState)
        }
    }

    // ---- End Date Picker Dialog ----
    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = endPickerState)
        }
    }
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