# NORR Denim Atlas — Android

A WebView shell around `app/src/main/assets/index.html` (the satellite edition of the atlas).
`docs/index.html` is the same atlas as an installable web page.

## Install
On the phone, open
https://github.com/midnightmarstef/norr-denim-atlas-/releases/latest/download/norr-atlas.apk
and tap the downloaded file.

## How updates work
1. Change the atlas (`app/src/main/assets/index.html`, and `docs/index.html` for the web version) and push to `main`.
2. **Build APK** runs on GitHub (about 3 minutes), numbers the build, signs it with the NORR key and replaces the `latest` release.
3. Next time the app opens it sees the newer build and offers **Download**. Tap the file to install over the old app; saved listings stay.

## Signing key
`signing/norr-release.p12.enc` is the app's signing key, encrypted (AES-256). The workflow unlocks it with the
repository secret `NORR_SIGNING_PASSPHRASE` (Settings → Secrets and variables → Actions). Without that secret the
workflow skips the build and leaves the current release in place.
Keep the passphrase somewhere safe as well: every update must be signed with this same key, or Android
will refuse to install it over the existing app.

## Build locally instead
Open this folder in Android Studio and choose **Build → Build APK(s)**. A local build uses the debug key,
so it can't be installed over a GitHub build (uninstall first).
