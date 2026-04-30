package com.example.wisewaste

import com.google.firebase.Timestamp

// ─── User ───────────────────────────────────────────────────────────────────

enum class UserRole { RESIDENT, ADMIN, AUTHORITY, COMMUNITY_LEADER }

data class User(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val role: UserRole = UserRole.RESIDENT,
    val totalPoints: Int = 0,
    val profileImageUrl: String = "",
    val barangay: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

// ─── Waste Category ─────────────────────────────────────────────────────────

enum class WasteType {
    BIODEGRADABLE,
    NON_BIODEGRADABLE,
    RECYCLABLE,
    E_WASTE,
    HAZARDOUS
}

data class WasteCategory(
    val categoryId: String = "",
    val categoryName: String = "",
    val wasteType: WasteType = WasteType.RECYCLABLE,
    val description: String = "",
    val colorHex: String = "#4CAF50",
    val iconName: String = "recycling"
)

// ─── Waste Item (educational) ────────────────────────────────────────────────

data class WasteItem(
    val itemId: String = "",
    val categoryId: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val disposalTip: String = ""
)

// ─── Waste Report ────────────────────────────────────────────────────────────

enum class ReportStatus { PENDING, UNDER_REVIEW, RESOLVED, REJECTED }

data class WasteReport(
    val reportId: String = "",
    val userId: String = "",
    val username: String = "",
    val wasteType: WasteType = WasteType.NON_BIODEGRADABLE,
    val description: String = "",
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val status: ReportStatus = ReportStatus.PENDING,
    val weightKg: Double = 0.0,
    val assignedTo: String = "",
    val adminNotes: String = "",
    val reportDate: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

// ─── Educational Content ─────────────────────────────────────────────────────

enum class ContentType { ARTICLE, VIDEO, INFOGRAPHIC }

data class EducationalContent(
    val contentId: String = "",
    val title: String = "",
    val contentType: ContentType = ContentType.ARTICLE,
    val body: String = "",
    val bodyUrl: String = "",
    val thumbnailUrl: String = "",
    val pointsAwarded: Int = 0,
    val categoryId: String = "",
    val publishedAt: Timestamp = Timestamp.now()
)

// ─── Campaign ────────────────────────────────────────────────────────────────

enum class CampaignStatus { UPCOMING, ACTIVE, COMPLETED }

data class Campaign(
    val campaignId: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val location: String = "",
    val startDate: Timestamp = Timestamp.now(),
    val endDate: Timestamp = Timestamp.now(),
    val status: CampaignStatus = CampaignStatus.UPCOMING,
    val createdBy: String = "",
    val participantCount: Int = 0,
    val maxParticipants: Int = 50
)

// ─── Redemption ──────────────────────────────────────────────────────────────

enum class RedemptionStatus { PENDING, COMPLETED, CANCELLED }

data class Redemption(
    val redemptionId: String = "",
    val userId: String = "",
    val rewardId: String = "",
    val rewardName: String = "",
    val pointsCost: Int = 0,
    val status: RedemptionStatus = RedemptionStatus.PENDING,
    val redeemedAt: Timestamp = Timestamp.now()
)

// ─── Reward ──────────────────────────────────────────────────────────────────

data class Reward(
    val rewardId: String = "",
    val name: String = "",
    val description: String = "",
    val pointsCost: Int = 0,
    val imageUrl: String = "",
    val stock: Int = 0,
    val isAvailable: Boolean = true
)

// ─── Notification ────────────────────────────────────────────────────────────

data class AppNotification(
    val notificationId: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "GENERAL",
    val isRead: Boolean = false,
    val relatedId: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

// ─── Leaderboard Entry ───────────────────────────────────────────────────────

data class LeaderboardEntry(
    val userId: String = "",
    val username: String = "",
    val profileImageUrl: String = "",
    val totalPoints: Int = 0,
    val totalReports: Int = 0,
    val barangay: String = "",
    val rank: Int = 0
)