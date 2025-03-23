package com.arjunmnath.wifilogger.wifi

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.logging.Logger

class LoginService : Service() {
    private val nReTries = 3;
    val channelId = "wifi_auto_login_channel"
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.getLogger("WifiLoginService").info("Service started")
        var handler: LoginHandler = LoginHandler(this);
        handler.initiateLogin();
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("WiFi Login Running")
            .setContentText("Your WiFi auto-login service is active.")
            .setSmallIcon(R.drawable.ic_secure)
            .build()
        startForeground(1, notification)
        createNotificationChannel()
    }


    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Wifi Auto Login"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    internal fun sendNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.stat_notify_chat)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1001, notification)
    }
    override fun onBind(intent: Intent?): IBinder? = null
}