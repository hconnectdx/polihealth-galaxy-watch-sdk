# Samsung Health Data SDK 연동 가이드

> 이 문서는 `samsung-server-sdk.aar`에 삼성헬스(Samsung Health) Data SDK를 연동한 내용을 정리한 문서입니다.
> 연동 당시의 설계 결정, 설정 방법, 알아둬야 할 함정을 기록해두어, 이후 SDK 버전업이나 유지보수 시 다시
> 처음부터 조사하지 않아도 되도록 하는 것이 목적입니다.
>
> 마지막 갱신: `2026-08-20` / 작성자: `heegyu.yu` / 검증한 SDK 버전: `[1.1.0]`

## 1. 개요

- samsung-server-sdk 를 이용하는 앱에서 요청을 하면 삼성헬스 Data SDK를 이용해서 삼성헬스 앱에 저장된 걸음수, 심박수, 산소포화도, 피부온도를 가져와서 SamsungServerSdk.init의 API url 즉 폴리헬스 서버로 데이타를 저장하는 기능을 추가한다.
- 연동 범위: 걸음수, 심박수, 산소포화도, 피부온도
- 연동 범위가 아닌 것: 혈압, 스트레스, 혈당변이도, 수면 데이터는 samsung-sdk 를 이용해서 수집한 ECG, PPG, ACC 데이타를 BLE로 전송하고 polihealth-android 앱에서 samsung-server-sdk가 데이타를 파일로 저장한 후 polihealth 파일을 전송하면 혈압, 스트레스, 혈당 변이도, 수면 점수 등을 알려준다. 

## 2. 전제조건 / 요구사항

- 삼성헬스 앱 최소 요구 버전 : 6.30.2 이후
- 삼성 기기 전용이며 삼성헬스 앱이 설치되어 있어야 함.
- `Android 10(API Level 29)` 이후


## 3. 아키텍처 개요

- samsung-sdk와 동일하게 samsung-health-data-api-1.1.0.aar파일은 별도 maven 저장소에서 받아 오지 않고 samsung-server-sdk.aar에 포함시킴.
- 이 라이브러리를 사용하는 앱에서 삼성헬스 데이타 읽기 권한이 있는지 여부를 확인하는 함수와 권한이 없을 경우 삼성헬스 권한을 요청하는 함수를 구현
- sync 함수를 호출하면 비동기로 걸음수, 산소포화도, 피부온도는 과거 30일치 데이타를 폴리헬스 서버로 전송하고 심박수의 경우 최초 1 호출시에 30일치 데이타를 읽어와서 호출하고 이후에는 sync 호출 시간을 기록해두었다가 sync 호출된 시점 이후 데이타만 읽어와서 폴리헬스 서버로 전송하도록 구현

## 4. Gradle / 빌드 설정

Samsung Health Data API AAR은 Maven Central에 공개되어 있지 않고, 파트너에게 개별 배포되는
`.aar` 파일 형태로 제공된다. AGP는 **라이브러리 모듈**(AAR로 패키징되는 모듈)에서
`files()`/`flatDir` 방식의 로컬 `.aar` 직접 의존을 금지하므로(`bundleDebugAar` 빌드 실패),
로컬 Maven 레이아웃(pom + aar)을 구성해서 GAV 좌표로 참조해야 한다.

> 대상 프로젝트가 **앱 모듈**(`com.android.application`)이라면 `files()`/`flatDir`로 바로 참조해도 
> 무방하다. 아래 로컬 maven repo 우회는 **라이브러리 모듈**에서만 필요하다.

### 4.1 로컬 Maven 레이아웃 구성

```
<module>/libs/repo/com/samsung/android/sdk/health/health-data-api/1.1
  ├── health-data-api-1.1.0.aar
  └── health-data-api-1.1.0.pom
```

`health-data-api-1.1.0.pom` (최소 pom):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http:/
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.samsung.android.sdk.health</groupId>
    <artifactId>health-data-api</artifactId>
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
    implementation 'com.samsung.android.sdk.health:health-data-api:1.
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


## 8. 걸음 수 (Steps) — `aggregateData` 예시

## 8. 걸음 수 (Steps) — `aggregateData` 예시

## 7. 에러 / 예외 상황 처리

- 삼성헬스 앱이 설치되어 있지 않은 경우
- 삼성헬스 앱 버전이 낮아 SDK 요구사항을 만족하지 못하는 경우
- 사용자가 권한을 거부한 경우
- 네트워크/동기화 지연으로 데이터가 아직 반영되지 않은 경우
- 그 외 SDK에서 발생했던 예외와 대응 방법 (실제로 겪었던 에러 메시지를 그대로 남겨두면 검색하기 좋습니다)

## 8. 테스트 방법

- Samsung Health SDK는 실제 삼성 기기 + 삼성헬스 앱 설치가 필요합니다
- 테스트용 실기기에 삼성헬스 계정으로 로그인되어 있어야함.
- 수동 테스트 이고 samsung-server-sdk-example에 검을수, 심박수, 산소포화도, 피부온도 정보를 가져와서 표시하는 화면 추가 필요.

## 9. 알려진 이슈 / 트러블슈팅

| 증상 | 원인 | 해결 방법 |
|---|---|---|
| | | |

## 10. 버전 변경 이력

SDK 버전을 올리거나 지원 데이터 타입을 바꿀 때마다 이 표에 기록해두면, 다음 버전업 때 "저번엔 뭘 고쳤었지?"를 바로 확인할 수 있습니다.

| 날짜 | SDK 버전 | 변경 내용 | 관련 PR/이슈 |
|---|---|---|---|
| | | | |

## 11. 참고 자료

- 삼성헬스 Data SDK 공식 문서 링크 : https://developer.samsung.com/health/data/overview.html
링크
