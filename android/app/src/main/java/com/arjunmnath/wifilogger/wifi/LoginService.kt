package com.arjunmnath.wifilogger.wifi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.logging.Logger

class NotificationAction {
    val title: String
    val intent: PendingIntent
    val drawable: Int
    constructor(title: String, intent: PendingIntent, drawable: Int) {
        this.title = title
        this.intent = intent
        this.drawable = drawable
    }
}

class LoginService : Service() {
    // TODO: logout timer not resetting
    // TODO: on wifi off state, stop the service, restart when wifi is turned on
    private val channelId = "wifi_login_channel"
    private val logoutInterval = 10800;
    private var state: LoginState = LoginState.UNKNOWN;
    private val notificationId = 1
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.getLogger("WifiLoginService").info("Service started")
        when (intent?.action) {
            ACTION_RETRY -> loginAction()
            ACTION_LOGIN -> loginAction()
            ACTION_LOGOUT -> logoutAction()
        }
        return START_STICKY
    }

    private fun logoutAction() {
        // todo: failed to update the notification fix it, fix: implement a lock for updateNotification
        CoroutineScope(Dispatchers.IO).launch {
            state = LoginHandler.initiateLogout()
            if (state == LoginState.LOGGEDOUT) {
                updateNotification(
                    title = "Logged out",
                    message = "You have been logged out",
                    onGoing = false,
                    indents = arrayOf(
                        NotificationAction(
                            title = "login",
                            drawable =com.arjunmnath.wifilogger.R.drawable.star_icon,
                            intent = getRetryIntend()
                        )
                    )
                )
            }
        }
    }


    private fun loginAction() {
        CoroutineScope(Dispatchers.IO).launch {
            var handler = LoginHandler(this@LoginService)
            state = handler.initiateLogin()
            Logger.getLogger("WifiLoginService").info(state.toString())
            when (state) {
                LoginState.CONNECTED, LoginState.LOGGEDIN -> {
                    val intent = Intent(this@LoginService, VPNService::class.java)
                    startService(intent)
                    initSuccessNotification()
                }

                LoginState.MAXCONCURRENT -> {
                    // TODO: ADD a retry action
                    updateNotification(
                        title = "Wifi Login Failed",
                        message = "Max concurrent login reached",
                        onGoing = false,
                        indents = arrayOf(NotificationAction(title = "Retry", drawable =com.arjunmnath.wifilogger.R.drawable.star_icon, intent = getRetryIntend()))
                    )
                }

                LoginState.LOGGEDOUT -> {
                    // TODO: recheck the logic here
                    updateNotification(
                        title = "Wifi logged out",
                        message = "You have been Logged out",
                        onGoing = false,
                        indents = arrayOf()
                    )
                }

                LoginState.AUTHFAILED -> {
                    // TODO: onclick open set username password page
                    updateNotification(
                        title="Wifi Login Failed",
                        message = "Wrong Username or Password",
                        onGoing = false,
                        indents = arrayOf()
                    )
                }

                LoginState.UNKNOWN -> {
                    updateNotification(
                        title = "Wifi Login Failed",
                        message = "Unknown Error",
                        onGoing = false,
                        indents = arrayOf()
                    )
                }

                LoginState.CREDUNAVAILABLE -> {
                    // TODO: onclick open set username password page
                    updateNotification(
                        title = "Wifi Login Failed",
                        message = "Username or Password not set",
                        onGoing = false,
                        indents = arrayOf())
                }
            }
        }
    }
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        notificationManager = getSystemService(NotificationManager::class.java)
        notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("WiFi Login Running")
            .setContentText("Initializing WiFi auto-login...")
            .setSmallIcon(com.arjunmnath.wifilogger.R.drawable.star_icon)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        startForeground(notificationId, notificationBuilder.build())
    }
    private fun initSuccessNotification() {
        serviceScope.launch {
            for (i in logoutInterval downTo 0 ){
                if (state != LoginState.LOGGEDIN) {
                    break;
                }
                updateNotification(
                    title = "Logged in to WiFi",
                    message = "Time remaining on network $i seconds...",
                    onGoing = true,
                    indents = arrayOf(NotificationAction(title = "Logout", drawable = com.arjunmnath.wifilogger.R.drawable.star_icon, intent = getLogoutIntend()))
                )
                delay(1000)
            }
        }
    }
    private fun createNotificationChannel() {
            val channel = NotificationChannel(
                channelId,
                "WiFi Auto Login",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)

    }

    private fun updateNotification(title: String, message: String, onGoing: Boolean, indents: Array<NotificationAction>) {
        notificationBuilder.clearActions()
        notificationBuilder.setContentTitle(title)
        notificationBuilder.setContentText(message)
        notificationBuilder.setOngoing(onGoing)
        notificationBuilder.setAutoCancel(true)
        notificationBuilder.setPriority(NotificationManager.IMPORTANCE_DEFAULT)
        notificationBuilder.foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
        for (intent in indents) {
            notificationBuilder.addAction(intent.drawable, intent.title, intent.intent)
        }
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun getRetryIntend(): PendingIntent {
        val intent = Intent(this, LoginService::class.java).apply {
            action =ACTION_RETRY
        }
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getLogoutIntend(): PendingIntent {
        val intent = Intent(this, LoginService::class.java).apply {
            action = ACTION_LOGOUT
        }
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val ACTION_RETRY = "com.arjunmnath.horizon.wifilogger.login_service.ACTION_RETRY"
        const val ACTION_LOGIN = "com.arjunmnath.horizon.wifilogger.login_service.ACTION_LOGIN"
        const val ACTION_LOGOUT= "com.arjunmnath.horizon.wifilogger.login_service.ACTION_LOGOUT"
    }
}