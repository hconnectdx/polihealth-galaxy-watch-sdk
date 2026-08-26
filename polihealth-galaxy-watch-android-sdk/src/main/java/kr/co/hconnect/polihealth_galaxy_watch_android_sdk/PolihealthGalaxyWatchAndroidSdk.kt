package kr.co.hconnect.polihealth_galaxy_watch_android_sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import kr.co.hconnect.polihealth_galaxy_watch_android_sdk.api.HealthOnClient
import kr.co.hconnect.polihealth_galaxy_watch_android_sdk.callback.ServerSdkCallback
import kr.co.hconnect.polihealth_galaxy_watch_android_sdk.health.HealthDataManager
import kr.co.hconnect.polihealth_galaxy_watch_android_sdk.service.WatchReceiverService
import kr.co.hconnect.polihealth_galaxy_watch_android_sdk.util.Constants

/**
 * polihealth-galaxy-watch-android-sdk 진입점.
 *
 * ## 사용 순서
 * 1. [init] — 이벤트 콜백 + HealthOn 서버 정보 등록 (서비스 시작 전에 반드시 호출)
 * 2. [setHealthOnUser] — 사용자 정보(userSno, userAge) 설정
 * 3. [start] — Galaxy Watch BLE 연결 및 데이터 수신 시작
 * 4. [stop]  — 서비스 중지 및 BLE 연결 해제
 *
 * ## 예시
 * ```kotlin
 * PolihealthGalaxyWatchAndroidSdk.init(
 *     baseUrl      = "https://mapi-stg.health-on.co.kr/",
 *     clientId     = "...",
 *     clientSecret = "...",
 *     callback     = object : ServerSdkCallback {
 *         override fun onConnected(deviceName: String) { ... }
 *         override fun onDisconnected() { ... }
 *         override fun onTrackingStarted(sessionId: String) { ... }
 *         override fun onTrackingFinished(sessionId: String) { ... }
 *         override fun onSensorData(sessionId, sensorType, samples) { ... }
 *         override fun onError(message: String) { ... }
 *     }
 * )
 * PolihealthGalaxyWatchAndroidSdk.setHealthOnUser(userSno = 1234, userAge = 30)
 *
 * PolihealthGalaxyWatchAndroidSdk.start(context)
 * // ...
 * PolihealthGalaxyWatchAndroidSdk.stop(context)
 * ```
 *
 * ## 삼성헬스 Data SDK 연동 (Galaxy Watch BLE와는 별개 기능)
 * 삼성헬스 앱에 저장된 걸음수/심박수/산소포화도/피부온도를 직접 읽어온다. [init]/[start]와 무관하게
 * 바로 사용할 수 있다. 자세한 배경은 `polihealth-galaxy-watch-android-sdk/docs/samsung-health-integration.md` 참고.
 * ```kotlin
 * val granted = PolihealthGalaxyWatchAndroidSdk.checkHealthDataPermission(context)
 * if (granted.values.any { !it }) {
 *     PolihealthGalaxyWatchAndroidSdk.requestHealthDataPermission(activity)
 * }
 * val steps = PolihealthGalaxyWatchAndroidSdk.readHealthDataSteps(context, days = 7)
 * ```
 */
object PolihealthGalaxyWatchAndroidSdk {

    @Volatile private var callback: ServerSdkCallback? = null
    @Volatile private var running: Boolean = false

    /**
     * SDK를 초기화한다.
     *
     * HealthOn 서버 정보([baseUrl], [clientId], [clientSecret])를 함께 등록하며,
     * [start] 호출 전에 반드시 한 번 호출해야 한다.
     *
     * @param baseUrl          HealthOn API 서버 주소 (ex. "https://mapi-stg.health-on.co.kr/")
     * @param clientId         서버에서 발급한 ClientId 헤더 값
     * @param clientSecret     서버에서 발급한 ClientSecret 헤더 값
     * @param callback         워치 연결 / 측정 / 오류 이벤트를 수신할 콜백
     * @param connectTimeoutMs 연결 타임아웃(ms). 기본 15초
     * @param readTimeoutMs    응답 대기 타임아웃(ms). CSV 업로드 후 서버 처리 시간을 고려해 기본 120초
     * @param writeTimeoutMs   요청 전송 타임아웃(ms). 대용량 멀티파트 업로드를 고려해 기본 120초
     */
    fun init(
        baseUrl: String,
        clientId: String,
        clientSecret: String,
        callback: ServerSdkCallback,
        connectTimeoutMs: Long = 15_000L,
        readTimeoutMs: Long = 120_000L,
        writeTimeoutMs: Long = 120_000L,
    ) {
        this.callback = callback
        HealthOnClient.init(
            baseUrl          = baseUrl,
            clientId         = clientId,
            clientSecret     = clientSecret,
            connectTimeoutMs = connectTimeoutMs,
            readTimeoutMs    = readTimeoutMs,
            writeTimeoutMs   = writeTimeoutMs,
        )
    }

    /**
     * Galaxy Watch 탐색 후 BLE 연결 및 데이터 수신을 시작한다.
     *
     * [WatchReceiverService]를 포그라운드 서비스로 실행한다.
     * [init]이 호출되지 않은 상태라면 예외를 던진다.
     *
     * @throws IllegalStateException [init] 미호출 시
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun start(context: Context) {
        checkNotNull(callback) {
            "PolihealthGalaxyWatchAndroidSdk.init()이 호출되지 않았습니다. start() 전에 init()을 호출하세요."
        }
        val intent = Intent(context, WatchReceiverService::class.java)
            .setAction(Constants.ACTION_START_SERVICE)
        context.startForegroundService(intent)
        running = true
    }

    /**
     * BLE 연결을 해제하고 [WatchReceiverService]를 중지한다.
     */
    fun stop(context: Context) {
        val intent = Intent(context, WatchReceiverService::class.java)
            .setAction(Constants.ACTION_STOP_SERVICE)
        context.startService(intent)
        running = false
    }

    /** SDK가 실행 중인지 여부. */
    fun isRunning(): Boolean = running

    /**
     * 워치에 수면측정 종료 명령(`MEASUREMENT_CONTROL:STOP_SLEEP`)을 전송한다.
     *
     * [start]로 연결된 [WatchReceiverService]가 실행 중이어야 하며, 서비스는 종료되지 않고 계속 실행된다.
     */
    fun stopSleepMeasurement(context: Context) {
        val intent = Intent(context, WatchReceiverService::class.java)
            .setAction(Constants.ACTION_STOP_SLEEP_MEASUREMENT)
        context.startService(intent)
    }

    /** 내부용: 서비스에서 콜백을 참조하기 위해 사용. */
    internal fun getCallback(): ServerSdkCallback? = callback

    // ── HealthOn 사용자 정보 ──

    /**
     * protocol2-1 전송 시 함께 보낼 사용자 정보(userSno, userAge)를 설정한다.
     * [init] 호출 이후 언제든 변경 가능하다.
     */
    fun setHealthOnUser(userSno: Int, userAge: Int) {
        HealthOnClient.setUser(userSno, userAge)
    }

    // ── 삼성헬스 Data SDK 연동 ──
    // Galaxy Watch BLE 연동([start]/[stop])과는 완전히 별개의 기능이다.
    // 삼성헬스 앱에 저장된 걸음수/심박수/산소포화도/피부온도를 직접 읽어온다.
    // 자세한 배경은 polihealth-galaxy-watch-android-sdk/docs/samsung-health-integration.md 참고.

    @Volatile
    private var healthDataManager: HealthDataManager? = null

    private fun healthDataManager(context: Context): HealthDataManager =
        healthDataManager ?: HealthDataManager(context).also { healthDataManager = it }

    /**
     * 삼성헬스 Data SDK 권한(걸음수/심박수/산소포화도/피부온도) 승인 상태를 확인한다.
     *
     * 반환값은 `{"read_step": Boolean, "read_heartRate": Boolean, "read_oxygenSaturation": Boolean,
     * "read_skinTemperature": Boolean}` 형태이며, 미승인 항목도 false로 명시된다.
     *
     * @throws kr.co.hconnect.polihealth_galaxy_watch_android_sdk.health.HealthDataSdkException
     *   삼성 기기가 아니거나 삼성헬스 앱이 없는 등 사전 조건 미충족, 또는 SDK 연결 실패 시
     */
    suspend fun checkHealthDataPermission(context: Context): Map<String, Boolean> =
        healthDataManager(context).checkPermission()

    /**
     * 미승인 권한이 있으면 삼성헬스 동의 화면을 띄운다. 반환값 형태는 [checkHealthDataPermission]과 같다.
     */
    suspend fun requestHealthDataPermission(activity: Activity): Map<String, Boolean> =
        healthDataManager(activity).requestPermission(activity)

    /** 최근 [days]일(최대 30일)의 걸음 수를 하루 단위로 조회한다. */
    suspend fun readHealthDataSteps(context: Context, days: Int = 7): List<Map<String, Any>> =
        healthDataManager(context).readSteps(days)

    /** 최근 [days]일(최대 30일)의 심박수를 조회한다. */
    suspend fun readHealthDataHeartRate(context: Context, days: Int = 7): List<Map<String, Any>> =
        healthDataManager(context).readHeartRate(days)

    /** 최근 [days]일(최대 30일)의 혈중 산소포화도를 조회한다. */
    suspend fun readHealthDataBloodOxygen(context: Context, days: Int = 7): List<Map<String, Any>> =
        healthDataManager(context).readBloodOxygen(days)

    /** 최근 [days]일(최대 30일)의 피부온도를 조회한다. */
    suspend fun readHealthDataSkinTemperature(context: Context, days: Int = 7): List<Map<String, Any>> =
        healthDataManager(context).readSkinTemperature(days)
}
