import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream


class VPNService: VpnService() {
    private var vpnThread: Thread? = null
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        vpnThread = Thread { runVpn() }
        vpnThread?.start()
        return START_STICKY
    }

    private fun runVpn() {
        val builder = Builder()
            .addAddress("10.0.0.2", 32)  // Fake VPN IP
            .addRoute("0.0.0.0", 0)  // Route all traffic
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
}
