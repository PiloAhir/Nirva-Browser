# Nirva Browser (નિર્વા બ્રાઉઝર)

Privacy-first Android WebView browser starter project. Build APK using GitHub Actions.

## Features
- Custom Nirva launcher icon and app label
- Selectable Google, Bing, Brave Search, or DuckDuckGo
- URL/search bar, back/forward/home, reload
- URL-based tab switcher (switching tabs reloads the URL; this is not full Chrome-style independent tab state)
- Bookmarks and recent history
- Limited tracker-host blocklist and disabled third-party cookies
- Private mode toggle (best-effort; not equivalent to Chrome Incognito)
- Desktop user-agent toggle
- Find text on page, share current URL
- DownloadManager support for ordinary HTTP/HTTPS downloads
- Clear history by time range; clear all WebView cookies/cache/site storage
- Android system-bar insets applied to keep controls below status/navigation bars

## Build
Push the complete folder to a GitHub repository. The workflow `.github/workflows/android.yml` builds `app-debug.apk` and uploads it as `Nirva-Browser-debug-apk`.

## Privacy limitations
This app uses Android System WebView; it is not Chromium/Chrome and has not been security-audited. The tracker blocklist is small and cannot block all trackers or ads. Search providers and websites can receive queries, IP address, and other normal connection metadata. Private mode is best-effort. Android force-stop may bypass lifecycle cleanup. Use a maintained, security-audited browser for high-risk activity.
