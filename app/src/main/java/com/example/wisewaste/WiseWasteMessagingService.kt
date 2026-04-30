package com.example.wisewaste

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WiseWasteMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Handle incoming FCM messages
        remoteMessage.notification?.let {
            // Notification handling can be expanded with NotificationManager
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Save new FCM token to Firestore
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmToken", token)
    }
}