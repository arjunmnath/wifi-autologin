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
                initiateLogout()
            }
        }
    }

    fun initiateLogout() {
        CoroutineScope(Dispatchers.IO).launch {
            val generateLogout = "http://172.16.222.1:1000/logout?"
            val captivePortalRequest= Request.Builder()
                .url(generateLogout)
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Accept", "text/html")
                .get()
                .build()
            val client = OkHttpClient()
            client.newCall(captivePortalRequest).execute().use { response ->
                val logoutPageHtml= response.body?.string()
                val isLoggedOut = extractSuccess(logoutPageHtml.toString())
                Log.d("handleCaptivePortal", isLoggedOut.toString())
            }
        }
    }

    private fun extractSuccess(html: String): Boolean {
        val regex = """You have successfully logged out""".toRegex()
        val matchResult = regex.find(html)
        if (matchResult != null) {
            return true;
        } else {
            Log.d("getLoginPortalURL", "No match found")
            return false;
        }
    }
}