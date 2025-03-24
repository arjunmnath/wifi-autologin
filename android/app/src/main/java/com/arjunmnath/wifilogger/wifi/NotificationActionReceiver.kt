package com.arjunmnath.wifilogger.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request


class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "LOGOUT" -> {
                Log.d("NotificationActionReceiver", "Logout action received")
                CoroutineScope(Dispatchers.IO).launch {
                    initiateLogout()
                    val loginStatus = LoginHandler.checkLoginStatus()
                    Log.d("LoginStatus", "Login status: $loginStatus")
                }
            }
        }
    }



}
