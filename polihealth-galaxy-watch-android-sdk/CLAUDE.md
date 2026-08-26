# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 모듈 역할

폰 앱에서 Galaxy Watch와 BLE로 연결해 Protobuf 센서 데이터를 수신하고, CSV 파일로 저장 후 HealthOn 서버에 HTTP 업로드하는 Android 라이브러리.

## 공개 API

`PolihealthGalaxyWatchAndroidSdk` (object) — 유일한 공개 진입점.

```kotlin
// 1) 초기화 (start 전 필수)
PolihealthGalaxyWatchAndroidSdk.init(
    baseUrl      = "https://mapi-stg.health-on.co.kr/",
    clientId     = "...",
    clientSecret = "...",
    callback     = object : ServerSdkCallback { ... }
)

// 2) 사용자 정보 (protocol2-1 전송 시 사용)
PolihealthGalaxyWatchAndroidSdk.setHealthOnUser(userSno = 1234, userAge = 30)

// 3) 연결 시작 / 종료
PolihealthGalaxyWatchAndroidSdk.start(context)   // WatchReceiverService 포그라운드 시작 (API 26+)
PolihealthGalaxyWatchAndroidSdk.stop(context)    // BLE 해제 + 서비스 중지
```

### 삼성헬스 Data SDK 연동 (Galaxy Watch BLE와는 별개 기능)

삼성헬스 앱에 저장된 걸음수/심박수/산소포화도/피부온도를 직접 읽어온다. `init`/`start`와 무관하게 바로 사용 가능.
전부 `suspend fun`이며, 상세 배경은 [docs/samsung-health-integration.md](docs/samsung-health-integration.md) 참고.

```kotlin
val granted = PolihealthGalaxyWatchAndroidSdk.checkHealthDataPermission(context)   // Map<String, Boolean>
if (granted.values.any { !it }) {
    PolihealthGalaxyWatchAndroidSdk.requestHealthDataPermission(activity)
}
val steps = PolihealthGalaxyWatchAndroidSdk.readHealthDataSteps(context, days = 7)          // List<Map<String, Any>>
val heartRate = PolihealthGalaxyWatchAndroidSdk.readHealthDataHeartRate(context, days = 7)
val bloodOxygen = PolihealthGalaxyWatchAndroidSdk.readHealthDataBloodOxygen(context, days = 7)
val skinTemp = PolihealthGalaxyWatchAndroidSdk.readHealthDataSkinTemperature(context, days = 7)
```

삼성 기기가 아니거나 삼성헬스 미설치/미로그인 등 사전 조건 미충족 시 `health.HealthDataSdkException(code: HealthDataErrorCode, ...)`을 던진다.
`samsung-health-data-api` AAR은 파트너 개별 배포라 git에는 pom만 있다 — 실물 `.aar`(원본 파일명 `samsung-health-data-api-1.1.0.aar` 그대로)는 `polihealth-galaxy-watch-android-sdk/libs/repo/.../samsung-health-data-api/1.1.0/`에 직접 추가해야 빌드된다 (`polihealth-galaxy-watch-android-sdk/libs/repo/README.md` 참고).
내부 구현은 `health/` 패키지(`HealthDataStoreProvider`, `HealthDataPermissionManager`, `*HealthDataReader`, `HealthDataManager`)에 있으며 전부 `internal` — `PolihealthGalaxyWatchAndroidSdk`만 공개 API로 노출한다.

## 아키텍처

```
PolihealthGalaxyWatchAndroidSdk (object)
  └── WatchReceiverService (ForegroundService)
        ├── WatchFinder — 페어링된 Galaxy Watch 탐색 (이름 prefix "Galaxy Watch")
        ├── HCBle (bluetooth-sdk-android-v2) — BLE 연결 / MTU 512 협상 / NUS 알림
        ├── PacketReassembler — BLE 청크 조립 → SensorBufferProto or MeasurementType
        ├── SessionManager — 세션 생명주기, 샘플 라우팅, API 전송
        │     ├── DataWriter — CSV 파일 저장
        │     └── HealthOnClient (OkHttp) → Protocol2_1API / Protocol8_1API
        │                                 → SleepStartAPI / SleepStopAPI
        └── ServerSdkCallback — 앱에 이벤트 전달
```

## BLE 프레이밍 (PacketReassembler)

워치가 보내는 프레임 형식:
```
[ 4바이트 big-endian 전체 길이 ] [ 페이로드 ]
```

페이로드는 두 종류:
1. `SensorBufferProto` 바이너리 — 센서 샘플 배치
2. `MEASUREMENT_TYPE:<ECG|SLEEP|STOP>` UTF-8 텍스트 — 측정 시작/종료 알림

텍스트 알림은 `buffer` 상태와 무관하게 **항상 먼저** 판정한다 (Protobuf 조립 미완 상태에서도 SLEEP/STOP을 놓치지 않기 위해).

`bleDispatcher = Dispatchers.IO.limitedParallelism(1)` — BLE Notification 순서 보장을 위해 단일 스레드로 직렬 처리.

## 측정 타입과 API 매핑

| MeasurementType | 세션 종료 시 호출 API | sessionId 형식 |
|-----------------|----------------------|----------------|
| `ECG` (일상) | `POST /poli/day/protocol2-1` | `yyyyMMdd_HHmmss` |
| `SLEEP` (수면) | `POST /poli/sleep/protocol8-1` (1분 청크마다) | `yyyyMMdd_HHmmss` (15자 고정) |
| null (미수신) | protocol2-1 fallback | — |

수면 세션은 워치에서 1분마다 `TrackingState.FINISH`가 발생 → 청크 파일 전송 후 새 청크 시작.
수면 종료 시 `SleepStopAPI` 호출 후 마지막 청크 전송.

## DataWriter 저장 경로

```
Android/data/{packageName}/files/PolihealthGalaxyWatchAndroidSdk/   ← 앱 전용 외부 저장소, 권한 불필요
  └── 2026-04-29/
      └── 15_30_00/
          ├── ACC.csv         (x,y,z) - timestamp 제거
          ├── ECG.csv         (value)
          ├── PPG_GREEN_25.csv  (green,ir,red) - ir, green, red 순으로 서버에서 변경함
          └── PPG_GREEN_100.csv (green,ir,red)
```

외부 저장소가 마운트되지 않은 경우에만 앱 내부 저장소(`filesDir`)로 fallback.
Play 정책상 심사가 까다로운 `MANAGE_EXTERNAL_STORAGE`(전체 파일 접근) 권한은 사용하지 않는다 — 공용 `Download/` 폴더에는 쓰지 않는다.

## HealthOnClient 주의사항

`HttpLoggingInterceptor`는 **반드시 `HEADERS` 레벨만 사용**. `BODY` 레벨로 설정하면 대용량 멀티파트 업로드 시 logcat 출력이 Azure Gateway의 idle timeout(≈20초)을 초과해 504 오류 발생.

응답 바디는 내부 `responseBodyLogInterceptor`가 최대 2,000자까지만 출력.

## ServerSdkCallback 주요 메서드

| 메서드 | 호출 시점 |
|-------|----------|
| `onConnected(deviceName)` | BLE 연결 수립 |
| `onDisconnected()` | BLE 연결 해제 |
| `onTrackingStarted(sessionId)` | 측정 세션 시작 |
| `onTrackingFinished(sessionId)` | 측정 세션 종료 |
| `onMeasurementStarted(sessionId, type)` | `MEASUREMENT_TYPE:*` 텍스트 수신 시 |
| `onSensorData(sessionId, sensorType, samples)` | 샘플 배치 수신 |
| `onStoragePath(path)` | CSV 저장 경로 결정/해제 |
| `onSleepFinished(sessionId, sleepQuality)` | `/poli/sleep/stop` 응답 수신 |
| `onProtocol2_1Result(...)` | `/poli/day/protocol2-1` 전송 결과 |
| `onProtocol8_1Result(...)` | `/poli/sleep/protocol8-1` 전송 결과 |
| `onError(message)` | SDK 내부 오류 |
