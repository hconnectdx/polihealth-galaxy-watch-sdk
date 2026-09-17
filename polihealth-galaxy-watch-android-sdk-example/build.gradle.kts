import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(FileInputStream(file))
}

/**
 * 서버 접속 정보는 소스에 두지 않는다 — local.properties(git 제외)나 환경변수에서 읽는다.
 *
 * local.properties 예시:
 *   exampleApiUrl=https://your-server.example.com/
 *   exampleClientId=<발급받은 ClientId>
 *   exampleClientSecret=<발급받은 ClientSecret>
 *
 * 값이 없으면 빈 문자열로 빌드된다 — 빌드는 통과하고 실행 시 SDK 초기화에서 막힌다.
 */
fun serverConfig(propKey: String, envKey: String, default: String = ""): String =
    localProperties.getProperty(propKey) ?: System.getenv(envKey) ?: default

android {
    namespace = "kr.co.hconnect.polihealth_galaxy_watch_android_sdk_example"
    compileSdk = 36

    defaultConfig {
        applicationId = "kr.co.hconnect.polihealth_galaxy_watch_android_sdk_example"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_URL",
            "\"${serverConfig("exampleApiUrl", "EXAMPLE_API_URL")}\"")
        buildConfigField("String", "CLIENT_ID",
            "\"${serverConfig("exampleClientId", "EXAMPLE_CLIENT_ID")}\"")
        buildConfigField("String", "CLIENT_SECRET",
            "\"${serverConfig("exampleClientSecret", "EXAMPLE_CLIENT_SECRET")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // release 전용 서버를 따로 쓸 때만 지정한다. 없으면 위 defaultConfig 값을 그대로 쓴다.
            serverConfig("exampleReleaseApiUrl", "EXAMPLE_RELEASE_API_URL").takeIf { it.isNotEmpty() }
                ?.let { buildConfigField("String", "API_URL", "\"$it\"") }
            serverConfig("exampleReleaseClientId", "EXAMPLE_RELEASE_CLIENT_ID").takeIf { it.isNotEmpty() }
                ?.let { buildConfigField("String", "CLIENT_ID", "\"$it\"") }
            serverConfig("exampleReleaseClientSecret", "EXAMPLE_RELEASE_CLIENT_SECRET").takeIf { it.isNotEmpty() }
                ?.let { buildConfigField("String", "CLIENT_SECRET", "\"$it\"") }
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
        buildConfig = true
    }
}

dependencies {
    implementation(project(":polihealth-galaxy-watch-android-sdk"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}