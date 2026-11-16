plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "de.wochenplan.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "de.wochenplan.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
    }

    // Feste Signatur, damit Updates ohne Deinstallation installierbar sind.
    signingConfigs {
        getByName("debug") {
            storeFile = file("wochenplan.keystore")
            storePassword = "wochenplan"
            keyAlias = "wochenplan"
            keyPassword = "wochenplan"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
