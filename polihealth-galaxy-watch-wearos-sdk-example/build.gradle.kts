plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "kr.co.hconnect.polihealth_galaxy_watch_wearos_sdk_example"
    compileSdk = 36

    defaultConfig {
        applicationId = "kr.co.hconnect.polihealth_galaxy_watch_wearos_sdk_example"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":polihealth-galaxy-watch-wearos-sdk"))
    implementation(files("libs/samsung-health-sensor-api-1.4.1.aar"))

    // 측정한 센서 데이터를 폰으로 보내는 BLE Peripheral.
    // wearos SDK는 protobuf 직렬화까지만 하고 전송 수단은 정해주지 않으므로,
    // 소비 앱이 직접 붙인다. 폰측은 bluetooth-sdk-android-v2(Central)로 받는다.
    implementation("kr.co.hconnect:bluetooth-sdk-android-peripheral:1.0.1")

    implementation(libs.play.services.wearable)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
