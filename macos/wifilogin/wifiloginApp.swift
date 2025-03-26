import SwiftUI

@main
struct MenuBarApp: App {
    @StateObject private var statusItemController = StatusItemController()
    
    var body: some Scene {
        Settings {
            EmptyView()
        }
    }
}

class StatusItemController: ObservableObject {
    private var statusItem: NSStatusItem
    private var popover: NSPopover
    
    init() {
        statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
        popover = NSPopover()
        
        if let button = statusItem.button {
            button.image = NSImage(systemSymbolName: "wifi", accessibilityDescription: "WiFi Login")
            button.action = #selector(togglePopover(_:))
        }
        
        popover.contentViewController = NSHostingController(rootView: CaptiveLoginView())
    }
    
    @objc private func togglePopover(_ sender: AnyObject?) {
        if let button = statusItem.button {
            if popover.isShown {
                popover.performClose(sender)
            } else {
                popover.show(relativeTo: button.bounds, of: button, preferredEdge: .minY)
            }
        }
    }
}

struct CaptiveLoginView: View {
    @State private var username: String = ""
    @State private var password: String = ""
    @State private var rememberMe: Bool = false
    
    var body: some View {
        VStack(spacing: 10)
        {
            Text("WiFi Login")
                .font(.headline)
            TextField("Username", text: $username)
                .textFieldStyle(RoundedBorderTextFieldStyle())
            SecureField("Password", text: $password)
                .textFieldStyle(RoundedBorderTextFieldStyle())
            Toggle("Remember Me", isOn: $rememberMe)
            HStack {
                Button("Cancel") {
                    NSApplication.shared.terminate(nil)
                }
                .keyboardShortcut(.cancelAction)
                
                Button("Login") {
                    authenticateUser()
                }
                .keyboardShortcut(.defaultAction)
            }
        }
        .padding()
        .frame(width: 300)
    }
    
    func authenticateUser() {
        print("Logging in with username: \(username)")
    }
}

#Preview {
    CaptiveLoginView()
}
