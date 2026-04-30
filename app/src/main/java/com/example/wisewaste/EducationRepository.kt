package com.example.wisewaste

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EducationRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    fun getWasteCategories(): Flow<Resource<List<WasteCategory>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection("wasteCategories").get().await()
            val categories = snapshot.documents.mapNotNull { it.toObject(WasteCategory::class.java) }
            emit(Resource.Success(categories))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load categories"))
        }
    }

    fun getWasteItemsByCategory(categoryId: String): Flow<Resource<List<WasteItem>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection("wasteItems")
                .whereEqualTo("categoryId", categoryId)
                .get()
                .await()
            val items = snapshot.documents.mapNotNull { it.toObject(WasteItem::class.java) }
            emit(Resource.Success(items))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load waste items"))
        }
    }

    fun getEducationalContent(): Flow<Resource<List<EducationalContent>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection("educationalContent")
                .orderBy("publishedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val content = snapshot.documents.mapNotNull { it.toObject(EducationalContent::class.java) }
            emit(Resource.Success(content))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load content"))
        }
    }

    suspend fun markContentCompleted(contentId: String, pointsAwarded: Int): Resource<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
            // Record completion
            firestore.collection("userContentProgress")
                .document("${uid}_${contentId}")
                .set(mapOf("userId" to uid, "contentId" to contentId, "completedAt" to Timestamp.now()))
                .await()
            // Award points
            if (pointsAwarded > 0) {
                firestore.collection("users").document(uid)
                    .update("totalPoints", com.google.firebase.firestore.FieldValue.increment(pointsAwarded.toLong()))
                    .await()
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark content completed")
        }
    }
}