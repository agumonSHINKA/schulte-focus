# 舒爾特專注力（Android）

WebView wrapper around the Schulte PWA with a home-screen widget.

- Package: `io.github.agumonshinka.schulte`
- minSdk 24 / targetSdk 34
- Widget shows streak, today's best, games today, and launches a new game

## Build

```bash
export ANDROID_HOME=/path/to/sdk
# place keystore at keystore/schulte.jks and keystore/keystore.properties
./gradlew :app:assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`
