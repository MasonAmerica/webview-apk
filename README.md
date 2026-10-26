# WebView APK Example

This is a simple Android application that demonstrates how to use the WebView component to create an APK that launches directly to a specified URL. The app essentially wraps a web application in a native Android container, providing a seamless mobile experience.

## Features

- **WebView Integration**: Uses Android's WebView component to display web content
- **JavaScript Support**: Enabled for full web application functionality
- **Progress Indicator**: Shows loading progress while the web page loads
- **Back Navigation**: Handles back button presses to navigate within the WebView
- **Error Handling**: Displays error messages if the web page fails to load, and retries network errors until the page loads (useful for kiosks launched at boot before Wi-Fi connects)
- **Media Playback**: Allows media playback without requiring user gesture
- **Landscape Lock**: The activity is locked to landscape (`sensorLandscape`)
- **Kiosk (Lock Task) Mode**: `android:lockTaskMode="if_whitelisted"` makes the app enter lock task automatically whenever it launches, if a device policy controller has allowlisted the package (e.g. Mason `lock_task_packages`). Back then does nothing on the first page. If the package isn't allowlisted, the app behaves normally.

## How It Works

The app uses a `WebView` component that loads a specified URL when the app launches. The main functionality is implemented in `MainActivity.java`:

```java
// The URL to load when the app starts
startWebView("https://ap-iwa.com/ProMobileV3");
```

### Key Components

1. **WebView Setup**: The WebView is configured with JavaScript enabled and media playback support
2. **WebViewClient**: Handles URL loading, page completion, and error handling
3. **WebChromeClient**: Manages permissions for web content
4. **Progress Bar**: Shows loading progress to improve user experience

## Customization

### Changing the Target URL

To change the URL that the app loads, modify line 22 in `app/src/main/java/com/support/webapp_to_webview/MainActivity.java`:

```java
startWebView("https://your-custom-url.com"); // Replace with your desired URL
```

### Updating the App Icon

To customize the app icon, follow these steps:

1. **Prepare Your Icon**: Create your icon image in the following formats:
   - PNG format
   - Multiple sizes: 48x48, 72x72, 96x96, 144x144, 192x192, 512x512 pixels
   - Both square and round versions (for round icons)

2. **Replace Icon Files**: Replace the existing icon files in the following directories:
   - `app/src/main/res/mipmap-hdpi/` - 72x72 pixels
   - `app/src/main/res/mipmap-mdpi/` - 48x48 pixels
   - `app/src/main/res/mipmap-xhdpi/` - 96x96 pixels
   - `app/src/main/res/mipmap-xxhdpi/` - 144x144 pixels
   - `app/src/main/res/mipmap-xxxhdpi/` - 192x192 pixels

3. **File Names**: Use these naming conventions:
   - `ic_launcher.png` - Square icon
   - `ic_launcher_round.png` - Round icon
   - `ic_launcher_foreground.png` - Foreground icon (for adaptive icons)

4. **Update Manifest**: The app icon is referenced in `app/src/main/AndroidManifest.xml`:
   ```xml
   android:icon="@mipmap/ic_launcher"
   android:roundIcon="@mipmap/ic_launcher_round"
   ```

5. **Alternative Method**: You can also use Android Studio's built-in Asset Studio:
   - Right-click on `app/src/main/res/`
   - Select "New" → "Image Asset"
   - Choose "Launcher Icons"
   - Import your custom image
   - Generate the icons

### Changing the App Name

To change the app name displayed on the device, modify `app/src/main/res/values/strings.xml`:

```xml
<string name="app_name">Your Custom App Name</string>
```

### Changing the Screen Orientation

The app is locked to landscape in `app/src/main/AndroidManifest.xml`. `sensorLandscape` still flips 180° to match how the device is mounted:

```xml
android:screenOrientation="sensorLandscape"
```

Use `sensorPortrait` for portrait deployments, or remove the attribute to follow the device's rotation.

## Building the APK

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd webview-btm
   ```

2. **Open in Android Studio** or build from command line:
   ```bash
   ./gradlew assembleRelease
   ```

3. **Find the APK**: The built APK will be located at:
   ```
   app/build/outputs/apk/release/app-release.apk
   ```

## Requirements

- Android API level 21+ (Android 5.0+)
- Internet permission (already configured)
- Target URL must be accessible from the device

## Permissions

The app requires the following permissions:
- `INTERNET` - To load web content
- `ACCESS_NETWORK_STATE` - To retry loading the page as soon as the network connects

## Troubleshooting

- **Web page not loading**: Check your internet connection and ensure the target URL is accessible
- **JavaScript not working**: JavaScript is enabled by default, but some websites may require additional permissions
- **App crashes**: Ensure the target URL is valid and the device has internet connectivity

## License

This project is provided as an example for educational purposes.