import java.io.FileInputStream
import java.util.Properties

include(":polihealth-galaxy-watch-wearos-sdk")


include(":polihealth-galaxy-watch-wearos-sdk-example")


include(":polihealth-galaxy-watch-android-sdk-example")


pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// local.properties 파일 읽기
var localProperties = Properties()
var localFile = File(rootDir, "local.properties")
if (localFile.exists()) {
    localProperties.load(FileInputStream(localFile))
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
//        maven { url 'https://jitpack.io' }
        maven {
            url = uri("https://maven.pkg.github.com/hconnectdx/bluetooth-sdk-android-v2")
            credentials {
                username = localProperties.getProperty("githubUsername")
                password = localProperties.getProperty("githubAccessToken")
            }
        }
        // Samsung Health Data API AAR (파트너 개별 배포, Maven Central 미공개).
        // dependencyResolutionManagement 가 FAIL_ON_PROJECT_REPOS 라서 모듈의 build.gradle.kts 에
        // repositories {} 를 선언할 수 없다 — 여기(settings.gradle.kts)에 로컬 maven repo로 등록한다.
        // 실제 .aar 파일은 polihealth-galaxy-watch-android-sdk/libs/repo/... 에 직접 배치해야 한다 (docs/samsung-health-integration.md 4절 참고).
        maven { url = uri(File(rootDir, "polihealth-galaxy-watch-android-sdk/libs/repo")) }
    }
}

rootProject.name = "PolihealthGalaxyWatchSDK"
include(":polihealth-galaxy-watch-android-sdk")
