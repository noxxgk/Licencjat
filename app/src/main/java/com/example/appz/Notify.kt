package com.example.appz


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.app.PendingIntent
import android.content.BroadcastReceiver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.content.pm.ServiceInfo

class Notify : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val NOTIFICATION_DELAY = 1800 * 1000L//1800 normalnie 15 do pokazania
    companion object {
        const val CHANNEL_ID = "delayed_notification_channel"
        const val NOTIFICATION_ID = 1
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scheduleNotification()
        return START_STICKY
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Opóźnione powiadomienia"
            val descriptionText = "Kanał dla powiadomień wysyłanych z opóźnieniem"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun scheduleNotification() {
        handler.postDelayed({
            showNotification()
        }, NOTIFICATION_DELAY)
    }
    private fun showNotification() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle("Pamiętaj")
            .setContentText("Sprawdź czy już jesteś trzeźwy")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}