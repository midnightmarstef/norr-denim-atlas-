# NORR Denim Atlas — Android

A WebView shell around `app/src/main/assets/index.html` (the satellite edition of the atlas).

## Get the APK
Every push to `main` runs **Build APK** (GitHub Actions), which publishes `norr-atlas.apk` as the
release tagged `latest`:

    https://github.com/midnightmarstef/norr-denim-atlas-/releases/latest/download/norr-atlas.apk

Open that link on the phone, download, tap the file, allow "install from this source" once.

## Build locally instead
Open this folder in Android Studio and choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
The APK lands in `app/build/outputs/apk/debug/`.

## Update the atlas
Replace `app/src/main/assets/index.html` (and `docs/index.html` for the web version), bump
`versionCode` in `app/build.gradle.kts`, push.
