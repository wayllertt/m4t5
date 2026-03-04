package com.example.m4t5

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ForegroundCounterService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var secondsElapsed: Int = 0
    private var tickerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(secondsElapsed))
        if (tickerJob?.isActive != true) {
            tickerJob = serviceScope.launch {
                while (true) {
                    delay(1_000)
                    secondsElapsed++
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, buildNotification(secondsElapsed))
                    sendBroadcast(Intent(ACTION_COUNTER_TICK).putExtra(EXTRA_SECONDS, secondsElapsed))
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(seconds: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Foreground service")
            .setContentText("Прошло $seconds секунд")
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Counter",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_COUNTER_TICK = "com.example.m4t5.ACTION_COUNTER_TICK"
        const val EXTRA_SECONDS = "extra_seconds"
        private const val CHANNEL_ID = "foreground_counter_channel"
        private const val NOTIFICATION_ID = 101
    }
}
