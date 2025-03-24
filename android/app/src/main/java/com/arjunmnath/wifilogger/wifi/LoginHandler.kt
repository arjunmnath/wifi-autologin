package com.arjunmnath.wifilogger.wifi

import android.content.ContextWrapper
import android.util.Log
import com.arjunmnath.wifilogger.R
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.util.Properties
import kotlin.collections.iterator

enum class LoginState { CONNECTED, LOGGEDIN, LOGGEDOUT, MAXCONCURRENT, AUTHFAILED, UNKNOWN, CREDUNAVAILABLE }


class LoginHandler(private val context: LoginService) {
    private var nReTries = 3;
    // TODO: wont work if connected to vpns
    // TODO: IOEXception not catching (unexpected end of stream) (caused function unknwon)
    // TODO: captive portal not responding
    suspend fun initiateLogin() : LoginState{
            val state = checkLoginStatus();
            if (state == LoginState.LOGGEDIN) {
                return state;
            }
            val generateCaptive= "http://connectivitycheck.gstatic.com/"
            val captivePortalRequest= Request.Builder()
                .url(generateCaptive)
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Accept", "text/html")
                .get()
                .build()
            Log.d("handleCaptivePortal", "Captive portal request sent")
            val client = OkHttpClient()
            client.newCall(captivePortalRequest).execute().use { response ->
                val captivePortalHTML = response.body?.string()
                Log.d("handleCaptivePortal", captivePortalHTML.toString())
                val portalURL= extractLoginPortalURL(captivePortalHTML.toString());
                
                if (portalURL == null) {
                    return LoginState.CONNECTED;
                }
                else {
                    return openLoginPortal(portalURL)
                }
            }

    }
    private fun extractLoginPortalURL(html:String): String? {
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

    private suspend fun openLoginPortal(poralUrl: String) : LoginState {
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
            val domainRegex = """http?://([^/]+)""".toRegex()
            val domain = domainRegex.find(poralUrl)
            val redirectAndMagic  = extractRedirectAndMagic(domain?.value.toString(), loginPortalHTML.toString())
            if (
                !redirectAndMagic.get("submit").isNullOrEmpty() &&
                !redirectAndMagic.get("magic").isNullOrEmpty() &&
                !redirectAndMagic.get("4Tredir").isNullOrEmpty()) {
//                val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_details", Context.MODE_PRIVATE)
//                val username = sharedPreferences.getString("username", "")
//                val password = sharedPreferences.getString("password", "")
//                if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
//                    return LoginState.CREDUNAVAILABLE
//                }
                val properties = Properties().apply {
                    ContextWrapper(context).resources.openRawResource(R.raw.config).use { load(it) }
                }
                val username = properties.getProperty("username")
                val password = properties.getProperty("password")
                redirectAndMagic["username"] = username
                redirectAndMagic["password"] = password
                return doLoginRequest(redirectAndMagic["submit"]!!, redirectAndMagic)
            }
            else {
                return LoginState.UNKNOWN;
            }
        }
    }
    private fun extractRedirectAndMagic(loginPortalDomain:String, html: String): MutableMap<String, String> {
        val regex = """<(form|input)[^>]*>""".toRegex()
        val matches = regex.findAll(html)
        var matchedTags = ""
        matches.forEach {
            matchedTags += it.value
        }
        val doc = Jsoup.parse(matchedTags)
        var res: MutableMap<String, String> = mutableMapOf()
        val forms = doc.select("form")
        for (form in forms) {
            val action = form.attr("action")
            val method = form.attr("method")
            val inputs = form.select("input")
            res["submit"] = loginPortalDomain + action
            Log.d("extractRedirectAndMagic", "Form action: $action, $method, $inputs")
            for (input in inputs) {
                val name = input.attr("name")
                val value = input.attr("value")
                if (name == "4Tredir" || name=="magic") {
                    res[name] = value
                }
            }

        }
        for ((key, value) in res) {
            Log.d("openLoginPortal", "$key: $value")
        }
        return res
    }

    private suspend fun doLoginRequest(URL: String, map: MutableMap<String, String>) : LoginState {
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
            val responseHtml = response.body?.string().toString()
            val status = extractLoginFailedState(responseHtml);
            Log.d("DoLoginRequest", status.toString())
            return status
            if (status == LoginState.UNKNOWN && nReTries > 0) {
                doLoginRequest(URL, map)
            } else if (nReTries == 0) {
                return LoginState.AUTHFAILED;
            }
            else {
                  return status;
            }
        }
        return LoginState.UNKNOWN;
    }

    private fun extractLoginFailedState(html: String) : LoginState {
        Log.d("ExtractLoginStatus", html)
        var maxConcurrentRegex = """Sorry, user&apos;s concurrent authentication is over limit""".toRegex()
        var authFailedRegex = """Firewall authentication failed. Please try again.""".toRegex()
        var successRegex  = """"http://172.16.222.1:1000/keepalive\?""".toRegex()
        if (successRegex.containsMatchIn(html)) {
            return LoginState.LOGGEDIN;
        }
        else if (maxConcurrentRegex.containsMatchIn(html)) {
            return LoginState.MAXCONCURRENT;
        }
        else if (authFailedRegex.containsMatchIn(html)) {
            return LoginState.AUTHFAILED;
        }
        return LoginState.UNKNOWN;
    }

    companion object {
        private fun extractLoginStatus(html: String): Boolean {
            val regex = """Firewall Authentication Keepalive Window""".toRegex()
            val matchResult = regex.find(html)
            return matchResult != null
        }

        private fun extractSuccess(html: String): Boolean {
            val regex = """You have successfully logged out""".toRegex()
            val matchResult = regex.find(html)
            return matchResult != null
        }


        suspend fun initiateLogout(): LoginState{
            //  handle sockettimeotuexcpetion
                val generateLogout = "http://172.16.222.1:1000/logout?"
                val captivePortalRequest = Request.Builder()
                    .url(generateLogout)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .addHeader("Accept", "text/html")
                    .get()
                    .build()
                val client = OkHttpClient()
                client.newCall(captivePortalRequest).execute().use { response ->
                    val logoutPageHtml = response.body?.string()
                    val isLoggedOut = extractSuccess(logoutPageHtml.toString())
                    Log.d("initiateLogout", isLoggedOut.toString())
                }
                return checkLoginStatus()
        }


        internal suspend fun checkLoginStatus(): LoginState {
            try {
                val generateLogout = "http://172.16.222.1:1000/keepalive?"
                val captivePortalRequest = Request.Builder()
                    .url(generateLogout)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .addHeader("Accept", "text/html")
                    .get()
                    .build()
                val client = OkHttpClient()
                client.newCall(captivePortalRequest).execute().use { response ->
                    val logoutPageHtml = response.body?.string()
                    if (extractLoginStatus(logoutPageHtml.toString()))
                        return LoginState.LOGGEDIN
                }
            } catch (e: IOException) {
                return LoginState.LOGGEDOUT
            }
            return LoginState.UNKNOWN;
        }
    }
}