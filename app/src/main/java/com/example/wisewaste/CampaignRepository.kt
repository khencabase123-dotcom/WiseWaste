package com.example.wisewaste

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CampaignRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    fun getCampaigns(): Flow<Resource<List<Campaign>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection("campaigns")
                .orderBy("startDate", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .await()
            val campaigns = snapshot.documents.mapNotNull { it.toObject(Campaign::class.java) }
            emit(Resource.Success(campaigns))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load campaigns"))
        }
    }

    suspend fun joinCampaign(campaignId: String): Resource<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
            val batch = firestore.batch()

            val participantRef = firestore.collection("campaigns")
                .document(campaignId)
                .collection("participants")
                .document(uid)
            batch.set(participantRef, mapOf("userId" to uid, "joinedAt" to Timestamp.now()))

            val campaignRef = firestore.collection("campaigns").document(campaignId)
            batch.update(campaignRef, "participantCount", com.google.firebase.firestore.FieldValue.increment(1))

            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to join campaign")
        }
    }

    suspend fun createCampaign(campaign: Campaign): Resource<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
            val campaignId = UUID.randomUUID().toString()
            val newCampaign = campaign.copy(campaignId = campaignId, createdBy = uid)
            firestore.collection("campaigns").document(campaignId).set(newCampaign).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create campaign")
        }
    }

    fun getLeaderboard(): Flow<Resource<List<LeaderboardEntry>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection("users")
                .orderBy("totalPoints", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            val entries = snapshot.documents.mapIndexedNotNull { index, doc ->
                val userId = doc.getString("userId") ?: return@mapIndexedNotNull null
                LeaderboardEntry(
                    userId = userId,
                    username = doc.getString("username") ?: "",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    totalPoints = (doc.getLong("totalPoints") ?: 0).toInt(),
                    barangay = doc.getString("barangay") ?: "",
                    rank = index + 1
                )
            }
            emit(Resource.Success(entries))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load leaderboard"))
        }
    }

    fun getRewards(): Flow<Resource<List<Reward>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection("rewards")
                .whereEqualTo("isAvailable", true)
                .get()
                .await()
            val rewards = snapshot.documents.mapNotNull { it.toObject(Reward::class.java) }
            emit(Resource.Success(rewards))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load rewards"))
        }
    }

    suspend fun redeemReward(reward: Reward, userPoints: Int): Resource<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
            if (userPoints < reward.pointsCost) throw Exception("Not enough points")

            val redemptionId = UUID.randomUUID().toString()
            val redemption = Redemption(
                redemptionId = redemptionId,
                userId = uid,
                rewardId = reward.rewardId,
                rewardName = reward.name,
                pointsCost = reward.pointsCost,
                status = RedemptionStatus.PENDING
            )

            val batch = firestore.batch()
            batch.set(firestore.collection("redemptions").document(redemptionId), redemption)
            batch.update(
                firestore.collection("users").document(uid),
                "totalPoints",
                com.google.firebase.firestore.FieldValue.increment((-reward.pointsCost).toLong())
            )
            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Redemption failed")
        }
    }
}