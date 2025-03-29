package com.arjunmnath.wifilogger.wifi

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.net.wifi.WifiManager
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress


class VPNService : VpnService(), Runnable {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var running = true

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startVpn()
        Thread(this).start()
        return START_STICKY
    }

    private fun startVpn() {
        val builder = Builder()
            .setSession("MyVPN")
            .addAddress("10.0.0.2", 24)  // Virtual VPN IP
            .addRoute("0.0.0.0", 0)      // Capture all traffic

        vpnInterface = builder.establish()
    }

    override fun run() {
        val vpnInput = FileInputStream(vpnInterface!!.fileDescriptor)
        val vpnOutput = FileOutputStream(vpnInterface!!.fileDescriptor)

        val packet = ByteArray(32767) // Max possible packet size

        while (running) {
            val length = vpnInput.read(packet)
            if (length > 0) {
                forwardPacket(packet, length) // Custom function to forward packets
            }
        }
    }

    private fun forwardPacket(packet: ByteArray, length: Int) {
        try {
            val socket = DatagramSocket()
            val gatewayIp = getWifiGateway(this) ?: return

            val packetData = DatagramPacket(packet, length, InetAddress.getByName(gatewayIp), 53) // Example: DNS
            socket.send(packetData)
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        running = false
        vpnInterface?.close()
    }
    fun getWifiGateway(context: Context): String? {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcpInfo = wifiManager.dhcpInfo
        val gatewayIp = dhcpInfo.gateway
        return InetAddress.getByAddress(
            byteArrayOf(
                (gatewayIp and 0xFF).toByte(),
                (gatewayIp shr 8 and 0xFF).toByte(),
                (gatewayIp shr 16 and 0xFF).toByte(),
                (gatewayIp shr 24 and 0xFF).toByte()
            )
        ).hostAddress
    }
}

