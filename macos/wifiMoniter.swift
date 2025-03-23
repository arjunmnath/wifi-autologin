import Foundation
import CoreWLAN

class WiFiMonitor {
    private let client = CWWiFiClient.shared()
    private var interface: CWInterface? {
        return client.interface()
    }

    init() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(wifiStateChanged),
            name: NSNotification.Name.CWSSIDDidChange,
            object: nil
        )
    }

    @objc private func wifiStateChanged() {
        if let interface = interface {
            if let ssid = interface.ssid() {
                print("Connected to Wi-Fi: \(ssid)")
            } else {
                print("Wi-Fi disconnected")
            }
        } else {
            print("No Wi-Fi interface found")
        }
    }

    func startMonitoring() {
        print("Wi-Fi monitoring started...")
        wifiStateChanged() // Check initial state
    }
}

// Run the WiFiMonitor
let monitor = WiFiMonitor()
monitor.startMonitoring()

// Keep the script running (use only if running as a script)
RunLoop.main.run()
