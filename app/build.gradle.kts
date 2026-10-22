plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mhttn.sunplusremote"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mhttn.sunplusremote"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    // libnative_lib.so is reused as-is from the original Ucast APK (arm64-v8a
    // + armeabi-v7a only, matching what was available to extract). Nothing
    // to build here - AGP packages src/main/jniLibs/<abi>/*.so automatically.

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
