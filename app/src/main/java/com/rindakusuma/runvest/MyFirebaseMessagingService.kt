package com.rindakusuma.runvest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Ambil data dari notifikasi FCM
        val notification = remoteMessage.notification
        val title = notification?.title ?: "Notification"
        val message = notification?.body ?: "You have a new message."

        sendNotification(title, message)
    }

    private fun sendNotification(title: String, messageBody: String) {
        val channelId = "default_channel_id"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Buat notification channel kalau API >= 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Default Channel"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Ganti icon sesuai kebutuhan
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))

        notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token baru dari FCM, simpan ke database jika perlu
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        user?.let {
            val uid = user.uid
            val dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().reference
            dbRef.child("users").child(uid).child("fcmToken").setValue(token)
        }
    }
}


