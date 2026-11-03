plugins {
    id("com.android.application")
}

android {
    namespace = "pe.controlfinanciero.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "pe.controlfinanciero.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "2.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
