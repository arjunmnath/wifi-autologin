package com.arjunmnath.wifilogger.wifi
import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import com.arjunmnath.wifilogger.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Properties
import kotlin.collections.iterator

enum class LoginState { CONNECTED, LOGGEDIN, LOGGEDOUT, MAXCONCURRENT, AUTHFAILED, UNKNOWN, CREDUNAVAILABLE, WIFINOTCONNECTED, AVAILABLE}


class LoginHandler() {
    private var nReTries = 3
    private lateinit var network: Network
    private lateinit var context: LoginService

    // TODO: wont work if connected to vpns
    // TODO: IOEXception not catching (unexpected end of stream) (caused function unknown)
    // TODO: captive portal not responding

    constructor(context: LoginService) : this() {
        this.context = context
    }

    fun getWifiNetwork(context: Context): LoginState {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networks: Array<Network> = cm.allNetworks
        for (network in networks) {
            val networkCapabilities = cm.getNetworkCapabilities(network)
            Log.d("getWifiNetwork", "🔍 Network: $network | Capabilities: $networkCapabilities")
            when {
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val info = wifiManager.connectionInfo
                    val wifiSSID = info.ssid?.replace("\"", "")
                    val ssidRegex = "IIITKottayam".toRegex()
                    if (ssidRegex.containsMatchIn(wifiSSID.toString())) {
                        if (networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true) {
                            println("Captive Portal: $network")
                            this.network = network
                            return LoginState.AVAILABLE
                        }
                        else {
                            return LoginState.LOGGEDIN
                        }
                    }
                    else {
                        return LoginState.WIFINOTCONNECTED
                    }

                }
                else ->
                    println("Other Network: $network")
            }
        }
        return LoginState.WIFINOTCONNECTED
    }


    suspend fun get(urlString: String): String? {
        if (!::network.isInitialized) {
            Log.e("LoginHandler:get", "Network not initialized")
        }
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = network.openConnection(url) as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doInput = true
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    "Error: ${connection.responseCode}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Exception: ${e.message}"
            }
        }
    }


    suspend fun initiateLogin(): LoginState {
        val state = checkLoginStatus()
        val wifiStatus = getWifiNetwork(context);
        if (
            wifiStatus == LoginState.WIFINOTCONNECTED ||
            wifiStatus == LoginState.LOGGEDIN) {
            return  wifiStatus
        }
        if (state == LoginState.LOGGEDIN) {
            return state
        }
        val generateCaptive = "http://connectivitycheck.gstatic.com/generate_204"
        Log.d("handleCaptivePortal", "Captive portal request sent")
        val captivePortalHTML = get(generateCaptive)
        Log.d("handleCaptivePortal", captivePortalHTML.toString())
        val portalURL = extractLoginPortalURL(captivePortalHTML.toString())
        if (portalURL == null) {
            return LoginState.LOGGEDIN
        } else {
            return openLoginPortal(portalURL)
        }
        return LoginState.UNKNOWN
    }

    private fun extractLoginPortalURL(html: String): String? {
        val regex = """http:\/\/172\.16\.222\.1:1000\/fgtauth\?([a-fA-F0-9]+)""".toRegex()
        val matchResult = regex.find(html)
        if (matchResult != null) {
            val entireUrl = matchResult.value
            Log.d("getLoginPortalURL", entireUrl)
            return entireUrl
        } else {
            Log.d("getLoginPortalURL", "No match found")
            return null
        }
    }


    private suspend fun openLoginPortal(portalUrl: String): LoginState {
        val loginPortalHTML = get(portalUrl)
        Log.d("openLoginPortal", loginPortalHTML.toString())
        val domainRegex = """http?://([^/]+)""".toRegex()
        val domain = domainRegex.find(portalUrl)
        val redirectAndMagic =
            extractRedirectAndMagic(domain?.value.toString(), loginPortalHTML.toString())
        if (
            !redirectAndMagic["submit"].isNullOrEmpty() &&
            !redirectAndMagic["magic"].isNullOrEmpty() &&
            !redirectAndMagic["4Tredir"].isNullOrEmpty()
        ) {
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
            redirectAndMagic["4Tredir"] = "http://connectivitycheck.static.com"
            return doLoginRequest(redirectAndMagic["submit"]!!, redirectAndMagic)
        } else {
            return LoginState.UNKNOWN
        }

    }

    private fun extractRedirectAndMagic(
        loginPortalDomain: String,
        html: String
    ): MutableMap<String, String> {
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
                if (name == "4Tredir" || name == "magic") {
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
        if (!::network.isInitialized) {
            Log.e("LoginHandler:get", "Network not initialized")
        }
        nReTries--
        val loginPayload = FormBody.Builder()
        for ((key, value) in map) {
            loginPayload.add(key, value)
        }
        Log.d("DoLoginRequest", URL)
        val formBody = loginPayload.build()

        val loginPortalRequest = Request.Builder()
            .url(URL)
            .post(formBody)
            .build()

        val client = OkHttpClient.Builder()
            .socketFactory(network.socketFactory)
            .build()


        client.newCall(loginPortalRequest).execute().use { response ->
            val responseHtml = response.body?.string().toString()
            val status = extractLoginFailedState(responseHtml)
            Log.d("DoLoginRequest", status.toString())
            return status
            if (status == LoginState.UNKNOWN && nReTries > 0) {
                doLoginRequest(URL, map)
            } else if (nReTries == 0) {
                return LoginState.AUTHFAILED
            }
            else {
                return status
            }
        }
        return LoginState.UNKNOWN
    }

    private fun extractLoginFailedState(html: String): LoginState {
        Log.d("ExtractLoginStatus", html)
        var maxConcurrentRegex =
            """Sorry, user&apos;s concurrent authentication is over limit""".toRegex()
        var authFailedRegex = """Firewall authentication failed. Please try again.""".toRegex()
        var successRegex = """"http://172.16.222.1:1000/keepalive\?""".toRegex()
        if (successRegex.containsMatchIn(html)) {
            return LoginState.LOGGEDIN
        } else if (maxConcurrentRegex.containsMatchIn(html)) {
            return LoginState.MAXCONCURRENT
        } else if (authFailedRegex.containsMatchIn(html)) {
            return LoginState.AUTHFAILED
        }
        return LoginState.UNKNOWN
    }

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


        suspend fun initiateLogout(): LoginState {
            val wifiStatus = getWifiNetwork(context);
            if (
                wifiStatus == LoginState.WIFINOTCONNECTED ||
                wifiStatus == LoginState.LOGGEDIN) {
                Log.d("initiateLogout", wifiStatus.toString())
                return  wifiStatus
            }
            val generateLogout = "http://172.16.222.1:1000/logout?"
            val response = get(generateLogout)
            val logoutPageHtml = response.toString();
            val isLoggedOut = extractSuccess(logoutPageHtml.toString())
            Log.d("initiateLogout", isLoggedOut.toString())
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
            return LoginState.UNKNOWN
        }

}