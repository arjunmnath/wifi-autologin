package com.arjunmnath.wifilogger.wifi

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.net.wifi.WifiManager
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream


class VPNService: VpnService() {
    private var vpnThread: Thread? = null
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("VPNService", "Received start id $startId: $intent")
        vpnThread = Thread { runVpn() }
        vpnThread?.start()
        return START_STICKY
    }
    private fun runVpn() {
        val gatewayIP = getWifiGatewayIP(this) ?: "172.16.222.1" // Default fallback
        Log.d("VPNService", "Gateway IP: $gatewayIP")
        val builder = Builder()
            .addAddress("10.0.0.2", 32)  // Fake VPN IP
            .addRoute(gatewayIP, 32)
            .addDnsServer("8.8.8.8")  // Google DNS as backup
            .addDnsServer("1.1.1.1")  // Cloudflare DNS
            .setSession("WifiVPN")

        vpnInterface = builder.establish()  // Creates the VPN tunnel

        vpnInterface?.let {
            val fd = it.fileDescriptor
            val buffer = ByteArray(32767)
            while (true) {
                try {
                    FileInputStream(fd).use { input ->
                        input.read(buffer)
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    override fun onDestroy() {
        vpnInterface?.close()
        vpnThread?.interrupt()
    }
    fun getWifiGatewayIP(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val dhcpInfo = wifiManager.dhcpInfo ?: return null
        return (dhcpInfo.gateway and 0xFF).toString() + "." +
                ((dhcpInfo.gateway shr 8) and 0xFF) + "." +
                ((dhcpInfo.gateway shr 16) and 0xFF) + "." +
                ((dhcpInfo.gateway shr 24) and 0xFF)
    }
}
