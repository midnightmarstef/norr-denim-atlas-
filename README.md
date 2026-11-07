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
Releases are signed with one permanent NORR key so each update installs over the last.
The key is created by the workflow on its first run and stored only encrypted (AES-256) as
`signing/norr-release.p12.enc`, locked with the repository secret `NORR_SIGNING_PASSPHRASE`
(Settings → Secrets and variables → Actions). Without that secret the workflow skips the build.
Keep the passphrase in your password manager too: if the secret is ever lost, updates can't be signed
with the same key and the app would have to be reinstalled.

## Build locally instead
Open this folder in Android Studio and choose **Build → Build APK(s)**. A local build uses the debug key,
so it can't be installed over a GitHub build (uninstall first).
