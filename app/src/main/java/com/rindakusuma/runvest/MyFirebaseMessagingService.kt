package com.rindakusuma.runvest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        const val CHANNEL_ID = "runvest_notifications"
        const val CHANNEL_NAME = "RunVest Notifications"
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // Jika Anda perlu mengirim token ke server aplikasi Anda, lakukan di sini
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        // Implementasikan logika untuk mengirim token ke server Anda di sini
        Log.d(TAG, "sendRegistrationTokenToServer($token)")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Cek jika message mengandung data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            // Handle data message di sini
            handleDataMessage(remoteMessage.data)
        }

        // Cek jika message mengandung notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title ?: getString(R.string.app_name), it.body ?: "")
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        // Implementasikan logika untuk menangani data message
        // Contoh: bisa membuka activity tertentu berdasarkan data
        val title = data["title"] ?: getString(R.string.app_name)
        val message = data["message"] ?: ""

        sendNotification(title, message)
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary)) // Sesuaikan dengan warna brand Anda

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Sejak Android Oreo, perlu membuat notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for RunVest notifications"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }

            notificationManager.createNotificationChannel(channel)
        }

        // Gunakan ID unik untuk setiap notifikasi
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}