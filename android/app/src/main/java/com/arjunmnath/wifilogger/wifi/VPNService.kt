import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class VPNService: VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("VPNService", "onStartCommand")
        setupVpn()
        return START_STICKY
    }

    private fun setupVpn() {
        val builder = Builder()
        builder.setSession("MyVPNService")
        builder.addAddress("10.0.0.2", 24)
        builder.addRoute("0.0.0.0", 0)
        builder.addDnsServer("8.8.8.8")

        vpnInterface = builder.establish()
        vpnThread = Thread { runVpn() }
        vpnThread?.start()
    }

    private fun runVpn() {
        vpnInterface?.let { vpnFd ->
            val inputStream = FileInputStream(vpnFd.fileDescriptor)
            val outputStream = FileOutputStream(vpnFd.fileDescriptor)
            val packet = ByteBuffer.allocate(32767)
            DatagramChannel.open().use { tunnel ->
                tunnel.connect(InetSocketAddress("172.16.222.1", 1000))
                while (!Thread.interrupted()) {
                    packet.clear()
                    val length = inputStream.read(packet.array())
                    if (length > 0) {
                        packet.limit(length)
                        tunnel.write(packet)
                        packet.clear()
                        val respLength = tunnel.read(packet)
                        if (respLength > 0) {
                            outputStream.write(packet.array(), 0, respLength)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        vpnThread?.interrupt()
        vpnInterface?.close()
        vpnInterface = null
        super.onDestroy()
    }
}
