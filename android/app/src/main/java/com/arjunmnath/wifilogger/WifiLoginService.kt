package com.arjunmnath.wifilogger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.logging.Logger
import org.jsoup.Jsoup
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import java.util.Properties

class WifiLoginService  : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.getLogger("WifiLoginService").info("Service started")
        startForeground(1, createNotification())
        handleCaptivePortal()
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val channelId = "wifi_login_channel"
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "WiFi Auto Login",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("WiFi Auto Login")
            .setContentText("Auto-login service running...")
            .setSmallIcon(android.R.drawable.ic_menu_week)
            .build()
    }

    private fun handleCaptivePortal() {
        CoroutineScope(Dispatchers.IO).launch {
            val generateCaptive= "http://connectivitycheck.gstatic.com/"  // Replace with actual portal URL
            val captivePortalRequest= Request.Builder()
                .url(generateCaptive)
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Accept", "text/html")
                .get()
                .build()

            val client = OkHttpClient()
            client.newCall(captivePortalRequest).execute().use { response ->
                val captivePortalHTML = response.body?.string()
                Log.d("handleCaptivePortal", captivePortalHTML.toString())
                val portalURL: String= getLoginPortalURL(captivePortalHTML.toString()) ?: "";
                openLoginPortal(portalURL);
            }
        }
    }

    private fun openLoginPortal(poralUrl: String) {
        val loginPortalRequest = Request.Builder()
            .url(poralUrl)
            .addHeader("User-Agent", "Mozilla/5.0")
            .addHeader("Accept", "text/html")
            .get()
            .build()

        val client = OkHttpClient()
        client.newCall(loginPortalRequest).execute().use { response ->
            val loginPortalHTML= response.body?.string()
            Log.d("openLoginPortal", loginPortalHTML.toString())
            val domainRegex = """http?://([^/]+)""".toRegex();
            val domain = domainRegex.find(poralUrl);
            val redirectAndMagic  = extractRedirectAndMagic(domain?.value.toString(), loginPortalHTML.toString());
            if (
                !redirectAndMagic.get("submit").isNullOrEmpty() &&
                !redirectAndMagic.get("magic").isNullOrEmpty() &&
                !redirectAndMagic.get("4Tredir").isNullOrEmpty()) {
                val properties = Properties().apply {
                    resources.openRawResource(R.raw.config);
                }
                val apiKey = properties.getProperty("api_key")
                println(apiKey)
                redirectAndMagic["username"] = properties.getProperty("username")
                redirectAndMagic["password"] =  properties.getProperty("password")
                doLoginRequest(redirectAndMagic["submit"]!!, redirectAndMagic);
            }
        }
    }

    private fun doLoginRequest(URL: String, map: MutableMap<String, String>) {
        val loginPayload = FormBody.Builder()
        for ((key, value) in map) {
            loginPayload.add(key, value)
        }
        val requestBody = loginPayload.build()
        val loginPortalRequest = Request.Builder()
            .url(URL)
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(loginPortalRequest).execute().use { response ->
            Log.d("DoLoginRequest", response.body?.string().toString())
        }
    }

    private fun extractRedirectAndMagic(loginPortalDomain:String, html: String): MutableMap<String, String> {
        val regex = """<(form|input)[^>]*>""".toRegex()
        val matches = regex.findAll(html)
        var matchedTags = "";
        matches.forEach {
            matchedTags += it.value
        }
        val doc = Jsoup.parse(matchedTags);
        var res: MutableMap<String, String> = mutableMapOf()
        val forms = doc.select("form");
        for (form in forms) {
            val action = form.attr("action")
            val method = form.attr("method")
            val inputs = form.select("input")
            res["submit"] = loginPortalDomain + action;
            Log.d("extractRedirectAndMagic", "Form action: $action, $method, $inputs")
            for (input in inputs) {
                val name = input.attr("name")
                val value = input.attr("value")
                if (name == "4Tredir" || name=="magic") {
                    res[name] = value;
                }
            }

        }
        for ((key, value) in res) {
            Log.d("openLoginPortal", "$key: $value")
        }
        return res
    }
    private fun getLoginPortalURL(html:String): String? {
        val regex = """http:\/\/172\.16\.222\.1:1000\/fgtauth\?([a-fA-F0-9]+)""".toRegex()
        val matchResult = regex.find(html)
        if (matchResult != null) {
            val entireUrl = matchResult.value
            Log.d("getLoginPortalURL", entireUrl)
            return entireUrl;
        } else {
            Log.d("getLoginPortalURL", "No match found")
            return null
        }
    }
    override fun onBind(intent: Intent?): IBinder? = null
}