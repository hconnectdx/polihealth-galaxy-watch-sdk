# polihealth-galaxy-watch-wearos-sdk

갤럭시워치에서 **생체 센서를 측정하고 protobuf 바이트로 직렬화**해주는 Wear OS 라이브러리입니다.

```
[갤럭시워치: 이 SDK]          ──▶  [앱이 전송]  ──BLE──▶  [휴대폰]
 삼성 Health Sensor로 측정          onDataReady(bytes)
 버퍼에 모으기
 protobuf 직렬화
```

> **이 SDK는 데이터를 보내지 않습니다.**
> 직렬화된 바이트를 `onDataReady`로 건네줄 뿐이고, **BLE 전송은 앱이 직접** 해야 합니다.
> 휴대폰에서 받는 쪽은 → [`polihealth-galaxy-watch-android-sdk`](../polihealth-galaxy-watch-android-sdk)

---

## 목차

1. [시작 전 준비물](#1-시작-전-준비물)
2. [설치](#2-설치)
3. [권한](#3-권한)
4. [측정 방식 두 가지](#4-측정-방식-두-가지)
5. [설정값](#5-설정값)
6. [함수 레퍼런스](#6-함수-레퍼런스)
7. [이벤트 — SensorDataCallback](#7-이벤트--sensordatacallback)
8. [폰으로 보내기](#8-폰으로-보내기)
9. [전체 예제](#9-전체-예제)
10. [자주 막히는 지점](#10-자주-막히는-지점)

---

## 1. 시작 전 준비물

| 항목 | 요구사항 |
|---|---|
| 기기 | **갤럭시워치** (Wear OS). 다른 제조사 워치는 동작하지 않습니다 |
| 삼성 승인 | **Samsung Health Sensor SDK 파트너십 필수** — 아래 참고 |
| 전송 수단 | BLE Peripheral 라이브러리 (예: `bluetooth-sdk-android-peripheral`) |
| 짝 | 폰에 `polihealth-galaxy-watch-android-sdk`로 만든 앱 |

### 삼성 파트너십이 반드시 필요합니다

이 SDK는 `samsung-health-sensor-api`를 통해 워치 센서에 접근합니다.
이 API는 **삼성 파트너십 승인을 받은 앱만** 사용할 수 있습니다.

- 승인 없이는 센서 접근이 거부되어 측정이 시작되지 않습니다
- 폰측 SDK가 요구하는 **Data SDK 파트너십과는 별개**입니다 — 따로 신청해야 합니다
- 신청 절차는 동봉된 `삼성_헬스_Sensor_SDK_파트너십_신청_가이드.pdf` 참고

편의를 위해 `libs/samsung-health-sensor-api-1.4.1.aar`이 레포에 포함돼 있지만,
**정식 배포 전에는 반드시 자체 파트너십 승인을 받으세요.**

---

## 2. 설치

### 방법 A — GitHub Packages (권장)

`local.properties`에 자격증명을 넣고 저장소를 등록하면 한 줄로 끝납니다.

```properties
githubUsername=<사용자명>
githubAccessToken=<read:packages 권한 토큰>
```

```kotlin
maven {
    url = uri("https://maven.pkg.github.com/hconnectdx/polihealth-galaxy-watch-sdk")
    credentials {
        username = localProperties.getProperty("githubUsername")
        password = localProperties.getProperty("githubAccessToken")
    }
}

dependencies {
    implementation("kr.co.hconnect:polihealth-galaxy-watch-wearos-sdk:1.0.0")

    // ⚠️ 삼성 Sensor API는 이 방법으로도 따라오지 않습니다 — 아래 설명
    implementation(files("libs/samsung-health-sensor-api-1.4.1.aar"))
}
```

POM이 함께 배포되므로 protobuf·coroutines 등 **전이 의존성은 자동으로 따라옵니다.**
**다만 삼성 Sensor AAR만은 예외입니다** — SDK가 `compileOnly`로 참조해서
POM에 아예 기록되지 않기 때문입니다.

### 방법 B — AAR 파일만 받은 경우

```kotlin
dependencies {
    implementation(files("libs/polihealth-galaxy-watch-wearos-sdk-1.0.0.aar"))

    // ⚠️ 삼성 Sensor API — 반드시 직접 넣어야 합니다 (아래 설명)
    implementation(files("libs/samsung-health-sensor-api-1.4.1.aar"))

    // ⚠️ AAR 단독에는 POM이 없어 전이 의존성이 따라오지 않습니다 (SDK 빌드 시점의 실제 버전)
    implementation("androidx.core:core-ktx:1.18.0")          // ⚠️ 아래 호환성 주의
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.protobuf:protobuf-javalite:3.25.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.4")
}
```

### ⚠️ `androidx.core` 1.18.0 호환성

`androidx.core:core-ktx:1.18.0`은 **`compileSdk 36` + `AGP 8.9.1` 이상**을 요구합니다.
낮은 환경이면 버전을 낮춰 고정하세요:

```kotlin
configurations.all {
    resolutionStrategy {
        force("androidx.core:core:1.13.1")
        force("androidx.core:core-ktx:1.13.1")
    }
}
```

### 삼성 AAR은 왜 직접 넣어야 하나

이 SDK는 삼성 AAR을 **`compileOnly`로** 참조합니다. 즉 **우리 AAR 안에 삼성 코드가 들어 있지 않습니다.**

| | 넣지 않으면 |
|---|---|
| 컴파일 | **통과합니다** |
| 실행 | 측정 시작 시 `NoClassDefFoundError`로 죽습니다 |

빌드가 됐다고 안심하면 안 됩니다. 실기기에서 측정을 걸어봐야 확인됩니다.

### protobuf 버전을 폰과 맞추세요

`protobuf-javalite`가 `api`로 노출됩니다. **워치와 폰의 버전이 다르면
워치가 직렬화한 데이터를 폰이 못 풉니다.** 양쪽을 같은 버전으로 고정하세요.

---

## 3. 권한

### 매니페스트 — 추가 작업 없음

SDK 매니페스트에 선언돼 있고 병합으로 자동 포함됩니다.

<details>
<summary>포함되는 권한 전체 보기</summary>

```xml
<uses-permission android:name="android.permission.BODY_SENSORS" />
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
<uses-permission android:name="android.permission.HIGH_SAMPLING_RATE_SENSORS" />
<uses-permission android:name="com.samsung.android.hardware.sensormanager.permission.READ_ADDITIONAL_HEALTH_DATA" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_HEALTH" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

함께 선언되는 컴포넌트:
- `TrackingService` — `foregroundServiceType="health"`
- `ScheduleReceiver` — 주기 알람 수신 (`ACTION_SLOT_ALARM`)
</details>

`READ_ADDITIONAL_HEALTH_DATA`는 삼성 전용 권한입니다. 파트너십 승인과 짝을 이룹니다.

### 런타임 요청 — 앱이 직접 해야 함

| 권한 | 대상 | 없으면 |
|---|---|---|
| `BODY_SENSORS` | 전체 | 센서 접근 거부 |
| `ACTIVITY_RECOGNITION` | Android 10 (API 29) 이상 | 일부 센서 제한 |
| `POST_NOTIFICATIONS` | Android 13 (API 33) 이상 | 포그라운드 알림 미표시 |

```kotlin
val permissions = buildList {
    add(Manifest.permission.BODY_SENSORS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        add(Manifest.permission.ACTIVITY_RECOGNITION)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}
requestPermissions(permissions.toTypedArray(), REQ_CODE)
```

---

## 4. 측정 방식 두 가지

성격이 다르니 먼저 무엇을 쓸지 정하세요.

### 주기 측정 — 정해진 시각에 자동으로

```
매 시 0분 ─────▶ 2분간 측정 ─────▶ 대기
매 시 30분 ────▶ 2분간 측정 ─────▶ 대기
```

알람으로 스케줄링되며, 앱을 안 켜놔도 동작합니다. 재부팅 후에도 유지됩니다.
`sessionId`는 `yyyyMMdd_HHmm` 형식으로 붙습니다.

```kotlin
setAlarmSlotMinutes(context, intArrayOf(0, 30))        // 언제
setMeasurementDuration(context, 120_000L)              // 얼마나
setAlarmSensorTypes(context, setOf(SensorType.ACC))    // 무엇을
schedulePeriodicAlarm(context, setOf(SensorType.ACC))  // 시작
```

### 온디맨드 측정 — 사용자가 누를 때

```kotlin
startOnDemandTracking(context, setOf(SensorType.ECG))
// ...
stopOnDemandTracking(context)
```

명시적으로 멈출 때까지 측정합니다. `sessionId`는 `on_demand_<timestamp>` 형식입니다.

---

## 5. 설정값

설정은 기기에 저장(DataStore)되어 **앱을 껐다 켜도 유지**됩니다.

| 설정 | 기본값 | 범위 |
|---|---|---|
| 측정 시간 `setMeasurementDuration` | — | ms 단위 |
| 알람 슬롯 `setAlarmSlotMinutes` | `[1, 31]` | 0~59 |
| 알람 센서 `setAlarmSensorTypes` | — | `SensorType` 집합 |
| 측정 타입 `setMeasurementType` | — | 임의 문자열 |

```kotlin
// 매 시 0분, 30분에 측정
setAlarmSlotMinutes(context, intArrayOf(0, 30))

// 매 시 15분 간격
setAlarmSlotMinutes(context, intArrayOf(0, 15, 30, 45))
```

`setAlarmSlotMinutes`는 값을 검증합니다 — 빈 배열이거나 0~59를 벗어나면
`IllegalArgumentException`이 납니다.

> **설정 함수는 내부에서 `runBlocking`을 씁니다.** 메인 스레드에서 연속 호출하면
> 잠깐 멈출 수 있습니다. 앱 시작 시 한 번에 몰아서 설정하세요.

### 센서 타입

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

폰측 SDK와 동일한 정의입니다.
</details>

---

## 6. 함수 레퍼런스

진입점은 `PolihealthGalaxyWatchWearOsSdk` object 하나입니다.

### 초기화

| 함수 | 설명 |
|---|---|
| `init(callback: SensorDataCallback)` | 콜백 등록. **다른 함수 호출 전 반드시 1회** |
| `isInitialized: Boolean` | 초기화 여부 |
| `trackingState` | 현재 측정 상태 |

`init()` 없이 측정 함수를 부르면 **`IllegalStateException`**이 납니다.

### 측정

| 함수 | 설명 |
|---|---|
| `startPeriodicTracking(context, durationMs, slotMinute, sensorTypes)` | 주기 측정 **1회** 즉시 실행 |
| `startOnDemandTracking(context, sensorTypes, timestamp = now)` | 온디맨드 측정 시작 |
| `stopOnDemandTracking(context)` | 측정 중지 |

> `startPeriodicTracking`은 **한 번만** 측정합니다. 반복하려면 `schedulePeriodicAlarm`을 쓰세요.

### 스케줄링

| 함수 | 설명 |
|---|---|
| `schedulePeriodicAlarm(context, sensorTypes)` | 주기 알람 등록 (반복) |
| `cancelPeriodicAlarm(context)` | 알람 해제 |

`schedulePeriodicAlarm`은 센서 타입을 자동 저장하므로, 재부팅 후에도
`ScheduleReceiver`가 같은 설정으로 측정합니다.

### 설정

| 함수 | 짝 |
|---|---|
| `setMeasurementDuration(context, ms)` | `getMeasurementDuration(context)` |
| `setAlarmSlotMinutes(context, IntArray)` | `getAlarmSlotMinutes(context)` |
| `setAlarmSensorTypes(context, Set)` | `getAlarmSensorTypes(context)` |
| `setMeasurementType(context, String)` | `getMeasurementType(context)` |

---

## 7. 이벤트 — `SensorDataCallback`

이벤트는 **하나뿐**입니다.

```kotlin
fun interface SensorDataCallback {
    fun onDataReady(data: ByteArray): Boolean
}
```

| | |
|---|---|
| **언제** | 직렬화된 protobuf 바이트가 전송 준비됐을 때 |
| **`data`** | `SensorBufferProto`를 직렬화한 바이트 배열 |
| **반환값** | 데이터를 받아들였는지 여부 |

`fun interface`라서 람다로 짧게 쓸 수 있습니다.

### ⚠️ 이 콜백 안에서 전송을 기다리면 안 됩니다

센서 데이터는 25~100Hz로 쏟아집니다. 콜백에서 전송이 끝날 때까지 블로킹하면
**역압(backpressure)이 걸려 수집량 자체가 깎입니다.**

> 동기 `sendData`가 호출자를 **배치당 수십 초** 세우는 바람에
> **수집량이 1/4로 떨어진 것이 실측**됐습니다.
> (`bluetooth-sdk-android-peripheral` CHANGELOG 1.0.1)

**큐에 넣고 즉시 반환하세요.**

```kotlin
PolihealthGalaxyWatchWearOsSdk.init { payload ->
    if (!HCBlePeripheral.isConnected) {
        return@init false          // 미연결 — 이 데이터는 폰에 도달하지 못합니다
    }
    HCBlePeripheral.sendDataAsync(payload)   // 논블로킹: 큐에 넣고 바로 리턴
}
```

| 함수 | 성격 | 콜백에서 |
|---|---|---|
| `sendDataAsync(data)` | 논블로킹 · 큐에 적재 | ✅ **이것을 쓰세요** |
| `sendData(data)` | 동기 · 전송 끝날 때까지 블록 | ❌ 쓰지 마세요 |

> `sendDataAsync`의 반환값은 **큐 적재 성공 여부이지 전송 완료가 아닙니다.**
> `false`는 큐가 가득 찼다는 뜻이고(드롭됨), 연결이 끊기면 큐에 남은 데이터도 폐기됩니다.
> 유실 건수를 추적하려면 이 반환값을 세어두세요.

큐 상한은 `PeripheralConfig.txQueueCapacity`로 **기본 32**입니다. 무한 큐로 인한
메모리 폭주를 막으려고 상한 도달 시 신규를 거부하는 정책입니다.
전송이 느려 드롭이 잦다면 이 값을 올리기 전에 **청크 크기부터 확인**하세요.

`sendDataAsync`는 `bluetooth-sdk-android-peripheral` **1.0.1 이상**에 있습니다.
(개발 중 내부 빌드에 1.0.2 라벨을 쓴 적이 있으나, 정식 배포는 1.0.1 하나로 합쳐졌습니다.)

### 진단에 쓸 수 있는 속성

문제가 생겼을 때 로그에 찍어두면 원인 추적이 빨라집니다.

| 속성 | 의미 |
|---|---|
| `HCBlePeripheral.isConnected` | 폰이 연결돼 있는지 |
| `HCBlePeripheral.negotiatedMtu` | 협상된 MTU |
| `HCBlePeripheral.currentChunkSize` | **실효 청크 크기** — 아래 참고 |

> 갤럭시워치–폰 연결에서 EATT 채널이 수립되면 `onMtuChanged`가 발화하지 않아
> MTU가 23(청크 20B)으로 잡히는 문제가 있습니다. 실효 ~1.2KB/s로 링크 용량의
> 극히 일부만 씁니다. Peripheral SDK 1.0.1이 **청크 프로브**로 실제 크기를 왕복 측정해
> 해결하며, 그 결과가 `currentChunkSize`입니다. `negotiatedMtu`가 23인데
> `currentChunkSize`가 253이면 프로브가 정상 동작한 것입니다.

---

## 8. 폰으로 보내기

이 SDK는 **전송 수단을 정해주지 않습니다.** `onDataReady`로 받은 바이트를
앱이 원하는 방식으로 보내면 됩니다. 폰측 SDK와 짝을 맞추려면 **BLE Peripheral**을 쓰세요.

```kotlin
// 1) BLE Peripheral 준비 — 워치가 광고하고, 폰이 연결해 옵니다
HCBlePeripheral.init(context)
HCBlePeripheral.start()

// 2) 센서 데이터가 나오면 큐에 넣고 즉시 반환
PolihealthGalaxyWatchWearOsSdk.init { payload ->
    if (!HCBlePeripheral.isConnected) return@init false
    HCBlePeripheral.sendDataAsync(payload)
}
```

`HCBlePeripheral`이 4바이트 길이 헤더를 붙이고 MTU 크기로 쪼개서 보냅니다.
폰측 `PacketReassembler`가 그걸 다시 붙입니다. **직접 쪼개지 마세요.**

### 측정 타입 알리기

폰에 "지금 일상 측정인지 수면 측정인지" 알리려면 별도 텍스트를 보냅니다.
폰측이 이걸 받아 `onMeasurementStarted` 콜백으로 올립니다.
센서 데이터는 바이너리고 이건 UTF-8 텍스트라, 폰이 형식으로 구분합니다.

| 메시지 | 의미 |
|---|---|
| `MEASUREMENT_TYPE:ECG` | 일상(ECG) 측정 시작 |
| `MEASUREMENT_TYPE:SLEEP` | 수면 측정 시작 |
| `MEASUREMENT_TYPE:STOP` | 측정 종료 |

**이것도 `sendDataAsync`로 보내야 합니다.** `sendText()`나 `sendData()`를 쓰면
동기 경로라 **센서 데이터 큐를 새치기합니다.**

```kotlin
private fun notifyMeasurementType(type: String) {
    if (!HCBlePeripheral.isConnected) {
        Log.w(TAG, "BLE 미연결 — 폰이 이 신호를 받지 못합니다")
        return
    }
    val message = "MEASUREMENT_TYPE:$type".toByteArray(Charsets.UTF_8)

    var ok = HCBlePeripheral.sendDataAsync(message)
    if (!ok) {
        // 큐가 가득 차 거부된 경우 — 이 신호는 순서보다 "도착"이 훨씬 중요하므로
        // 동기 경로로라도 반드시 보낸다
        ok = HCBlePeripheral.sendData(message)
    }
}
```

> **`STOP`을 동기로 보내면 데이터가 통째로 사라집니다.**
> 아직 전송되지 않은 센서 데이터가 큐에 남아 있는데 `STOP`이 새치기해서 먼저 도착하면,
> 폰이 세션을 닫아버려 **뒤에 남아있던 마지막 센서 데이터가 전부 버려집니다.**
> 실측으로 확인된 문제입니다. 같은 큐를 쓰면 항상 센서 데이터 뒤에 순서대로 나갑니다.
>
> 큐 적재가 거부됐을 때만 동기 경로로 폴백하세요 — 그 경우엔 순서보다 도착이 중요합니다.

---

## 9. 전체 예제

```kotlin
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) BLE Peripheral 시작 — 폰이 연결해 올 통로
        HCBlePeripheral.init(this)
        HCBlePeripheral.start()

        // 2) SDK 초기화 — 데이터가 나오면 큐에 적재하고 즉시 반환 (역압 방지)
        PolihealthGalaxyWatchWearOsSdk.init { payload ->
            if (!HCBlePeripheral.isConnected) {
                skipCount++
                return@init false
            }
            HCBlePeripheral.sendDataAsync(payload).also { accepted ->
                if (!accepted) dropCount++      // 큐 가득 참 — 이 데이터는 유실됩니다
            }
        }

        // 3) 설정은 앱 시작 시 한 번에 (runBlocking이라 몰아서)
        with(PolihealthGalaxyWatchWearOsSdk) {
            setMeasurementDuration(this@MainActivity, 120_000L)      // 2분
            setAlarmSlotMinutes(this@MainActivity, intArrayOf(0, 30)) // 매 시 0분, 30분
            setAlarmSensorTypes(this@MainActivity, SENSORS)
        }

        setContent {
            WatchScreen(
                onStartPeriodic = { startPeriodic() },
                onStartOnDemand = { startOnDemand() },
                onStop          = { stop() },
            )
        }
    }

    private fun startPeriodic() {
        if (!hasPermissions()) { requestPermissions(REQUIRED, REQ_CODE); return }

        notifyMeasurementType("SLEEP")
        PolihealthGalaxyWatchWearOsSdk.schedulePeriodicAlarm(this, SENSORS)
    }

    private fun startOnDemand() {
        if (!hasPermissions()) { requestPermissions(REQUIRED, REQ_CODE); return }

        notifyMeasurementType("ECG")
        PolihealthGalaxyWatchWearOsSdk.startOnDemandTracking(this, setOf(SensorType.ECG))
    }

    private fun stop() {
        PolihealthGalaxyWatchWearOsSdk.stopOnDemandTracking(this)
        PolihealthGalaxyWatchWearOsSdk.cancelPeriodicAlarm(this)
        notifyMeasurementType("STOP")      // 반드시 같은 큐로 — 8절 참고
    }

    /** 측정 타입 알림. 센서 데이터와 같은 송신 큐를 써야 순서가 보장됩니다. */
    private fun notifyMeasurementType(type: String) {
        if (!HCBlePeripheral.isConnected) return
        val message = "MEASUREMENT_TYPE:$type".toByteArray(Charsets.UTF_8)
        if (!HCBlePeripheral.sendDataAsync(message)) {
            HCBlePeripheral.sendData(message)   // 큐 거부 시에만 동기 폴백
        }
    }

    private var skipCount = 0   // BLE 미연결로 보내지 못한 건수
    private var dropCount = 0   // 큐가 가득 차 버려진 건수

    companion object {
        private val SENSORS = setOf(SensorType.ACC, SensorType.PPG_GREEN_25)
        private val REQUIRED = arrayOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION,
        )
        private const val REQ_CODE = 1001
    }
}
```

---

## 10. 자주 막히는 지점

**측정 시작 시 `NoClassDefFoundError`**
`samsung-health-sensor-api-1.4.1.aar`이 빠졌습니다. `compileOnly`라서 빌드는 통과합니다.
[2절](#2-설치)을 확인하세요.

**센서 접근이 거부됨 · 측정이 시작되지 않음**
삼성 파트너십 승인을 받지 않은 앱입니다. 패키지명과 서명키가 승인받은 것과
일치해야 합니다.

**`IllegalStateException: init()을 먼저 호출하세요`**
`init(callback)`을 안 불렀습니다. 측정·스케줄링 함수 모두 초기화가 선행돼야 합니다.

**주기 측정이 안 울림**
`SCHEDULE_EXACT_ALARM` 권한 문제일 수 있습니다. Android 12 이상에서는
사용자가 설정에서 "알람 및 리마인더"를 허용해야 정확한 알람이 동작합니다.

**측정 종료 직전 데이터가 통째로 사라짐**
`MEASUREMENT_TYPE:STOP`을 `sendData()`/`sendText()`로 보내고 있습니다.
동기 경로라 센서 데이터 큐를 새치기해서, 폰이 세션을 먼저 닫아버립니다.
`sendDataAsync()`로 보내세요 → [8절](#측정-타입-알리기)

**측정이 자꾸 밀리고 끊김**
`onDataReady`에서 `sendData()`로 전송을 기다리고 있습니다.
센서는 25~100Hz로 쏟아지므로 콜백이 블로킹되면 역압이 걸립니다.
`sendDataAsync()`로 큐에 넣고 즉시 반환하세요 → [7절](#7-이벤트--sensordatacallback)

**데이터가 드문드문 유실됨**
`sendDataAsync()`가 `false`를 반환하고 있는지 확인하세요 — 큐가 가득 차면
새 데이터를 거부합니다(드롭). BLE 연결이 끊겨도 큐에 남은 것은 폐기됩니다.
반환값을 세어두면 유실 규모를 알 수 있습니다.

**폰이 데이터를 못 읽음**
워치와 폰의 `protobuf-javalite` 버전이 다릅니다. 같은 버전으로 맞추세요.

**`startPeriodicTracking`을 불렀는데 한 번만 측정됨**
정상입니다. 이 함수는 1회 실행이고, 반복은 `schedulePeriodicAlarm`입니다.

**설정 함수 호출 시 UI가 잠깐 멈춤**
내부에서 `runBlocking`을 씁니다. 앱 시작 시 한 번에 몰아서 설정하세요.

---

## 예제 앱 돌려보기

이 레포를 클론했다면 예제가 바로 빌드됩니다. `local.properties`에 세 줄을 넣고:

```properties
sdk.dir=/Users/<사용자>/Library/Android/sdk
githubUsername=<사용자명>
githubAccessToken=<read:packages 권한 토큰>
```

```bash
./gradlew :polihealth-galaxy-watch-wearos-sdk-example:assembleDebug
```

레포 안에서는 예제가 `implementation(project(":polihealth-galaxy-watch-wearos-sdk"))`로
옆 모듈을 직접 참조하므로 2절의 의존성 목록을 따로 적을 필요가 없습니다.
**다만 삼성 Sensor AAR은 예제에서도 `libs/`에 직접 있어야 합니다.**

예제에서 볼 수 있는 것:

- **BLE 전송 경로 전체** — `onDataReady` → `sendDataAsync` → 폰
- 미연결·큐 드롭·스킵 건수 집계 (이 콜백의 반환값을 어떻게 다뤄야 하는지)
- `MEASUREMENT_TYPE` 전송을 센서 데이터와 같은 큐로 보내는 방법
- 화면의 BLE 연결 상태와 실효 청크 크기 — 측정이 돌아도 여기가 끊겨 있으면
  폰에 아무것도 도달하지 않습니다

> 예제를 **워치에만** 올리면 광고만 하고 끝입니다. 폰에
> [`polihealth-galaxy-watch-android-sdk-example`](../polihealth-galaxy-watch-android-sdk-example)을
> 같이 올려야 연결부터 전송까지 확인됩니다.

## 관련 문서

- [`polihealth-galaxy-watch-android-sdk`](../polihealth-galaxy-watch-android-sdk) — 짝이 되는 폰 SDK
- `bluetooth-sdk-android-peripheral` — BLE 전송 (워치가 Peripheral 역할)
