package com.arjunmnath.wifilogger.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class StateChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WifiStateChangeReceiver", "WiFi state changed")
        val serviceIntent = Intent(context, MonitorService::class.java)
        context.startForegroundService(serviceIntent)
    }
}