package com.example.wisewaste

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val reportsCollection = firestore.collection("wasteReports")

    fun getUserReports(): Flow<Resource<List<WasteReport>>> = flow {
        emit(Resource.Loading())
        try {
            val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
            val snapshot = reportsCollection
                .whereEqualTo("userId", uid)
                .orderBy("reportDate", Query.Direction.DESCENDING)
                .get()
                .await()
            val reports = snapshot.documents.mapNotNull { it.toObject(WasteReport::class.java) }
            emit(Resource.Success(reports))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load reports"))
        }
    }

    fun getAllReports(): Flow<Resource<List<WasteReport>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = reportsCollection
                .orderBy("reportDate", Query.Direction.DESCENDING)
                .get()
                .await()
            val reports = snapshot.documents.mapNotNull { it.toObject(WasteReport::class.java) }
            emit(Resource.Success(reports))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load reports"))
        }
    }

    suspend fun submitReport(
        report: WasteReport,
        imageBytes: ByteArray?
    ): Resource<String> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
            val reportId = UUID.randomUUID().toString()
            var imageUrl = ""

            if (imageBytes != null) {
                val ref = storage.reference.child("reports/$reportId.jpg")
                ref.putBytes(imageBytes).await()
                imageUrl = ref.downloadUrl.await().toString()
            }

            val finalReport = report.copy(
                reportId = reportId,
                userId = uid,
                imageUrl = imageUrl
            )

            reportsCollection.document(reportId).set(finalReport).await()

            // Award points to user
            firestore.collection("users").document(uid)
                .update("totalPoints", com.google.firebase.firestore.FieldValue.increment(10))
                .await()

            Resource.Success(reportId)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to submit report")
        }
    }

    suspend fun updateReportStatus(
        reportId: String,
        status: ReportStatus,
        adminNotes: String = ""
    ): Resource<Unit> {
        return try {
            reportsCollection.document(reportId)
                .update(
                    mapOf(
                        "status" to status.name,
                        "adminNotes" to adminNotes,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update report")
        }
    }

    fun getReportsByStatus(status: ReportStatus): Flow<Resource<List<WasteReport>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = reportsCollection
                .whereEqualTo("status", status.name)
                .orderBy("reportDate", Query.Direction.DESCENDING)
                .get()
                .await()
            val reports = snapshot.documents.mapNotNull { it.toObject(WasteReport::class.java) }
            emit(Resource.Success(reports))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load reports"))
        }
    }
}