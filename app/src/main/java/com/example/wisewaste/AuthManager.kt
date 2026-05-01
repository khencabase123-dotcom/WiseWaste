package com.example.wisewaste

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Login with role verification
    suspend fun login(email: String, password: String, expectedRole: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: return Result.failure(Exception("Login failed: null user"))
            val uid = user.uid

            // Fetch user document from Firestore
            val userDoc = db.collection("users").document(uid).get().await()
            val userRole = userDoc.getString("role") ?: "RESIDENT"

            if (userRole != expectedRole) {
                // Role mismatch – sign out and fail
                auth.signOut()
                return Result.failure(Exception("This account is not registered as $expectedRole"))
            }

            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Register with role (for Community Members, role = "RESIDENT")
    suspend fun register(email: String, password: String, username: String, role: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("User creation failed"))

            val user = User(
                userId = userId,
                username = username.trim(),
                email = email,
                role = role,
                totalPoints = 0
            )

            try {
                db.collection("users").document(userId).set(user).await()
                Result.success(userId)
            } catch (dbError: Exception) {
                // Rollback: delete the Auth user if Firestore write fails
                auth.currentUser?.delete()
                Result.failure(dbError)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid
}