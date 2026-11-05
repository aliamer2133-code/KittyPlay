plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.kittyplay.toddlergame"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kittyplay.toddlergame"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // The online build supplies a private signing key, so each new build can
    // install over the last one and keep the settings. Without the key, a local
    // build falls back to Android's normal debug key.
    val keyPath = System.getenv("KITTYPLAY_KEYSTORE")
    signingConfigs {
        if (keyPath != null) {
            create("kittyplay") {
                storeFile = file(keyPath)
                storeType = "pkcs12"
                storePassword = System.getenv("KITTYPLAY_KEY_PASSWORD")
                keyAlias = "kittyplay"
                keyPassword = System.getenv("KITTYPLAY_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keyPath != null) {
                signingConfigs.getByName("kittyplay")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
}
