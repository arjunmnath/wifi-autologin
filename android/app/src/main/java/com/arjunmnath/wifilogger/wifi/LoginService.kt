package com.arjunmnath.wifilogger.wifi

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.logging.Logger

class LoginService : Service() {
    private val channelId = "wifi_login_channel"
    private val notificationId = 1
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.getLogger("WifiLoginService").info("Service started")
        CoroutineScope(Dispatchers.IO).launch {
            var handler: LoginHandler = LoginHandler(this@LoginService);
            val state = handler.initiateLogin();
            Logger.getLogger("WifiLoginService").info(state.toString())
            if (state == LoginState.CONNECTED) {
                updateNotification("Connected to WiFi")
                initiateLogoutTimer()
            }
            else if (state == LoginState.LOGGEDIN) {
                updateNotification("Logged in to WiFi")
                initiateLogoutTimer()
            }
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        notificationManager = getSystemService(NotificationManager::class.java)
        notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("WiFi Login Running")
            .setContentText("Initializing WiFi auto-login...")
            .setSmallIcon(R.drawable.ic_secure)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        startForeground(notificationId, notificationBuilder.build())
    }

    private fun initiateLogoutTimer() {
        serviceScope.launch {
            for (i in 10700 downTo 0) {
                updateNotification("Time remaining on network $i seconds...");
                delay(1000);
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "WiFi Auto Login",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(message: String) {
        val logout = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "LOGOUT"
        }
        val logoutPendingIntent = PendingIntent.getBroadcast(this, 0, logout, PendingIntent.FLAG_IMMUTABLE)
        notificationBuilder.clearActions();
        notificationBuilder.setContentText(message)
        notificationBuilder.addAction(R.drawable.ic_secure, "Logout", logoutPendingIntent)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}