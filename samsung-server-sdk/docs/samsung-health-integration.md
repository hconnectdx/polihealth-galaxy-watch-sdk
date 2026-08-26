# Samsung Health Data SDK 연동 가이드

> 이 문서는 `samsung-server-sdk.aar`에 삼성헬스(Samsung Health) Data SDK를 연동한 내용을 정리한 문서입니다.
> 연동 당시의 설계 결정, 설정 방법, 알아둬야 할 함정을 기록해두어, 이후 SDK 버전업이나 유지보수 시 다시
> 처음부터 조사하지 않아도 되도록 하는 것이 목적입니다.
>
> 마지막 갱신: `2026-08-20` / 작성자: `heegyu.yu` / 검증한 SDK 버전: `[1.1.0]`

## 1. 개요

- samsung-server-sdk 를 이용하는 앱에서 요청을 하면 삼성헬스 Data SDK를 이용해서 삼성헬스 앱에 저장된 걸음수, 심박수, 산소포화도, 피부온도를 가져온다.
- 연동 범위: 걸음수, 심박수, 산소포화도, 피부온도
- 연동 범위가 아닌 것: 걸음수, 심박수, 산소포화도, 피부온도 제외한 값은 연동 범위가 아님.

## 2. 전제조건 / 요구사항

- 삼성헬스 앱 최소 요구 버전 : 6.30.2 이후
- 삼성 기기 전용이며 삼성헬스 앱이 설치되어 있어야 함.
- `Android 10(API Level 29)` 이후


## 3. 아키텍처 개요

- samsung-sdk와 동일하게 samsung-health-data-api-1.1.0.aar파일은 별도 maven 저장소에서 받아 오지 않고 samsung-server-sdk.aar에 포함시킴.
- 이 라이브러리는 삼성헬스 데이타 읽기 권한이 있는지 여부를 확인하는 함수(checkPermission)와 권한이 없을 경우 삼성헬스 권한을 요청하는 함수를 구현(requestPermission) 해야 함
- checkPermission에는 삼성기기 여부와 삼성헬스 앱 설치 여부도 판단해서 별도 예외 코드를 정의 필요



## 4. Gradle / 빌드 설정

Samsung Health Data API AAR은 Maven Central에 공개되어 있지 않고, 파트너에게 개별 배포되는
`.aar` 파일 형태로 제공된다. AGP는 **라이브러리 모듈**(AAR로 패키징되는 모듈)에서
`files()`/`flatDir` 방식의 로컬 `.aar` 직접 의존을 금지하므로(`bundleDebugAar` 빌드 실패),
로컬 Maven 레이아웃(pom + aar)을 구성해서 GAV 좌표로 참조해야 한다.

> 대상 프로젝트가 **앱 모듈**(`com.android.application`)이라면 `files()`/`flatDir`로 바로 참조해도 
> 무방하다. 아래 로컬 maven repo 우회는 **라이브러리 모듈**에서만 필요하다.

### 4.1 로컬 Maven 레이아웃 구성

```
<module>/libs/repo/com/samsung/android/sdk/health/samsung-health-data-api/1.1.0
  ├── samsung-health-data-api-1.1.0.aar   ← 파트너가 배포하는 원본 파일명 그대로 (리네임 불필요)
  └── samsung-health-data-api-1.1.0.pom
```

> ⚠️ 버전 디렉터리명(`1.1.0`)은 pom의 `<version>`과 정확히 일치해야 한다 — 다르면 Gradle이
> `Could not find ....jar`로 GAV 좌표를 못 찾는다. artifactId도 파트너가 배포하는 실제 파일명
> (`samsung-health-data-api`)과 pom/의존성 선언에서 반드시 동일하게 써야 한다.

`samsung-health-data-api-1.1.0.pom` (최소 pom):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http:/
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.samsung.android.sdk.health</groupId>
    <artifactId>samsung-health-data-api</artifactId>
    <version>1.1.0</version>
    <packaging>aar</packaging>
</project>
```

### 4.2 build.gradle (라이브러리 모듈)

```groovy
// allprojects{} 클로저는 프로젝트마다 반복 실행되므로, 상대경로를 쓰
// 이 프로젝트 기준이 아니라 그때그때의 project.projectDir 기준으로
// 잘못 해석된다. 그래서 여기서 절대경로로 먼저 캡처해둔다.
def localRepoDir = file('libs/repo')


// ⚠️ 함정: Project.allprojects()는 "호출한 프로젝트 자신 + 그 하위 
// 대상으로 한다. 이 모듈이 하위 프로젝트가 없는 leaf 모듈이면 allpro
// 여기서 그냥 쓰면 자기 자신에게만 적용되고, 이 모듈을 참조하는 앱(:
// 프로젝트에는 전달되지 않아 :app 쪽 의존성 해석에서 repo가 안 보이
// 생긴다. rootProject.allprojects{}로 호출해야 멀티프로젝트 빌드 루
// (:app 포함 전체)에 적용된다.
rootProject.allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url uri(localRepoDir) }
    }
}


android {
    defaultConfig {
        minSdkVersion 29   // Samsung Health Data SDK 요구사항 (기존 
    }
}


dependencies {
    implementation 'com.samsung.android.sdk.health:samsung-health-data-api:1.1.0'
    // compileOnly 로 선언하면 실제 런타임에 클래스 못 찾음 — impleme
}
```

### 4.3 AndroidManifest.xml

```xml
<queries>
    <package android:name="com.sec.android.app.shealth" />
    <package android:name="com.samsung.android.wear.shealth" />
</queries>
```
---

## 5. 권한(Permission) 요청 패턴

```kotlin
class AuthSamsungManager(private val store: HealthDataStore) {


    suspend fun getCurrentAuth(): Set<Permission> {
        return store.getGrantedPermissions(permissions)
    }


    suspend fun requestAuth(activity: Activity): Set<Permission> {
        val granted = getCurrentAuth()
        if (granted.isNotEmpty()) return granted


        store.requestPermissions(permissions, activity)
        return getCurrentAuth()
    }


    fun extractPermissions(granted: Set<Permission>): Map<String, Boolean> {
        // 미승인 항목도 false로 명시적으로 내려줘야 클라이언트가 구분 가능
        return mapOf(
            "read_step" to granted.contains(Permission.of(DataTypes.STEPS, AccessType.READ)),
            "read_heartRate" to granted.contains(Permission.of(DataTypes.HEART_RATE, AccessType.READ)),
            "read_oxygenSaturation" to granted.contains(Permission.of(DataTypes.BLOOD_OXYGEN, AccessType.REA
            // ...
        )
    }


    companion object {
        val permissions: Set<Permission> = setOf(
            Permission.of(DataTypes.BLOOD_PRESSURE, AccessType.READ),
            Permission.of(DataTypes.BLOOD_GLUCOSE, AccessType.READ),
            Permission.of(DataTypes.HEART_RATE, AccessType.READ),
            Permission.of(DataTypes.BLOOD_OXYGEN, AccessType.READ),
            Permission.of(DataTypes.SLEEP, AccessType.READ),
            Permission.of(DataTypes.STEPS, AccessType.READ),
            Permission.of(DataTypes.EXERCISE, AccessType.READ),
            Permission.of(DataTypes.ACTIVITY_SUMMARY, AccessType.READ),
            Permission.of(DataTypes.NUTRITION, AccessType.READ),
        )
    }
}
```


> ⚠️ **파트너 정책(PartnerData_SHealth) 함정**: 요청 세트(`permissions`)에 삼성이 해당 앱/파트너에게
> 승인하지 않은 데이터 타입이 **하나라도** 섞여 있으면, `requestPermissions()`가 동의 화면조차
> 띄우지 않고 세트 전체를 `ERR_ACCESS_CONTROL`(2003)로 반려한다. 반드시 파트너 승인받은
> 타입만 요청 세트에 넣을 것 

## 6. 데이터 조회 공통 패턴

- 단건/시계열 조회: `DataTypes.X.readDataRequestBuilder` + `store.readData(request)`
- 합계만 필요(걸음수 등): `DataType.XType.TOTAL.requestBuilder` + `store.aggregateData(requ
  (`STEPS`는 `readData` 자체가 없고 aggregate만 지원)
- 시간 필터: `InstantTimeFilter.of(Instant, Instant)` 또는 `LocalTimeFilter.of(LocalDateTim
- 최대 30일, 하루 단위로 반복 조회하는 날짜 윈도우 방식을 권장 (커서 페이징 없음)


```kotlin
suspend fun readDays(days: Int): Map<String, Any> = withContext(Dispatchers.IO) {
    val targetDays = days.coerceIn(1, 30)
    val today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
    val zoneId = ZoneId.systemDefault()
    val data = (0 until targetDays).mapNotNull { i ->
        val from = today.minusDays(i.toLong())
        val to = from.plusDays(1).minusSeconds(1)
        readOneDay(from.atZone(zoneId).toInstant(), to.atZone(zoneId).toInstant())
    }.flatten()
    mapOf("data" to data)
}
```

## 7. 걸음 수 (Steps) — `aggregateData` 예시


`DataTypes.STEPS`는 `readData()`가 없다. `AggregateOperation`(TOTAL)만 제공되므로
`aggregateData()`로 하루 합계를 구한다.


```kotlin
class StepsSamsungManager(private val store: HealthDataStore) {


    suspend fun readDays(days: Int): Map<String, Any> = withContext(Dispatchers.IO)
        val targetDays = days.coerceIn(1, 30)
        val today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
        val data = (0 until targetDays).mapNotNull { i ->
            val from = today.minusDays(i.toLong())
            val to = from.plusDays(1).minusSeconds(1)
            readStepAggregate(from, to)
        }
        mapOf("type" to "step", "data" to data)
    }


    private suspend fun readStepAggregate(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Map<String, Any>? {
        val request = DataType.StepsType.TOTAL.requestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(startDateTime, endDateTime))
            .build()


        val response = store.aggregateData(request)
        val totalSteps = response.dataList.sumOf { (it.value as? Long) ?: 0L }


        if (totalSteps == 0L) return null
        return mapOf("date" to startDateTime.toString(), "step" to totalSteps.toInt
    }
}
```


## 8. 심박수 (Heart Rate) — `readData` 예시

```kotlin
class HeartRateSamsungManager(private val store: HealthDataStore) {


    private suspend fun readHeartRateData(
        startInstant: Instant,
        endInstant: Instant
    ): List<Map<String, Any>>? {
        val request = DataTypes.HEART_RATE.readDataRequestBuilder
            .setInstantTimeFilter(InstantTimeFilter.of(startInstant, endInstant))
            .build()


        val response = store.readData(request)
        if (response.dataList.isEmpty()) return null


        return response.dataList.map { point ->
            val bpm = (point.getValue(DataType.HeartRateType.HEART_RATE) as? Float)?.toInt()
            val minBpm = (point.getValue(DataType.HeartRateType.MIN_HEART_RATE) as? Float)?.toInt()
            val maxBpm = (point.getValue(DataType.HeartRateType.MAX_HEART_RATE) as? Float)?.toInt()


            buildMap<String, Any> {
                put("date", point.startTime.toString())
                bpm?.let { put("bpm", it) }
                minBpm?.let { put("minBpm", it) }
                maxBpm?.let { put("maxBpm", it) }
            }
        }
    }
}
```

## 8. 혈중 산소포화도 (Blood Oxygen) — `readData` 예시

```kotlin
class BloodOxygenSamsungManager(private val store: HealthDataStore) {


    private suspend fun readBloodOxygenData(
        startInstant: Instant,
        endInstant: Instant
    ): List<Map<String, Any>>? {
        val request = DataTypes.BLOOD_OXYGEN.readDataRequestBuilder
            .setInstantTimeFilter(InstantTimeFilter.of(startInstant, endInstant))
            .build()


        val response = store.readData(request)
        if (response.dataList.isEmpty()) return null


        return response.dataList.map { point ->
            val value = (point.getValue(DataType.BloodOxygenType.OXYGEN_SATURATION) as? Float)?.toDouble()
            val minValue = (point.getValue(DataType.BloodOxygenType.MIN_OXYGEN_SATURATION) as? Float)?.toDouble()
            val maxValue = (point.getValue(DataType.BloodOxygenType.MAX_OXYGEN_SATURATION) as? Float)?.toDouble()


            buildMap<String, Any> {
                put("date", point.startTime.toString())
                value?.let { put("value", it) }
                minValue?.let { put("minValue", it) }
                maxValue?.let { put("maxValue", it) }
            }
        }
    }
}
```



## 9. `getValue()` 반환 타입 — 실측 기반 주의사항


`DataPoint.getValue(field)`는 필드 타입에 따라 안전한 캐스팅이 필요하다. 문서상 타입과
실측이 다른 경우가 있었으므로 **반드시 실기기에서 로그로 실제 런타임 타입을 확인**할 것.


- `Float` 필드(대부분의 수치형: `HEART_RATE`, `OXYGEN_SATURATION`, `SYSTOLIC` 등) →
  `as? Float` 캐스팅. `as? Double`로 캐스팅하면 무조건 실패해서 값이 **조용히 드롭**된다.
- 실제로 겪은 버그: `DataType.BloodGlucoseType.GLUCOSE_LEVEL`을 문서 가정대로
  `List<BloodGlucose>`로 캐스팅했는데, 실기기 응답은 **단일 `Float`**(mmol/L)였다.
  `is List<*>` 캐스팅이 항상 실패해 혈당 데이터가 전량 드롭되는 버그로 이어졌다.
  → 방어 코드로 `when (rawValue) { is Number -> ...; is List<*> -> ...; else -> emptyList() }`
  형태로 두 케이스를 모두 처리하는 것을 권장.
- `Int` 필드(`PULSE_RATE` 등)는 `as? Int`.


```kotlin
val rawGlucose = point.getValue(DataType.BloodGlucoseType.GLUCOSE_LEVEL)
when (rawGlucose) {
    is Number -> rawGlucose.toDouble()          // 실측: 단일 Float로 온다
    is List<*> -> rawGlucose /* BloodGlucose 리스트 케이스 방어 */
    else -> null
}
```


---


## 10. 공용 유틸 (원본에서 사용한 확장 함수)


원본에서는 시각을 `"yyyyMMddHHmmssXXX"`(타임존 오프셋 포함) 문자열로 통일해서 내려준다.
필요 시 대상 프로젝트의 날짜 포맷 규칙에 맞게 교체하면 된다.


```kotlin
private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")


fun Instant.toFormattedString(): String {
    val zonedDateTime = this.atZone(ZoneId.systemDefault())
    return "${zonedDateTime.format(dateFormatter)}${zonedDateTime.offset}"
}


fun LocalDateTime.toFormattedString(): String {
    val zoneId = ZoneId.systemDefault()
    return "${this.format(dateFormatter)}${zoneId.rules.getOffset(this)}"
}


fun Double.toTwoDecimalPlace(): Double = String.format("%.2f", this).toDouble()
```


---


## 11. 테스트 방법

- Samsung Health SDK는 실제 삼성 기기 + 삼성헬스 앱 설치가 필요합니다
- 테스트용 실기기에 삼성헬스 계정으로 로그인되어 있어야함.
- 수동 테스트 이고 samsung-server-sdk-example에 검을수, 심박수, 산소포화도, 피부온도 정보를 가져와서 표시하는 화면 추가 필요.

## 12. 알려진 이슈 / 트러블슈팅

| 증상 | 원인 | 해결 방법 |
|---|---|---|
| `bundleDebugAar` 실패 (`repositories {}` 를 build.gradle.kts에 선언) | 이 repo의 `settings.gradle.kts`가 `repositoriesMode = FAIL_ON_PROJECT_REPOS`라, 4.2절 예시처럼 모듈 build.gradle.kts에 `rootProject.allprojects { repositories {...} }`를 쓰면 빌드가 막힌다 | 로컬 maven repo를 `settings.gradle.kts`의 `dependencyResolutionManagement.repositories`에 등록해야 한다 (`maven { url = uri(File(rootDir, "samsung-server-sdk/libs/repo")) }`) |
| `Could not find samsung-health-data-api-1.1.0.jar` (aar→jar 변환 실패) | 로컬 maven repo에 `.pom`만 있고 실물 `.aar`가 없음, 또는 artifactId/버전 디렉터리명이 pom과 불일치 | 파트너가 배포한 원본 파일명(`samsung-health-data-api-1.1.0.aar`) 그대로 `samsung-server-sdk/libs/repo/com/samsung/android/sdk/health/samsung-health-data-api/1.1.0/`에 추가. artifactId는 반드시 `samsung-health-data-api`로 통일(짧게 `health-data-api`로 줄이면 실제 배포 파일명과 안 맞아 리네임이 필요해짐 — 처음에 이 실수로 한 번 헤맴) |
| `Suspend function ... should be called only from a coroutine` | `HealthDataStore.readData()`/`aggregateData()`/`getGrantedPermissions()`/`requestPermissions()`는 모두 `suspend fun` — 이를 호출하는 내부 헬퍼 함수(`readOneDay` 등)도 반드시 `suspend`로 선언해야 함 | 호출 체인 전체를 `suspend fun`으로 선언하거나 `withContext`로 감쌀 것 |
| samsung-server-sdk 를 쓰는 앱이 `minSdkVersion X cannot be smaller than Y` 오류 | 이 라이브러리의 minSdk를 24 → 29로 올리면(2절 요구사항), 이 라이브러리를 쓰는 모든 앱도 minSdk를 29 이상으로 올려야 한다 (AGP가 강제) | `samsung-server-sdk-example`도 함께 minSdk 29로 올림. 앱마다 실제로 Android 10 미만 지원이 필요 없는지 먼저 확인할 것 |
| `ErrorCode.ERR_PLATFORM_NOT_INSTALLED` 등을 enum처럼 비교하면 타입 오류 | 실물 AAR의 `ErrorCode`는 enum이 아니라 `Int` 상수 모음이고, `HealthDataException.errorCode`도 `Int?`로 내려온다 (공식 가이드 문서 예시 코드가 enum처럼 보이게 축약되어 있어 헷갈리기 쉬움) | `when (errorCode: Int?) { ErrorCode.ERR_PLATFORM_NOT_INSTALLED -> ...; ... }` 형태로 비교할 것. 확실치 않은 API는 `.aar` 안의 `classes.jar`를 풀어 `javap -p`로 실제 시그니처를 확인하는 게 가장 빠르다 |
| 실기기에서 `readData()`류 호출 시 `NoClassDefFoundError: com.google.gson.GsonBuilder` (권한은 다 승인했는데도 발생) | `samsung-health-data-api` 내부적으로 Gson에 의존하는데(`ReadDataRequest.writeToParcel` 등), 우리가 로컬 maven repo용으로 직접 작성한 최소 pom에는 `<dependencies>`가 없어 이 전이 의존성이 전혀 선언돼 있지 않았음. ART가 한 번 클래스 초기화에 실패하면 그 프로세스 생명주기 동안 해당 클래스를 "이미 실패함"으로 캐싱해버려서, 이후 어떤 데이터 타입으로 재시도해도 (걸음수/심박수 포함) 같은 에러가 반복될 수 있음 | `samsung-server-sdk/build.gradle.kts`에 `implementation(libs.gson)`을 명시적으로 추가. 파트너 AAR을 우리가 만든 자체 pom으로 감쌀 때는, 그 AAR이 실제로 어떤 서드파티에 의존하는지 `classes.jar`를 풀어 `strings`로 훑어봐야 안전함(`cat $(find . -name '*.class') \| strings -a \| grep -E '^(com/google/\|okhttp3/\|retrofit2/)'` 형태) |

## 13. 버전 변경 이력

SDK 버전을 올리거나 지원 데이터 타입을 바꿀 때마다 이 표에 기록해두면, 다음 버전업 때 "저번엔 뭘 고쳤었지?"를 바로 확인할 수 있습니다.

| 날짜 | SDK 버전 | 변경 내용 | 관련 PR/이슈 |
|---|---|---|---|
| 2026-08-26 | 1.1.0 | 최초 연동. `checkHealthDataPermission`/`requestHealthDataPermission`/`readHealthDataSteps`/`readHealthDataHeartRate`/`readHealthDataBloodOxygen`/`readHealthDataSkinTemperature`를 `SamsungServerSdk`에 추가 (`health/` 패키지). samsung-server-sdk 모듈 minSdk 24→29 상향. 로컬 maven repo(`libs/repo`)에 실물 `samsung-health-data-api-1.1.0.aar` 추가 후 실제 컴파일/빌드 성공 확인(`javap`로 실제 API 시그니처 검증, `ErrorCode` Int 타입 이슈 수정) | - |

## 14. 참고 자료

- 삼성헬스 Data SDK 공식 문서 링크 : https://developer.samsung.com/health/data/overview.html
링크
