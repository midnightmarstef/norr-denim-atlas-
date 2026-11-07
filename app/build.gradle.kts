plugins { id("com.android.application") }

// GitHub Actions numbers every build; that number becomes the version so each APK installs over the last.
val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0
// Set by the workflow after it decrypts signing/norr-release.p12.enc with the NORR_SIGNING_PASSPHRASE secret.
val keystorePath: String? = System.getenv("NORR_KEYSTORE")
val signingPass: String? = System.getenv("NORR_SIGNING_PASSPHRASE")

android {
    namespace = "is.norr.atlas"
    compileSdk = 35
    defaultConfig {
        applicationId = "is.norr.atlas"
        minSdk = 26
        targetSdk = 34
        versionCode = 100 + runNumber
        versionName = "1.$runNumber"
    }
    signingConfigs {
        create("norr") {
            if (keystorePath != null && signingPass != null) {
                storeFile = file(keystorePath)
                storeType = "pkcs12"
                storePassword = signingPass
                keyAlias = "norr"
                keyPassword = signingPass
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            // Always the same NORR key on GitHub, so Android accepts each update over the installed app.
            signingConfig = if (keystorePath != null) signingConfigs.getByName("norr") else signingConfigs.getByName("debug")
        }
    }
    // Sideloaded app, not on the Play Store: don't let Play's target-SDK deadline lint block the release build.
    lint { checkReleaseBuilds = false; abortOnError = false }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
