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

    // ==================== Campaign Participation ====================

    suspend fun getCampaignParticipants(campaignId: String): Set<String> {
        return try {
            val doc = db.collection("campaignParticipants").document(campaignId).get().await()
            @Suppress("UNCHECKED_CAST")
            (doc.get("userIds") as? List<String> ?: emptyList()).toSet()
        } catch (e: Exception) {
            Log.e(tag, "getCampaignParticipants failed for $campaignId", e)
            emptySet()
        }
    }

    suspend fun hasJoinedCampaign(campaignId: String, userId: String): Boolean {
        return try {
            val doc = db.collection("campaignParticipants").document(campaignId).get().await()
            @Suppress("UNCHECKED_CAST")
            val list = doc.get("userIds") as? List<String> ?: emptyList()
            list.contains(userId)
        } catch (e: Exception) {
            Log.e(tag, "hasJoinedCampaign failed", e)
            false
        }
    }

    suspend fun joinCampaign(campaignId: String, userId: String): Boolean {
        return try {
            db.collection("campaignParticipants").document(campaignId)
                .set(
                    mapOf("userIds" to FieldValue.arrayUnion(userId)),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()
            Log.d(tag, "joinCampaign: userId=$userId campaignId=$campaignId")
            true
        } catch (e: Exception) {
            Log.e(tag, "joinCampaign failed", e)
            false
        }
    }

    suspend fun completeCampaignAndAwardPoints(campaign: Campaign, pointsToAward: Int): Boolean {
        return try {
            // 1. Mark campaign as COMPLETED
            val updated = campaign.copy(status = "COMPLETED")
            db.collection("campaigns").document(campaign.campaignId).set(updated).await()

            // 2. Fetch participants
            val participants = getCampaignParticipants(campaign.campaignId)
            Log.d(tag, "completeCampaign: ${participants.size} participants for ${campaign.campaignId}")

            if (participants.isNotEmpty() && pointsToAward > 0) {
                // 3. Batch-increment points for all participants
                val batch = db.batch()
                participants.forEach { uid ->
                    val userRef = db.collection("users").document(uid)
                    batch.update(userRef, "totalPoints", FieldValue.increment(pointsToAward.toLong()))
                }
                batch.commit().await()
                Log.d(tag, "Awarded $pointsToAward pts to ${participants.size} participants")

                // 4. Send notification to each participant (non-critical)
                try {
                    val notifBatch = db.batch()
                    participants.forEach { uid ->
                        val notifId = java.util.UUID.randomUUID().toString()
                        val notif = Notification(
                            notificationId = notifId,
                            userId         = uid,
                            title          = "Campaign Completed! 🎉",
                            message        = "\"${campaign.title}\" has been completed. +$pointsToAward points have been added to your account!",
                            type           = "ANNOUNCEMENT",
                            relatedId      = campaign.campaignId
                        )
                        notifBatch.set(db.collection("notifications").document(notifId), notif)
                    }
                    notifBatch.commit().await()
                } catch (notifEx: Exception) {
                    Log.w(tag, "Notification broadcast failed (non-critical)", notifEx)
                }
            }

            true
        } catch (e: Exception) {
            Log.e(tag, "completeCampaignAndAwardPoints failed", e)
            false
        }
    }

    suspend fun saveCampaign(campaign: Campaign): Boolean {
        return try {
            db.collection("campaigns").document(campaign.campaignId).set(campaign).await()
            Log.d(tag, "saveCampaign success: ${campaign.campaignId}")

            // Notify all residents of the new campaign
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
                        title          = "New Campaign: ${campaign.title}",
                        message        = campaign.description,
                        type           = "ANNOUNCEMENT",
                        relatedId      = campaign.campaignId
                    )
                    batch.set(db.collection("notifications").document(notifId), notif)
                }
                batch.commit().await()
            } catch (notifEx: Exception) {
                Log.w(tag, "Campaign notification failed (non-critical)", notifEx)
            }

            true
        } catch (e: Exception) {
            Log.e(tag, "saveCampaign failed", e)
            false
        }
    }

    suspend fun updateCampaign(campaign: Campaign): Boolean {
        return try {
            db.collection("campaigns").document(campaign.campaignId).set(campaign).await()
            Log.d(tag, "updateCampaign success: ${campaign.campaignId}")
            true
        } catch (e: Exception) {
            Log.e(tag, "updateCampaign failed", e)
            false
        }
    }

    suspend fun deleteCampaign(campaignId: String): Boolean {
        return try {
            db.collection("campaigns").document(campaignId).delete().await()
            Log.d(tag, "deleteCampaign success: $campaignId")
            true
        } catch (e: Exception) {
            Log.e(tag, "deleteCampaign failed", e)
            false
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

    suspend fun deleteNotification(notificationId: String): Boolean {
        return try {
            db.collection("notifications").document(notificationId).delete().await()
            true
        } catch (e: Exception) {
            Log.e(tag, "deleteNotification failed for $notificationId", e)
            false
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

    suspend fun updateCollectionSchedule(schedule: CollectionSchedule): Boolean {
        return try {
            db.collection("collectionSchedules").document(schedule.scheduleId).set(schedule).await()
            Log.d(tag, "updateCollectionSchedule success: ${schedule.scheduleId}")

            // Notify all residents of the updated schedule
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
                        title          = "Collection Schedule Updated",
                        message        = "${schedule.wasteType} collection at ${schedule.area} has been updated to every ${schedule.dayOfWeek} at ${schedule.time}.",
                        type           = "ANNOUNCEMENT",
                        relatedId      = schedule.scheduleId
                    )
                    batch.set(db.collection("notifications").document(notifId), notif)
                }
                batch.commit().await()
            } catch (notifEx: Exception) {
                Log.w(tag, "Schedule update notification failed (non-critical)", notifEx)
            }

            true
        } catch (e: Exception) {
            Log.e(tag, "updateCollectionSchedule failed", e)
            false
        }
    }


    suspend fun getCompletedContentIds(userId: String): Set<String> {
        return try {
            val doc = db.collection("userProgress").document(userId).get().await()
            @Suppress("UNCHECKED_CAST")
            val list = doc.get("completedContent") as? List<String> ?: emptyList()
            list.toSet()
        } catch (e: Exception) {
            Log.e(tag, "getCompletedContentIds failed for userId=$userId", e)
            emptySet()
        }
    }

    suspend fun markContentCompleted(userId: String, contentId: String, points: Int): Boolean {
        return try {
            // 1. Add contentId to the completed array (idempotent)
            db.collection("userProgress").document(userId)
                .set(
                    mapOf("completedContent" to FieldValue.arrayUnion(contentId)),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()

            // 2. Award points only if not already completed (checked by caller)
            if (points > 0) {
                db.collection("users").document(userId)
                    .update("totalPoints", FieldValue.increment(points.toLong()))
                    .await()
            }

            Log.d(tag, "markContentCompleted: userId=$userId contentId=$contentId pts=$points")
            true
        } catch (e: Exception) {
            Log.e(tag, "markContentCompleted failed: ${e.message}", e)
            false
        }
    }



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