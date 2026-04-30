package com.example.wisewaste

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun register(
        email: String,
        password: String,
        username: String,
        barangay: String
    ): Resource<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("User ID is null")
            val user = User(
                userId = uid,
                username = username,
                email = email,
                role = UserRole.RESIDENT,
                barangay = barangay
            )
            firestore.collection("users").document(uid).set(user).await()
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Registration failed")
        }
    }

    suspend fun login(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Resource.Success(result.user!!)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Login failed")
        }
    }

    suspend fun resetPassword(email: String): Resource<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Reset failed")
        }
    }

    fun logout() = auth.signOut()

    suspend fun getCurrentUserData(): Resource<User> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
            val doc = firestore.collection("users").document(uid).get().await()
            val user = doc.toObject(User::class.java) ?: throw Exception("User not found")
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get user data")
        }
    }

    suspend fun updateProfile(username: String, barangay: String): Resource<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
            firestore.collection("users").document(uid)
                .update(mapOf("username" to username, "barangay" to barangay))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update profile")
        }
    }
}