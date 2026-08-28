# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 모듈 역할

삼성헬스(Samsung Health) 앱에 저장된 걸음수/심박수/산소포화도/피부온도를 Samsung Health Data SDK로
직접 읽어오는 Android 라이브러리. Galaxy Watch BLE 연동(`polihealth-galaxy-watch-android-sdk`)과는
완전히 별개이며, 서버 업로드 기능은 없다 — 순수하게 삼성헬스 데이터를 읽어서 Kotlin 값으로 반환한다.

## 공개 API

`PolihealthSamsungHealthDataAndroidSdk` (object) — 유일한 공개 진입점. 전부 `suspend fun`.

```kotlin
val granted = PolihealthSamsungHealthDataAndroidSdk.checkHealthDataPermission(context)   // Map<String, Boolean>
if (granted.values.any { !it }) {
    PolihealthSamsungHealthDataAndroidSdk.requestHealthDataPermission(activity)
}
val steps = PolihealthSamsungHealthDataAndroidSdk.readHealthDataSteps(context, days = 7)          // List<Map<String, Any>>
val heartRate = PolihealthSamsungHealthDataAndroidSdk.readHealthDataHeartRate(context, days = 7)
val bloodOxygen = PolihealthSamsungHealthDataAndroidSdk.readHealthDataBloodOxygen(context, days = 7)
val skinTemp = PolihealthSamsungHealthDataAndroidSdk.readHealthDataSkinTemperature(context, days = 7)
```

삼성 기기가 아니거나 삼성헬스 미설치/미로그인 등 사전 조건 미충족 시 `HealthDataSdkException(code: HealthDataErrorCode, ...)`을 던진다.

**에러 처리 설계 원칙**: 라이브러리는 `HealthDataErrorCode`(구조화된 원인 코드)와
`HealthDataErrorCode.describe()`(개발자용 로그 메시지)까지만 책임진다. 최종 사용자에게 보여줄
안내 문구/UI(다이얼로그, "삼성헬스 앱 열기" 버튼 등)는 이 라이브러리를 쓰는 앱이 각자의 UX에 맞게
설계해야 한다 — 코드별 의미와 앱이 취해야 할 권장 조치는
[docs/samsung-health-integration.md](docs/samsung-health-integration.md) 9-1절 표 참고.

## 아키텍처

```
PolihealthSamsungHealthDataAndroidSdk (object)
  └── HealthDataManager (internal)
        ├── HealthDataStoreProvider — 삼성 기기/삼성헬스 설치 여부 확인 + HealthDataStore 연결
        ├── HealthDataPermissionManager — 권한 확인/요청
        └── *HealthDataReader (Steps/HeartRate/BloodOxygen/SkinTemperature) — 실제 데이터 조회
```

전부 `internal` — `PolihealthSamsungHealthDataAndroidSdk`만 공개 API로 노출한다.

## Samsung Health Data API AAR

파트너 개별 배포라 git에는 pom만 있다 — 실물 `.aar`(원본 파일명 `samsung-health-data-api-1.1.0.aar` 그대로)는
`polihealth-samsung-health-data-android-sdk/libs/repo/.../samsung-health-data-api/1.1.0/`에 직접 추가해야 빌드된다
(`libs/repo/README.md` 참고).

## 주의사항

- `minSdk = 29` — Samsung Health Data SDK 요구사항(Android 10+). 이 모듈을 쓰는 앱도 minSdk 29 이상이어야 한다.
- `implementation(libs.gson)` 필수 — `samsung-health-data-api`가 내부적으로 Gson에 의존하는데 로컬 pom에는
  전이 의존성이 선언되어 있지 않다. 빠뜨리면 실기기에서 `NoClassDefFoundError: com.google.gson.GsonBuilder`.
- `AndroidManifest.xml`의 `<queries>`(`com.sec.android.app.shealth`, `com.samsung.android.wear.shealth`)는
  삼성헬스 설치 여부 확인(`PackageManager#getPackageInfo`)에 필수 — Android 11+ 패키지 가시성 정책.
- 상세 배경/트러블슈팅은 [docs/samsung-health-integration.md](docs/samsung-health-integration.md) 참고.
