package com.arjunmnath.wifilogger.wifi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class MonitorService : Service() {
    private lateinit var connectivityManager: ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                val ssid = wifiInfo.ssid.replace("\"", "")
                Log.d("WifiMonitorService", "Connected to WiFi: $ssid")
                if (ssid.equals("IIITKottayam", ignoreCase = true)) {
                    Log.d("WifiMonitorService", "Connected to target WiFi: $ssid")
                    invokeLoginService()
                }
            }
        }
    }

    private fun invokeLoginService() {
        val serviceIntent = Intent(this, LoginService::class.java)
        serviceIntent.action = LoginService.ACTION_LOGIN;
        startService(serviceIntent)
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        createNotificationChannel()
        startForeground(1, getNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "wifi_monitor_channel",
            "WiFi Monitor",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun getNotification(): Notification {
        return NotificationCompat.Builder(this, "wifi_monitor_channel")
            .setContentTitle("WiFi Monitor Service")
            .setContentText("Monitoring WiFi connection...")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
