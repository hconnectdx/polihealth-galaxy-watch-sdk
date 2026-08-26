import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.protobuf)
    `maven-publish`
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(FileInputStream(file))
}

val libVersion = "1.1.0"

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                maybeCreate("java").apply {
                    option("lite")
                }
            }
        }
    }
}

android {
    namespace = "kr.co.hconnect.polihealth_galaxy_watch_android_sdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 29 // Samsung Health Data SDK 요구사항 (Android 10+). docs/samsung-health-integration.md 2절

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    api(libs.protobuf.javalite)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.datastore.preferences)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    implementation("kr.co.hconnect:bluetooth-sdk-android-v2:1.0.11")

    // Samsung Health Data API — 로컬 maven repo(libs/repo)에서 받아온다 (settings.gradle.kts 참고).
    // compileOnly가 아니라 implementation 이어야 한다: 컴파일 타임에만 있고 런타임에 없으면 클래스를 못 찾는다.
    // Health Data SDK 타입은 공개 API(PolihealthGalaxyWatchAndroidSdk)로 노출하지 않으므로 api가 아닌 implementation으로 캡슐화한다.
    implementation(libs.samsung.health.data.api)

    // ⚠️ health-data-api가 내부적으로 Gson에 의존한다(ReadDataRequest 등 Parcelable 직렬화에 사용).
    // 로컬 maven repo용 pom을 우리가 직접 작성한 최소 pom이라 이 전이 의존성이 선언돼 있지 않다 —
    // 없으면 실기기에서 NoClassDefFoundError: com.google.gson.GsonBuilder 로 readData() 계열 호출이 죽는다.
    implementation(libs.gson)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "kr.co.hconnect"
                artifactId = "polihealth-galaxy-watch-android-sdk"
                version = libVersion
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/hconnectdx/polihealth-galaxy-watch-sdk")
                credentials {
                    username = localProperties.getProperty("githubUsername") ?: System.getenv("GITHUB_USERNAME")
                    password = localProperties.getProperty("githubAccessToken") ?: System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
