package com.freeebookbundle.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.freeebookbundle.MainActivity
import com.freeebookbundle.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID = "push_notifications"
        const val CHANNEL_NAME = "Push Notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d(TAG, "New FCM token: $token")
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        android.util.Log.d(TAG, "Message received from: ${message.from}")

        if (message.notification != null) {
            val title = message.notification?.title ?: getString(R.string.app_name)
            val body = message.notification?.body ?: ""
            showNotification(title, body)
        }

        message.data.isNotEmpty().let {
            android.util.Log.d(TAG, "Message data: ${message.data}")
            handleDataMessage(message.data)
        }
    }

    private fun sendTokenToServer(token: String) {
        // TODO: Send token to your backend server
        // This would typically be an API call to save the token in your database
        android.util.Log.d(TAG, "Token ready to send to server: $token")
        
        // Store locally for now (would be replaced with API call in production)
        val prefs = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        val title = data["title"]
        val body = data["body"]
        val bookId = data["book_id"]

        when (type) {
            "book_update" -> {
                android.util.Log.d(TAG, "Book update received: $bookId - $title")
                showNotification(title ?: "Book Updated", body ?: "New content available", type, bookId)
            }
            "new_book" -> {
                showNotification(title ?: "New Book Available", body ?: "A new book is waiting for you!", type, bookId)
            }
            "reading_reminder" -> {
                showNotification(
                    title ?: "Time to Read",
                    body ?: "Continue your journey in your current book.",
                    type,
                    bookId
                )
            }
            "streak_milestone" -> {
                val days = data["days"] ?: "7"
                showNotification(
                    title ?: "Amazing!",
                    body ?: "You've reached a $days-day reading streak!",
                    type
                )
            }
            "daily_goal" -> {
                showNotification(
                    title ?: "Goal Reached!",
                    body ?: "You've hit your reading goal for today. Keep it up!",
                    type
                )
            }
            "genre_update" -> {
                val genre = data["genre"] ?: "your favorite"
                showNotification(
                    title ?: "New for You",
                    body ?: "New $genre books have been added to the library.",
                    type,
                    genre = genre
                )
            }
            "book_finished" -> {
                showNotification(
                    title ?: "Congratulations!",
                    body ?: "You've finished reading. How would you rate it?",
                    type,
                    bookId
                )
            }
            "voice_update" -> {
                showNotification(
                    title ?: "New Voice Available",
                    body ?: "Enhance your experience with a new high-quality TTS voice.",
                    type
                )
            }
            "download_complete" -> {
                showNotification(
                    title ?: "Download Complete",
                    body ?: "Your book is ready for offline reading.",
                    type,
                    bookId
                )
            }
            else -> {
                android.util.Log.d(TAG, "Unknown message type: $type")
                if (title != null && body != null) {
                    showNotification(title, body)
                }
            }
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String? = null,
        bookId: String? = null,
        genre: String? = null
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", type)
            putExtra("book_id", bookId)
            putExtra("genre", genre)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_book)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Use a unique ID based on type and bookId to allow multiple notifications
        val notificationId = when (type) {
            "book_update", "new_book", "reading_reminder", "book_finished", "download_complete" -> {
                (type + (bookId ?: "")).hashCode()
            }
            else -> type.hashCode()
        }
        
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Push notification channel for new books and updates"
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}