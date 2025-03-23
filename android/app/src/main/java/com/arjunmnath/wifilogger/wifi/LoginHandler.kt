package com.arjunmnath.wifilogger.wifi

import android.content.ContextWrapper
import android.util.Log
import com.arjunmnath.wifilogger.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.Properties
import kotlin.collections.iterator

enum class LoginState{ CONNECTED, LOGGEDIN, MAXCONCURRENT, AUTHFAILED, UNKNOWN }


class LoginHandler(private val context: LoginService) {
    private var nReTries = 3;

    fun initiateLogin() : LoginState{
            val generateCaptive= "http://connectivitycheck.gstatic.com/"
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

    private fun openLoginPortal(poralUrl: String) : LoginState {
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
                val properties = Properties().apply {
                    ContextWrapper(context).resources.openRawResource(R.raw.config).use { load(it) }
                }
                Log.d("openLoginPortal", properties.keys.toString())
                redirectAndMagic["username"] = properties.getProperty("username")
                redirectAndMagic["password"] =  properties.getProperty("password")
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

    private fun doLoginRequest(URL: String, map: MutableMap<String, String>) : LoginState {
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
            Log.d("DoLoginRequest", responseHtml)
            val keepaliveURL = extractKeepaliveURL(responseHtml)
            // TODO: analyse the resposne html and return maxconcurrent or authfailed accordingly
            if (keepaliveURL.isNullOrEmpty() && nReTries > 0) {
                doLoginRequest(URL, map)
            } else if (nReTries == 0) {
                return LoginState.AUTHFAILED;
            }
            else {
                return openKeepAlive(keepaliveURL.toString())
            }
        }
        return LoginState.UNKNOWN;
    }

    private fun extractKeepaliveURL(html: String): String? {
        val regex = """http:\/\/172\.16\.222\.1:1000\/keepalive\?([a-fA-F0-9]+)""".toRegex()
        val matchResult = regex.find(html)
        if (matchResult != null) {
            val entireUrl = matchResult.value
            Log.d("extractKeepaliveURL", entireUrl)
            return entireUrl;
        } else {
            Log.d("extractKeepaliveURL", "No match found")
            return null
        }
    }

    private fun openKeepAlive(keepaliveURL: String) : LoginState {
        val captivePortalRequest = Request.Builder()
            .url(keepaliveURL)
            .addHeader("User-Agent", "Mozilla/5.0")
            .addHeader("Accept", "text/html")
            .get()
            .build()
        val client = OkHttpClient()
        client.newCall(captivePortalRequest).execute().use { response ->
            if (response.code == 200) {
                return LoginState.LOGGEDIN
            }
            val captivePortalHTML = response.body?.string()
            // TODO: add a auth successfull check here and return unknown state if failed
            Log.d("handleCaptivePortal", captivePortalHTML.toString())
            return LoginState.LOGGEDIN
        }
    }
}