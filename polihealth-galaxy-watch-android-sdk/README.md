# polihealth-galaxy-watch-android-sdk

갤럭시워치가 BLE로 보낸 생체 센서 데이터를 **휴대폰에서 수신 · 복원 · 서버 업로드**하는 SDK입니다.

```
[갤럭시워치]  ──BLE──▶  [휴대폰: 이 SDK]  ──HTTPS──▶  [서버]
 센서 측정                패킷 재조립                  CSV 수신
 protobuf 직렬화          protobuf 디코드
                         CSV 변환
```

> **이 SDK만으로는 동작하지 않습니다.**
> 워치에 짝이 되는 앱이 필요합니다 → [`polihealth-galaxy-watch-wearos-sdk`](../polihealth-galaxy-watch-wearos-sdk)

---

## 목차

1. [시작 전 준비물](#1-시작-전-준비물)
2. [설치](#2-설치)
3. [권한](#3-권한)
4. [설정값 — init 파라미터](#4-설정값--init-파라미터)
5. [함수 레퍼런스](#5-함수-레퍼런스)
6. [이벤트 — ServerSdkCallback](#6-이벤트--serversdkcallback)
7. [데이터 구조](#7-데이터-구조)
8. [전체 예제](#8-전체-예제)
9. [서버로 나가는 요청](#9-서버로-나가는-요청)
10. [자주 막히는 지점](#10-자주-막히는-지점)

---

## 1. 시작 전 준비물

| 항목 | 요구사항 |
|---|---|
| Android | **8.0 (API 26) 이상** — `start()`가 포그라운드 서비스를 씁니다 |
| 기기 | 갤럭시워치와 페어링 가능한 안드로이드폰 (제조사 무관) |
| 워치 앱 | `polihealth-galaxy-watch-wearos-sdk`로 만든 앱이 워치에 설치돼 있어야 함 |
| 서버 | [9절](#9-서버로-나가는-요청)의 엔드포인트 4개를 받아줄 서버 |
| 인증 | 서버에서 발급한 `ClientId` / `ClientSecret` |

삼성 파트너십 승인은 **필요 없습니다.** 이 SDK는 삼성 코드를 쓰지 않습니다 —
워치에서 이미 protobuf로 직렬화돼 넘어온 바이트를 받을 뿐입니다.
(삼성 승인이 필요한 쪽은 워치측 SDK입니다.)

---

## 2. 설치

받는 방법이 두 가지고, **난이도가 많이 다릅니다.** 가능하면 A를 쓰세요.

### 방법 A — GitHub Packages (권장)

`local.properties`에 발급받은 자격증명을 넣고:

```properties
githubUsername=<사용자명>
githubAccessToken=<read:packages 권한 토큰>
```

저장소를 등록한 뒤:

```kotlin
// settings.gradle.kts 또는 build.gradle.kts
maven {
    url = uri("https://maven.pkg.github.com/hconnectdx/polihealth-galaxy-watch-sdk")
    credentials {
        username = localProperties.getProperty("githubUsername")
        password = localProperties.getProperty("githubAccessToken")
    }
}
```

**한 줄이면 끝납니다.**

```kotlin
dependencies {
    implementation("kr.co.hconnect:polihealth-galaxy-watch-android-sdk:1.0.0")
}
```

POM이 함께 배포되므로 `bluetooth-sdk-android-v2`를 포함한 **전이 의존성이 전부
자동으로 따라옵니다.** 아래 목록을 손으로 적을 필요가 없습니다.

### 방법 B — AAR 파일만 받은 경우

자격증명을 받을 수 없을 때만 쓰세요. `.aar`을 `app/libs/`에 넣고:

```kotlin
dependencies {
    implementation(files("libs/polihealth-galaxy-watch-android-sdk-1.0.0.aar"))
    implementation(files("libs/bluetooth-sdk-android-v2-1.0.11.aar"))   // 필수 — 아래 설명

    // ⚠️ AAR 단독에는 POM이 없어 전이 의존성이 따라오지 않습니다.
    //    아래를 전부 직접 선언해야 합니다. (SDK 빌드 시점의 실제 버전)
    implementation("androidx.core:core-ktx:1.18.0")          // ⚠️ 아래 호환성 주의
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("com.google.protobuf:protobuf-javalite:3.25.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
```

**`bluetooth-sdk-android-v2`는 선택이 아닙니다.** 이 SDK가 BLE 스캔과 연결을
그쪽에 맡기고 있어서, 빠지면 런타임에 `NoClassDefFoundError`가 납니다.

### ⚠️ `androidx.core` 1.18.0 호환성

이 SDK는 `androidx.core:core-ktx:1.18.0`을 끌어옵니다. 그런데 **1.18.0은
`compileSdk 36` + `AGP 8.9.1` 이상을 요구합니다.**

낮은 환경이라면 빌드가 이런 식으로 깨집니다:

```
Dependency 'androidx.core:core:1.18.0' requires libraries and applications that
depend on it to compile against version 36 or later of the Android APIs.
```

compileSdk를 올릴 수 없다면 **버전을 낮춰 고정**하세요:

```kotlin
configurations.all {
    resolutionStrategy {
        force("androidx.core:core:1.13.1")
        force("androidx.core:core-ktx:1.13.1")
    }
}
```

실제로 운영 중인 앱이 이 방식으로 `compileSdk 35` / `AGP 8.6.0` 환경에서 쓰고 있습니다.

### protobuf 버전을 워치와 맞추세요

`protobuf-javalite`가 `implementation`이 아니라 **`api`로 노출**됩니다.
워치 앱과 폰 앱이 서로 다른 protobuf 버전을 쓰면 **워치가 직렬화한 데이터를 폰이 못 풉니다.**
양쪽 모두 같은 버전으로 고정하세요.

---

## 3. 권한

### 매니페스트 — 추가 작업 없음

필요한 권한은 SDK의 `AndroidManifest.xml`에 이미 선언돼 있고, 매니페스트 병합으로
앱에 자동 포함됩니다. **앱 매니페스트에 다시 적지 않아도 됩니다.**

<details>
<summary>포함되는 권한 전체 보기</summary>

```xml
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />

<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

`WatchReceiverService`(`foregroundServiceType="connectedDevice"`)도 함께 선언됩니다.
</details>

### 런타임 요청 — 앱이 직접 해야 함

`start()`를 부르기 **전에** 아래를 받아두세요. 안 받으면 스캔이 조용히 0건을 반환합니다.

| 권한 | 대상 |
|---|---|
| `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` | Android 12 (API 31) 이상 |
| `ACCESS_FINE_LOCATION` | Android 11 (API 30) 이하 |
| `POST_NOTIFICATIONS` | Android 13 (API 33) 이상 — 포그라운드 서비스 알림용 |

```kotlin
val permissions = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_SCAN)
        add(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}
requestPermissions(permissions.toTypedArray(), REQ_CODE)
```

---

## 4. 설정값 — `init` 파라미터

```kotlin
PolihealthGalaxyWatchAndroidSdk.init(
    baseUrl:          String,
    clientId:         String,
    clientSecret:     String,
    callback:         ServerSdkCallback,
    connectTimeoutMs: Long = 15_000L,
    readTimeoutMs:    Long = 120_000L,
    writeTimeoutMs:   Long = 120_000L,
)
```

| 파라미터 | 필수 | 기본값 | 설명 |
|---|:---:|---|---|
| `baseUrl` | ✓ | — | 서버 주소. **끝에 `/`를 붙이든 안 붙이든** 내부에서 정규화합니다. 예: `https://api.example.com/` |
| `clientId` | ✓ | — | 모든 요청의 `ClientId` 헤더로 들어갑니다 |
| `clientSecret` | ✓ | — | 모든 요청의 `ClientSecret` 헤더로 들어갑니다 |
| `callback` | ✓ | — | 연결·측정·오류 이벤트 수신자 → [6절](#6-이벤트--serversdkcallback) |
| `connectTimeoutMs` | | 15초 | 서버 연결 타임아웃 |
| `readTimeoutMs` | | 120초 | 응답 대기. CSV 업로드 후 서버 처리 시간을 고려해 깁니다 |
| `writeTimeoutMs` | | 120초 | 요청 전송. 대용량 멀티파트를 고려해 깁니다 |

> 타임아웃 기본값을 줄이지 마세요. 수면 측정은 한 번에 수 MB를 올립니다.

### 사용자 정보

```kotlin
PolihealthGalaxyWatchAndroidSdk.setHealthOnUser(userSno = 1234, userAge = 30)
```

`userSno`(사용자 식별자)와 `userAge`는 측정 데이터 전송 시 함께 나갑니다.
`init()` 이후 **언제든 바꿀 수 있습니다** — 로그인 계정이 바뀌면 다시 부르세요.

---

## 5. 함수 레퍼런스

진입점은 `PolihealthGalaxyWatchAndroidSdk` object 하나입니다. 공개 함수는 6개입니다.

| 함수 | 반환 | 설명 |
|---|---|---|
| `init(...)` | `Unit` | 서버 정보와 콜백 등록. **`start()` 전에 반드시 1회** |
| `setHealthOnUser(userSno, userAge)` | `Unit` | 사용자 정보 설정 · 변경 |
| `start(context)` | `Unit` | 워치 탐색 → BLE 연결 → 수신 시작 |
| `stop(context)` | `Unit` | BLE 해제 + 서비스 종료 |
| `stopSleepMeasurement(context)` | `Unit` | 워치에 수면측정 종료 명령 전송 |
| `isRunning()` | `Boolean` | 실행 중 여부 |

### `start(context)`

`WatchReceiverService`를 포그라운드 서비스로 띄웁니다. 앱이 백그라운드로 가도
연결이 유지됩니다.

- **Android 8.0 (API 26) 이상 필요** (`@RequiresApi(O)`)
- `init()`을 안 부르고 호출하면 **`IllegalStateException`**

```kotlin
PolihealthGalaxyWatchAndroidSdk.start(context)
// → 이후 onConnected(deviceName) 콜백이 옵니다
```

### `stop(context)`

BLE 연결을 끊고 서비스를 내립니다. 진행 중인 측정 세션이 있으면 중단됩니다.

### `stopSleepMeasurement(context)`

워치에 `MEASUREMENT_CONTROL:STOP_SLEEP` 명령을 보냅니다.
**`stop()`과 다릅니다** — 서비스는 계속 살아 있고, 워치의 수면 측정만 끝냅니다.

측정이 실제로 종료되면 [`onSleepFinished`](#선택-구현-5개)가 호출됩니다.

```kotlin
// 사용자가 "수면 측정 종료" 버튼을 누른 경우
PolihealthGalaxyWatchAndroidSdk.stopSleepMeasurement(context)
// → 잠시 후 onSleepFinished(sessionId, sleepQuality)
```

---

## 6. 이벤트 — `ServerSdkCallback`

이벤트는 **11개**입니다. 6개는 반드시 구현해야 하고, 5개는 기본 구현이 있어 선택입니다.

### 필수 구현 (6개)

| 이벤트 | 언제 |
|---|---|
| `onConnected(deviceName: String)` | 워치와 BLE 연결 성공 |
| `onDisconnected()` | 연결 끊김 |
| `onTrackingStarted(sessionId: String)` | 워치가 측정 시작 신호를 보냄 |
| `onTrackingFinished(sessionId: String)` | 워치가 측정 종료 신호를 보냄 |
| `onSensorData(sessionId, sensorType, samples)` | 센서 샘플 배치 도착 |
| `onError(message: String)` | SDK 내부 오류 |

### 선택 구현 (5개)

| 이벤트 | 언제 | 쓰임새 |
|---|---|---|
| `onMeasurementStarted(sessionId, type)` | 측정 종류(일상/수면)가 확정됨 | 화면에 "수면 측정 중" 표시 |
| `onStoragePath(path: String?)` | CSV 저장 폴더가 정해짐 | 디버깅 · 파일 확인 |
| `onSleepFinished(sessionId, sleepQuality: Int?)` | 수면 측정 최종 종료 + 서버 응답 수신 | 수면 점수 표시 |
| `onProtocol2_1Result(sessionId, success, httpCode, body)` | 일상 데이터 업로드 결과 | 실패 재시도 · 로깅 |
| `onProtocol8_1Result(sessionId, success, httpCode, body)` | 수면 데이터 업로드 결과 | 상동 |

### 세션 ID 형식

이벤트마다 `sessionId`가 따라옵니다. 형식이 측정 방식에 따라 다릅니다.

| 측정 | 형식 | 예 |
|---|---|---|
| 주기 측정 | `yyyyMMdd_HHmm` | `20260917_1430` |
| 즉시 측정 | `on_demand_<timestamp>` | `on_demand_1758... ` |
| 수면 종료 (`onSleepFinished`) | `yyyyMMdd_HHmmss` | `20260917_143052` |

> `onSleepFinished`만 **초 단위까지** 붙습니다. 다른 이벤트의 sessionId와
> 문자열 비교로 매칭하려 하면 어긋납니다.

### 이벤트 순서

```
start()
  │
  ├─ onConnected("Galaxy Watch6 ABCD")
  │
  ├─ onTrackingStarted("20260917_1430")        측정 시작
  ├─ onMeasurementStarted(..., SLEEP)          종류 확정 (순서가 앞뒤로 바뀔 수 있음)
  ├─ onStoragePath("/data/.../20260917_1430")
  │
  ├─ onSensorData(..., ACC, [...])             ← 반복
  ├─ onSensorData(..., PPG_GREEN_25, [...])    ← 반복
  │
  ├─ onTrackingFinished("20260917_1430")       측정 종료
  ├─ onProtocol8_1Result(..., true, 200, "..")  업로드 결과 (수면은 여러 번)
  └─ onSleepFinished("20260917_143052", 82)    수면 최종 종료
```

> `onMeasurementStarted`는 `onTrackingStarted`보다 **먼저 올 수도, 나중에 올 수도** 있습니다.
> 워치가 보내는 텍스트 알림과 protobuf 데이터의 도착 순서가 보장되지 않기 때문입니다.
> 둘 중 하나에만 의존하는 로직을 짜지 마세요.

### 호출 스레드

콜백은 **메인 스레드가 아닙니다.** UI를 건드리려면 직접 전환하세요.

```kotlin
override fun onConnected(deviceName: String) {
    runOnUiThread { statusText.text = "연결됨: $deviceName" }
}
```

---

## 7. 데이터 구조

### `onSensorData`로 오는 것

```kotlin
fun onSensorData(
    sessionId: String,
    sensorType: SensorType,
    samples: List<SensorSamples>,
)
```

`SensorType`은 protobuf enum입니다. 실제로 오는 값은 주로 앞의 넷입니다.

| 값 | 내용 | 샘플링 |
|---|---|---|
| `ACC` | 3축 가속도 | 25Hz |
| `ECG` | 심전도 | — |
| `PPG_GREEN_25` | PPG 녹색광 | 25Hz |
| `PPG_GREEN_100` | PPG 녹색광 | 100Hz |

<details>
<summary>정의된 전체 값</summary>

`ACC`, `ECG`, `PPG_GREEN_25`, `PPG_GREEN_100`, `UNKNOWN`,
`PPG_IR_25`, `PPG_RED_25`, `PPG_100_IR`, `PPG_100_RED`
</details>

### `SensorSamples` 꺼내 쓰기

`oneof`라서 타입별로 담기는 필드가 다릅니다. `sensorType`으로 분기하세요.

```kotlin
override fun onSensorData(
    sessionId: String,
    sensorType: SensorType,
    samples: List<SensorSamples>,
) {
    samples.forEach { s ->
        when (sensorType) {
            SensorType.ACC -> {
                val d = s.acc25Data
                log("t=${d.timestamp} x=${d.x} y=${d.y} z=${d.z}")
            }
            SensorType.ECG -> {
                val d = s.ecgData
                log("t=${d.timestamp} value=${d.value} leadOff=${d.leadOff}")
            }
            SensorType.PPG_GREEN_25 -> {
                val d = s.ppgGreen25Data
                log("green=${d.green25} ir=${d.ir25} red=${d.red25}")
            }
            SensorType.PPG_GREEN_100 -> {
                val d = s.ppgGreen100Data
                log("green=${d.green100} ir=${d.ir100} red=${d.red100}")
            }
            else -> Unit
        }
    }
}
```

<details>
<summary>필드 정의 (proto3)</summary>

```protobuf
message Acc25       { int64 timestamp; sint32 x; sint32 y; sint32 z; }
message PpgGreen25  { int64 timestamp; int32 green25;  int32 ir25;  int32 red25; }
message PpgGreen100 { int64 timestamp; int32 green100; int32 ir100; int32 red100; }
message Ecg         { int64 timestamp; float value; sint32 lead_off;
                      float max_threshold_mv; float min_threshold_mv; uint32 seq; }
```
</details>

> **`onSensorData`를 안 받아도 서버 업로드는 됩니다.** 이 콜백은 실시간 표시·자체 저장용이고,
> 서버 전송은 SDK가 알아서 합니다. 화면에 그래프를 그릴 게 아니면 비워둬도 됩니다.

---

## 8. 전체 예제

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PolihealthGalaxyWatchAndroidSdk.init(
            baseUrl      = "https://api.example.com/",
            clientId     = BuildConfig.CLIENT_ID,
            clientSecret = BuildConfig.CLIENT_SECRET,
            callback     = watchCallback,
        )
        PolihealthGalaxyWatchAndroidSdk.setHealthOnUser(userSno = 1234, userAge = 30)

        startButton.setOnClickListener {
            if (hasPermissions()) {
                PolihealthGalaxyWatchAndroidSdk.start(this)
            } else {
                requestPermissions(requiredPermissions(), REQ_CODE)
            }
        }

        stopButton.setOnClickListener {
            PolihealthGalaxyWatchAndroidSdk.stop(this)
        }
    }

    private val watchCallback = object : ServerSdkCallback {

        override fun onConnected(deviceName: String) {
            runOnUiThread { status.text = "연결됨 · $deviceName" }
        }

        override fun onDisconnected() {
            runOnUiThread { status.text = "연결 끊김" }
        }

        override fun onTrackingStarted(sessionId: String) {
            runOnUiThread { status.text = "측정 중 · $sessionId" }
        }

        override fun onTrackingFinished(sessionId: String) {
            runOnUiThread { status.text = "측정 종료 · 업로드 중" }
        }

        override fun onMeasurementStarted(sessionId: String, type: MeasurementType) {
            val label = when (type) {
                MeasurementType.ECG   -> "일상 측정"
                MeasurementType.SLEEP -> "수면 측정"
                MeasurementType.STOP  -> "측정 종료"
            }
            runOnUiThread { typeText.text = label }
        }

        override fun onSensorData(
            sessionId: String,
            sensorType: SensorType,
            samples: List<SensorSamples>,
        ) {
            // 실시간 표시가 필요할 때만 구현. 서버 전송은 SDK가 알아서 합니다.
        }

        override fun onSleepFinished(sessionId: String, sleepQuality: Int?) {
            runOnUiThread {
                status.text = sleepQuality
                    ?.let { "수면 점수 $it" }
                    ?: "수면 측정 완료 (점수 없음)"
            }
        }

        override fun onProtocol8_1Result(
            sessionId: String, success: Boolean, httpCode: Int, body: String,
        ) {
            if (!success) Log.e(TAG, "수면 업로드 실패 [$httpCode] $body")
        }

        override fun onError(message: String) {
            Log.e(TAG, "SDK 오류: $message")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 측정을 계속하려면 stop()을 부르지 마세요 — 포그라운드 서비스가 유지됩니다.
    }
}
```

---

## 9. 서버로 나가는 요청

SDK가 호출하는 엔드포인트는 **4개**입니다. 자체 서버를 쓴다면 이것들을 구현해야 합니다.

| 엔드포인트 | 시점 | 본문 |
|---|---|---|
| `POST poli/sleep/start` | 수면 측정 시작 | JSON |
| `POST poli/sleep/stop-1` | 수면 측정 종료 | JSON |
| `POST poli/day/protocol2-1` | 일상 데이터 업로드 | CSV 멀티파트 |
| `POST poli/sleep/protocol8-1` | 수면 데이터 업로드 | CSV 멀티파트 |

모든 요청에 붙는 헤더:

```http
ClientId:     <init에 넘긴 값>
ClientSecret: <init에 넘긴 값>
```

공통 필드: `userSno`, `userAge`, `sessionId`, `reqDate`(`yyyyMMddHHmmss`)

### CSV 본문 포맷

센서 종류에 따라 헤더 행이 다릅니다.

```csv
GREEN,IR,RED        # PPG
x,y,z               # 가속도
value               # ECG
```

> 수면 측정은 `protocol8-1`이 **1분 청크 단위로 여러 번** 호출됩니다.
> 서버는 같은 `sessionId`로 여러 번 들어오는 것을 누적 처리해야 합니다.
>
> `poli/sleep/stop-1`의 응답에 수면 점수를 담아주면 `onSleepFinished`의
> `sleepQuality`로 전달됩니다. 안 주면 `null`이 갑니다.

---

## 10. 자주 막히는 지점

**`start()`에서 `IllegalStateException`**
`init()`을 안 불렀습니다. 순서는 `init()` → `setHealthOnUser()` → `start()`입니다.

**워치를 못 찾음 · 스캔 결과 0건**
런타임 권한을 안 받았을 가능성이 큽니다. BLE 스캔은 권한이 없어도 **예외를 던지지 않고
빈 결과를 반환**합니다. [3절](#3-런타임-요청--앱이-직접-해야-함)을 확인하세요.

**`NoClassDefFoundError`**
`bluetooth-sdk-android-v2`나 다른 전이 의존성이 빠졌습니다.
AAR엔 POM이 없어 자동으로 안 따라옵니다. [2절](#2-설치) 목록을 전부 넣으세요.

**워치 데이터가 안 풀림 · protobuf 파싱 오류**
워치 앱과 폰 앱의 `protobuf-javalite` 버전이 다릅니다. 같은 버전으로 맞추세요.

**전송이 극단적으로 느림 (~1.2KB/s)**
갤럭시워치 연결에서 EATT 채널이 수립되면 MTU 협상이 워치 앱까지 도달하지 않아
청크가 20B로 잡히는 문제가 있습니다. 이 SDK가 워치의 **청크 프로브**(`PROBE:`)를
받아 `PROBE_ACK`로 회신해 실효 크기를 판정하므로, **폰과 워치 SDK를 같이 최신으로
올리면** 자동 해결됩니다. 앱이 따로 할 일은 없습니다.

한쪽만 올리면 프로브가 동작하지 않으니 **두 SDK는 항상 짝을 맞춰 배포하세요.**

**업로드가 타임아웃**
`readTimeoutMs` / `writeTimeoutMs`를 줄이지 마세요. 기본 120초입니다.
수면 측정은 한 번에 수 MB가 나갑니다.

**앱을 내렸는데 측정이 멈춤**
`onDestroy()`에서 `stop()`을 부르고 있지 않은지 확인하세요.
포그라운드 서비스라 앱이 백그라운드여도 계속 돌아야 정상입니다.

**Android 13에서 알림이 안 뜸**
`POST_NOTIFICATIONS` 런타임 권한이 필요합니다. 없으면 포그라운드 서비스 알림이
표시되지 않고, 시스템이 서비스를 일찍 종료할 수 있습니다.

---

## 예제 앱 돌려보기

이 레포를 클론했다면 예제가 바로 빌드됩니다. `local.properties`에 두 줄만 넣으면 됩니다.

```properties
sdk.dir=/Users/<사용자>/Library/Android/sdk
githubUsername=<사용자명>
githubAccessToken=<read:packages 권한 토큰>

# 예제가 붙을 서버 — 없으면 앱이 "서버 설정이 없습니다" 안내를 띄운다
exampleApiUrl=https://your-server.example.com/
exampleClientId=<발급받은 ClientId>
exampleClientSecret=<발급받은 ClientSecret>
```

```bash
./gradlew :polihealth-galaxy-watch-android-sdk-example:assembleDebug
```

### 설정 키 전체 목록

서버 접속 정보는 **소스에 두지 않습니다.** `local.properties`(git 제외)를 먼저 보고,
없으면 환경변수를 읽습니다. 둘 다 없으면 빈 문자열로 빌드되며, 빌드는 통과하고
SDK 초기화 단계에서 안내와 함께 멈춥니다.

| local.properties | 환경변수 | 채울 값 |
|---|---|---|
| `exampleApiUrl` | `EXAMPLE_API_URL` | 서버 주소 (끝의 `/` 유무는 무관) |
| `exampleClientId` | `EXAMPLE_CLIENT_ID` | 발급받은 ClientId |
| `exampleClientSecret` | `EXAMPLE_CLIENT_SECRET` | 발급받은 ClientSecret |

release 빌드에서 **다른 서버**를 쓸 때만 아래를 추가로 지정합니다.
비워두면 위 값을 그대로 씁니다.

| local.properties | 환경변수 |
|---|---|
| `exampleReleaseApiUrl` | `EXAMPLE_RELEASE_API_URL` |
| `exampleReleaseClientId` | `EXAMPLE_RELEASE_CLIENT_ID` |
| `exampleReleaseClientSecret` | `EXAMPLE_RELEASE_CLIENT_SECRET` |

> 이 키들은 **예제 앱 전용**입니다. SDK 자체는 `init()` 파라미터로 값을 받으므로,
> 여러분의 앱에서는 원하는 방식(BuildConfig, 원격 설정, 로그인 응답 등)으로
> 넘기면 됩니다.

레포 안에서는 예제가 `implementation(project(":polihealth-galaxy-watch-android-sdk"))`로
**옆 모듈을 직접 참조**하므로, 2절의 의존성 목록을 따로 적을 필요가 없습니다.
버전은 루트 `gradle/libs.versions.toml`이 관리합니다.

예제에서 볼 수 있는 것:

- 콜백 11개 전부 구현 — 로그 화면에 이벤트가 순서대로 찍힙니다
- `SDK 초기화` → `서비스 시작` → 워치 연결 → 측정 → 업로드까지의 전체 흐름
- `수면측정 종료` 버튼 — `stop()`과 어떻게 다른지

## 관련 문서

- [`polihealth-galaxy-watch-wearos-sdk`](../polihealth-galaxy-watch-wearos-sdk) — 짝이 되는 워치 앱 SDK
- [`bluetooth-sdk-android-v2`](https://github.com/hconnectdx/bluetooth-sdk-android-v2) — BLE 통신 기반
