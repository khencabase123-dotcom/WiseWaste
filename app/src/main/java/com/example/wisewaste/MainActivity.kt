package com.example.wisewaste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            WiseWasteApp()
        }
    }
}

@Composable
fun WiseWasteApp() {
    val auth = FirebaseAuth.getInstance()
    val authManager = remember { AuthManager() }
    val db = remember { FirebaseHelper() }
    val scope = rememberCoroutineScope()

    // null = still resolving, "" = not logged in, "RESIDENT"/"AUTHORITY" = logged in with known role
    var userRole by remember { mutableStateOf<String?>(null) }

    // On first composition, check if a session already exists and resolve the role
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            userRole = ""   // Not logged in
        } else {
            // Session exists — fetch role from Firestore before showing any dashboard
            val role = db.getUser(uid)?.role ?: "RESIDENT"
            userRole = role
        }
    }

    // Also listen for sign-out events (e.g. token expiry) to reset state
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                userRole = ""   // Signed out — go back to login
            }
            // Sign-in is handled by onLoginSuccess below (we already know the role there)
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF4CAF50),
            secondary = Color(0xFF795548),
            background = Color(0xFFF5F5F5)
        )
    ) {
        when (userRole) {
            null -> {
                // Resolving session — show spinner, render nothing else
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF4CAF50))
                }
            }
            "" -> {
                // Not authenticated
                LoginScreen(
                    onLoginSuccess = { role ->
                        // Role is provided directly from the login result — no extra Firestore fetch
                        userRole = role
                    }
                )
            }
            "AUTHORITY" -> {
                AuthorityDashboardScreen(onLogout = {
                    authManager.logout()
                    userRole = ""
                })
            }
            else -> {
                // "RESIDENT" or any unrecognised role defaults to resident dashboard
                ResidentDashboardScreen(onLogout = {
                    authManager.logout()
                    userRole = ""
                })
            }
        }
    }
}