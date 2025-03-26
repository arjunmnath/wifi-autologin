package com.arjunmnath.wifilogger.wifi

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log

class StateChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("StateChangeReceiver", "State changed");
        if (intent.action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
            val wifiState =
                intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
            val isRunning = isServiceRunning(context, LoginService::class.java)
            when (wifiState) {
                WifiManager.WIFI_STATE_ENABLED -> {
                    Log.d("WifiStateChangeReceiver", "WiFi turned ON")
                    val serviceIntent = Intent(context, MonitorService::class.java)
                    context.startForegroundService(serviceIntent)
                }

                WifiManager.WIFI_STATE_DISABLED-> {
                    Log.d("WifiStateChangeReceiver", "WiFi turned OFF")
                    val serviceIntent = Intent(context, LoginService::class.java)
                    context.stopService(serviceIntent)
                }
            }
        }
    }
    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE).any { it.service.className == serviceClass.name }
    }
}