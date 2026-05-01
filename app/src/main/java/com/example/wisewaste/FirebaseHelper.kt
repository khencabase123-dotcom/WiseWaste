package com.example.wisewaste

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirebaseHelper {
    private val db = FirebaseFirestore.getInstance()
    private val tag = "FirebaseHelper"

    // ==================== User ====================

    suspend fun getUser(userId: String): User? {
        return try {
            val document = db.collection("users").document(userId).get().await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e(tag, "getUser failed for userId=$userId", e)
            null
        }
    }

    suspend fun updateUserPoints(userId: String, newPoints: Int): Boolean {
        return try {
            db.collection("users").document(userId).update("totalPoints", newPoints).await()
            true
        } catch (e: Exception) {
            Log.e(tag, "updateUserPoints failed for userId=$userId", e)
            false
        }
    }

    /** Atomically adds [delta] points — never overwrites, safe against race conditions */
    suspend fun incrementUserPoints(userId: String, delta: Int): Boolean {
        return try {
            db.collection("users").document(userId)
                .update("totalPoints", FieldValue.increment(delta.toLong()))
                .await()
            true
        } catch (e: Exception) {
            Log.e(tag, "incrementUserPoints failed for userId=$userId", e)
            false
        }
    }

    /**
     * Reconcile points for existing COMPLETED reports that were marked complete
     * before the points-award fix was deployed.
     *
     * Logic:
     *  - Fetch all of this user's COMPLETED reports from Firestore.
     *  - Sum their pointsAwarded values — this is the minimum the user should have.
     *  - If the stored totalPoints is LESS than that sum, overwrite with the sum
     *    so no completed report goes un-credited.
     *  - If totalPoints is already equal or higher (e.g. from quizzes/campaigns
     *    that were correctly awarded), leave it untouched.
     *
     * Safe to call on every dashboard load — it is a no-op once points are correct.
     */
    suspend fun reconcileCompletedPoints(userId: String): Int {
        return try {
            // Read all COMPLETED reports for this user directly (uses existing index)
            val completedDocs = db.collection("wasteReports")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "COMPLETED")
                .get()
                .await()
                .documents

            val completedPointsSum = completedDocs.sumOf { doc ->
                (doc.getLong("pointsAwarded") ?: doc.getLong("points_awarded") ?: 0L).toInt()
            }

            Log.d(tag, "reconcileCompletedPoints: userId=$userId completedSum=$completedPointsSum")

            if (completedPointsSum <= 0) return@reconcileCompletedPoints completedPointsSum

            // Read the user's current totalPoints
            val userDoc = db.collection("users").document(userId).get().await()
            val currentPoints = (userDoc.getLong("totalPoints") ?: 0L).toInt()

            if (currentPoints < completedPointsSum) {
                // Points are under-credited — set to the correct minimum
                db.collection("users").document(userId)
                    .update("totalPoints", completedPointsSum)
                    .await()
                Log.d(tag, "reconcileCompletedPoints: corrected $currentPoints -> $completedPointsSum for $userId")
                completedPointsSum
            } else {
                // Already correct (or higher from other sources) — leave untouched
                currentPoints
            }
        } catch (e: Exception) {
            Log.e(tag, "reconcileCompletedPoints failed for userId=$userId", e)
            // Return current stored value as fallback so the UI still shows something
            try {
                (db.collection("users").document(userId).get().await()
                    .getLong("totalPoints") ?: 0L).toInt()
            } catch (_: Exception) { 0 }
        }
    }

    // ==================== Waste Reports ====================

    suspend fun submitReport(report: WasteReport): Boolean {
        return try {
            db.collection("wasteReports").document(report.reportId).set(report).await()
            Log.d(tag, "submitReport success: ${report.reportId}")
            true
        } catch (e: Exception) {
            Log.e(tag, "submitReport failed: ${e.message}", e)
            false
        }
    }

    /**
     * Resident: fetch only this user's reports.
     * Uses the existing composite index: userId (ASC) + reportDate (DESC).
     */
    suspend fun getUserReports(userId: String): List<WasteReport> {
        return try {
            db.collection("wasteReports")
                .whereEqualTo("userId", userId)
                .orderBy("reportDate", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(WasteReport::class.java) }
        } catch (e: Exception) {
            Log.e(tag, "getUserReports failed for userId=$userId", e)
            emptyList()
        }
    }

    /**
     * Authority: fetch ALL reports across all users.
     *
     * No .orderBy() is used here because fetching all documents without a
     * userId filter would need a separate single-field or collection-group
     * index that isn't in your current Firestore setup. We fetch the whole
     * collection and sort newest-first in memory instead — correct and fast
     * for typical community-app volumes.
     *
     * Optional [statusFilter] is also applied in memory after the fetch.
     */
    suspend fun getAllReports(statusFilter: String? = null): List<WasteReport> {
        return try {
            val allDocs = db.collection("wasteReports")
                .get()              // no .orderBy() — no extra index required
                .await()
                .documents
                .mapNotNull { doc ->
                    try {
                        doc.toObject(WasteReport::class.java)
                    } catch (parseEx: Exception) {
                        Log.w(tag, "Skipping unparseable report ${doc.id}: ${parseEx.message}")
                        null
                    }
                }

            val filtered = if (statusFilter != null)
                allDocs.filter { it.status == statusFilter }
            else
                allDocs

            filtered.sortedByDescending { it.reportDate.seconds }
        } catch (e: Exception) {
            Log.e(tag, "getAllReports failed: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Authority: update a report's status.
     *
     * When marking COMPLETED:
     *  1. Reads pointsAwarded from Firestore BEFORE updating status (guards
     *     against local-object deserialization mismatches and avoids a second
     *     round-trip after the write).
     *  2. Atomically increments the resident's totalPoints via FieldValue.increment.
     *  3. Sends a notification to the resident (non-critical).
     */
    suspend fun updateReportStatus(
        reportId: String,
        newStatus: String,
        userId: String,
        pointsAwarded: Int = 0
    ): Boolean {
        return try {
            val reportRef = db.collection("wasteReports").document(reportId)

            // 1. Read authoritative points from Firestore BEFORE writing status
            val ptsToAward: Int = if (newStatus == "COMPLETED") {
                if (pointsAwarded > 0) {
                    pointsAwarded
                } else {
                    // Caller passed 0 — read from Firestore to be sure
                    val snap = reportRef.get().await()
                    (snap.getLong("pointsAwarded") ?: snap.getLong("points_awarded") ?: 0L)
                        .toInt()
                        .also { Log.d(tag, "Points read from Firestore for $reportId: $it") }
                }
            } else 0

            // 2. Update the report status
            reportRef.update("status", newStatus).await()
            Log.d(tag, "updateReportStatus: $reportId -> $newStatus")

            // 3. Award points atomically (COMPLETED only)
            if (newStatus == "COMPLETED" && ptsToAward > 0) {
                db.collection("users").document(userId)
                    .update("totalPoints", FieldValue.increment(ptsToAward.toLong()))
                    .await()
                Log.d(tag, "Awarded $ptsToAward pts to $userId for report $reportId")
            }

            // 4. Send notification to the resident (non-critical — won't roll back status)
            try {
                val notificationId = java.util.UUID.randomUUID().toString()
                val message = when (newStatus) {
                    "COMPLETED" ->
                        if (ptsToAward > 0)
                            "Your waste report has been completed! +$ptsToAward points have been added to your account."
                        else
                            "Your waste report has been marked as completed."
                    "APPROVED"  -> "Your waste report has been approved and is being processed."
                    "REJECTED"  -> "Your waste report was reviewed and could not be processed at this time."
                    else        -> "Your waste report status has been updated to: $newStatus"
                }
                val notification = Notification(
                    notificationId = notificationId,
                    userId         = userId,
                    title          = "Report Status Updated",
                    message        = message,
                    type           = "STATUS_UPDATE",
                    relatedId      = reportId
                )
                db.collection("notifications").document(notificationId).set(notification).await()
            } catch (notifEx: Exception) {
                Log.w(tag, "Notification creation failed (non-critical)", notifEx)
            }

            true
        } catch (e: Exception) {
            Log.e(tag, "updateReportStatus failed for reportId=$reportId", e)
            false
        }
    }

    // ==================== Categories & Content ====================

    suspend fun getWasteCategories(): List<WasteCategory> {
        return try {
            db.collection("wasteCategories").get().await()
                .documents.mapNotNull { it.toObject(WasteCategory::class.java) }
        } catch (e: Exception) {
            Log.e(tag, "getWasteCategories failed", e)
            emptyList()
        }
    }

    suspend fun getEducationalContent(): List<EducationalContent> {
        return try {
            db.collection("educationalContent").get().await()
                .documents.mapNotNull { it.toObject(EducationalContent::class.java) }
        } catch (e: Exception) {
            Log.e(tag, "getEducationalContent failed", e)
            emptyList()
        }
    }

    suspend fun getCampaigns(): List<Campaign> {
        return try {
            val querySnapshot = db.collection("campaigns").get().await()
            Log.d(tag, "getCampaigns: found ${querySnapshot.documents.size} documents")
            querySnapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Campaign::class.java) ?: Campaign(
                        campaignId   = doc.id,
                        title        = doc.getString("title") ?: "",
                        description  = doc.getString("description") ?: "",
                        pointsReward = (doc.getLong("pointsReward") ?: 0L).toInt(),
                        status       = doc.getString("status") ?: "UPCOMING",
                        location     = doc.getString("location"),
                        postedBy     = doc.getString("postedBy") ?: ""
                    )
                } catch (parseEx: Exception) {
                    Log.w(tag, "Campaign parse error for ${doc.id}: ${parseEx.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "getCampaigns failed", e)
            emptyList()
        }
    }

    // ==================== Leaderboard ====================

    suspend fun getLeaderboard(): List<LeaderboardEntry> {
        return try {
            db.collection("users")
                .orderBy("totalPoints", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
                .documents
                .mapIndexedNotNull { index, document ->
                    document.toObject(User::class.java)?.let { user ->
                        LeaderboardEntry(
                            userId      = user.userId,
                            username    = user.username,
                            totalPoints = user.totalPoints,
                            rank        = index + 1
                        )
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "getLeaderboard failed", e)
            emptyList()
        }
    }

    // ==================== Announcements ====================

    suspend fun postAnnouncement(announcement: Announcement): Boolean {
        return try {
            db.collection("announcements")
                .document(announcement.announcementId)
                .set(announcement)
                .await()

            val campaign = Campaign(
                campaignId   = announcement.announcementId,
                title        = announcement.title,
                description  = announcement.message,
                startDate    = announcement.createdAt,
                endDate      = announcement.createdAt,
                location     = null,
                pointsReward = 0,
                status       = "ACTIVE",
                postedBy     = announcement.authorityName.ifBlank { "Authority" }
            )
            db.collection("campaigns").document(campaign.campaignId).set(campaign).await()
            Log.d(tag, "Campaign mirror success: ${campaign.campaignId}")

            try {
                val residents = db.collection("users")
                    .whereEqualTo("role", "RESIDENT")
                    .get().await()
                val batch = db.batch()
                residents.documents.forEach { doc ->
                    val notifId = java.util.UUID.randomUUID().toString()
                    val notif = Notification(
                        notificationId = notifId,
                        userId         = doc.id,
                        title          = announcement.title,
                        message        = announcement.message,
                        type           = "ANNOUNCEMENT",
                        relatedId      = announcement.announcementId
                    )
                    batch.set(db.collection("notifications").document(notifId), notif)
                }
                batch.commit().await()
            } catch (broadcastEx: Exception) {
                Log.w(tag, "Notification broadcast failed (non-critical)", broadcastEx)
            }

            Log.d(tag, "postAnnouncement success: ${announcement.announcementId}")
            true
        } catch (e: Exception) {
            Log.e(tag, "postAnnouncement failed", e)
            false
        }
    }

    // ==================== Notifications ====================

    suspend fun getUserNotifications(userId: String): List<Notification> {
        return try {
            db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Notification::class.java) }
        } catch (e: Exception) {
            Log.e(tag, "getUserNotifications failed for userId=$userId", e)
            emptyList()
        }
    }

    suspend fun markNotificationRead(notificationId: String): Boolean {
        return try {
            db.collection("notifications").document(notificationId)
                .update("isRead", true)
                .await()
            true
        } catch (e: Exception) {
            Log.e(tag, "markNotificationRead failed for $notificationId", e)
            false
        }
    }

    // ==================== Collection Schedules ====================

    suspend fun getCollectionSchedules(): List<CollectionSchedule> {
        return try {
            db.collection("collectionSchedules").get().await()
                .documents.mapNotNull { it.toObject(CollectionSchedule::class.java) }
        } catch (e: Exception) {
            Log.e(tag, "getCollectionSchedules failed", e)
            emptyList()
        }
    }

    suspend fun saveCollectionSchedule(schedule: CollectionSchedule): Boolean {
        return try {
            db.collection("collectionSchedules").document(schedule.scheduleId).set(schedule).await()

            try {
                val residents = db.collection("users")
                    .whereEqualTo("role", "RESIDENT")
                    .get().await()
                val batch = db.batch()
                residents.documents.forEach { doc ->
                    val notifId = java.util.UUID.randomUUID().toString()
                    val notif = Notification(
                        notificationId = notifId,
                        userId         = doc.id,
                        title          = "New Collection Schedule",
                        message        = "${schedule.wasteType} collection at ${schedule.area} every ${schedule.dayOfWeek} at ${schedule.time}",
                        type           = "ANNOUNCEMENT",
                        relatedId      = schedule.scheduleId
                    )
                    batch.set(db.collection("notifications").document(notifId), notif)
                }
                batch.commit().await()
            } catch (notifEx: Exception) {
                Log.w(tag, "Schedule notification failed (non-critical)", notifEx)
            }

            true
        } catch (e: Exception) {
            Log.e(tag, "saveCollectionSchedule failed", e)
            false
        }
    }

    // ==================== Guidelines ====================

    suspend fun getGuidelines(): List<Guideline> {
        return try {
            db.collection("guidelines").get().await()
                .documents.mapNotNull { it.toObject(Guideline::class.java) }
        } catch (e: Exception) {
            Log.e(tag, "getGuidelines failed", e)
            emptyList()
        }
    }
}