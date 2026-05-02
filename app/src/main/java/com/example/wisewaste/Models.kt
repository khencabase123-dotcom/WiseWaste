package com.example.wisewaste

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class User(
    @get:PropertyName("userId")         @set:PropertyName("userId")         var userId: String = "",
    @get:PropertyName("username")       @set:PropertyName("username")       var username: String = "",
    @get:PropertyName("email")          @set:PropertyName("email")          var email: String = "",
    @get:PropertyName("role")           @set:PropertyName("role")           var role: String = "RESIDENT",
    @get:PropertyName("totalPoints")    @set:PropertyName("totalPoints")    var totalPoints: Int = 0,
    @get:PropertyName("createdAt")      @set:PropertyName("createdAt")      var createdAt: Timestamp = Timestamp.now(),
    @get:PropertyName("profileImageUrl") @set:PropertyName("profileImageUrl") var profileImageUrl: String? = null
)

data class WasteReport(
    @get:PropertyName("reportId")    @set:PropertyName("reportId")    var reportId: String = "",
    @get:PropertyName("userId")      @set:PropertyName("userId")      var userId: String = "",
    @get:PropertyName("username")    @set:PropertyName("username")    var username: String = "",
    @get:PropertyName("wasteType")   @set:PropertyName("wasteType")   var wasteType: String = "",
    @get:PropertyName("weightKg")    @set:PropertyName("weightKg")    var weightKg: Double = 0.0,
    @get:PropertyName("reportDate")  @set:PropertyName("reportDate")  var reportDate: Timestamp = Timestamp.now(),
    @get:PropertyName("status")      @set:PropertyName("status")      var status: String = "PENDING",
    @get:PropertyName("imageUrl")    @set:PropertyName("imageUrl")    var imageUrl: String? = null,
    @get:PropertyName("latitude")    @set:PropertyName("latitude")    var latitude: Double? = null,
    @get:PropertyName("longitude")   @set:PropertyName("longitude")   var longitude: Double? = null,
    @get:PropertyName("description") @set:PropertyName("description") var description: String? = null,
    @get:PropertyName("pointsAwarded") @set:PropertyName("pointsAwarded") var pointsAwarded: Int = 0
)

data class WasteCategory(
    val categoryId: String = "",
    val categoryName: String = "",
    val description: String = "",
    val colorCode: String = "#4CAF50",
    val pointsPerKg: Int = 10,
    val icon: String = "♻️"
)

data class EducationalContent(
    val contentId: String = "",
    val title: String = "",
    val contentType: String = "ARTICLE",
    val content: String = "",
    val pointsAwarded: Int = 10,
    val categoryId: String? = null
)

data class Campaign(
    val campaignId: String = "",
    val title: String = "",
    val description: String = "",
    val startDate: Timestamp = Timestamp.now(),
    val endDate: Timestamp = Timestamp.now(),
    val location: String? = null,
    val pointsReward: Int = 50,
    val status: String = "UPCOMING",
    val postedBy: String = ""      // Authority name who posted this
)

data class LeaderboardEntry(
    val userId: String = "",
    val username: String = "",
    val totalPoints: Int = 0,
    val rank: Int = 0
)

data class DashboardMenuItem(
    val title: String,
    val icon: String,
    val color: Color,
    val screen: String
)


data class Announcement(
    val announcementId: String = "",
    val authorityId: String = "",
    val authorityName: String = "",
    val title: String = "",
    val message: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val priority: String = "NORMAL" // NORMAL, HIGH, URGENT
)

data class CollectionSchedule(
    val scheduleId: String = "",
    val area: String = "",
    val wasteType: String = "",
    val dayOfWeek: String = "",
    val time: String = "",
    val notes: String? = null,
    val createdBy: String = ""
)

data class Notification(
    val notificationId: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "GENERAL", // GENERAL, STATUS_UPDATE, ANNOUNCEMENT
    val relatedId: String? = null,
    val isRead: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
)

data class Guideline(
    val guidelineId: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "",
    val createdAt: Timestamp = Timestamp.now()
)