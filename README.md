# WiFi AutoLogin

## Overview
WiFi AutoLogin is a cross-platform background service with a minimal UI that automates logging into captive portal networks. The app supports Android, iOS, macOS, Linux, and Windows, leveraging platform-specific technologies for seamless authentication.

## Features
- Automatic login to configured WiFi networks with captive portals
- Runs as a background service with an optional menu bar/tray UI
- Secure credential storage using platform-specific keychain/secure storage
- Lightweight and efficient with minimal battery impact
- Supports multiple network profiles

## Supported Platforms
- **macOS**: Menu bar app with LaunchAgent for auto-start
- **iOS**: Background network handling with secure credential storage
- **Android**: Background service with persistent notifications
- **Windows**: System tray app with auto-start functionality
- **Linux**: System daemon with CLI configuration

## Technologies Used
- **SwiftUI** (macOS, iOS) for UI components
- **Kotlin** (Android) with WorkManager for background tasks
- **WinUI/WPF** (Windows) for system tray UI
- **GTK/QT** (Linux) for optional UI
- **Network frameworks** for monitoring WiFi status across platforms
- **Keychain/Encrypted Storage** for secure credential management

## Installation
### macOS
1. Download the latest release from [GitHub Releases](#).
2. Move the app to the `/Applications` folder.
3. (Optional) Enable auto-start on login:
   ```sh
   launchctl load ~/Library/LaunchAgents/com.yourdomain.wifiautologin.plist
   ```

### Windows
1. Download the installer from [GitHub Releases](#).
2. Run the installer and follow the setup instructions.
3. The app will run in the system tray.

### Linux
1. Download the package for your distribution.
2. Install using:
   ```sh
   sudo dpkg -i wifi-autologin.deb  # For Debian-based distros
   sudo rpm -i wifi-autologin.rpm   # For RedHat-based distros
   ```
3. Enable the daemon:
   ```sh
   systemctl --user enable wifi-autologin.service
   ```

### iOS & Android
1. Download from the App Store / Play Store (TBD, download from [GitHub Releases](#) for now).
2. Follow on-screen instructions to configure networks.

## Usage
1. Open the app or access it via the menu bar/system tray.
2. Add login credentials for networks with captive portals.
3. The app will automatically authenticate when connecting to a known network.

## Configuration
- Credentials are stored securely in platform-specific keychains.
- Network profiles can be managed via the UI.
- Logs are stored in platform-specific locations.

## Development
### Requirements
- macOS 14+ (Sonoma) / iOS 17+
- Android 10+
- Windows 10+
- Linux (Ubuntu 20.04+, Fedora 36+ recommended)
- Xcode 15+, Android Studio, Visual Studio, GCC/Clang

### Running the Project
```sh
git clone https://github.com/yourusername/wifi-autologin.git
cd wifi-autologin
```

For platform-specific builds, refer to the documentation in each subfolder.

## Roadmap
- [ ] android
- [ ] macos
- [ ] ios/ipados
- [ ] windows
- [ ] linux
- [ ] Support for additional authentication methods (OAuth, QR codes)
- [ ] iCloud/Google Drive sync for network profiles
- [ ] CLI version for automation across platforms

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing
Contributions are welcome! Please read our [Contributions Guide](CONTRIBUTING.md) before submitting a pull request. If you encounter any issues, feel free to open an issue in the repository.

## Acknowledgments
Special thanks to the open-source community for inspiration and resources.

