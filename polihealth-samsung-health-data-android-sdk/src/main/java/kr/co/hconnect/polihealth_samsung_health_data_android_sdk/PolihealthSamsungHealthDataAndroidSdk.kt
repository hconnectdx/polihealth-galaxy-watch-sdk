package kr.co.hconnect.polihealth_samsung_health_data_android_sdk

import android.app.Activity
import android.content.Context

/**
 * polihealth-samsung-health-data-android-sdk 진입점.
 *
 * 삼성헬스 앱에 저장된 걸음수/심박수/산소포화도/피부온도를 직접 읽어온다.
 * 자세한 배경은 `polihealth-samsung-health-data-android-sdk/docs/samsung-health-integration.md` 참고.
 *
 * ## 사용 순서
 * ```kotlin
 * val granted = PolihealthSamsungHealthDataAndroidSdk.checkHealthDataPermission(context)
 * if (granted.values.any { !it }) {
 *     PolihealthSamsungHealthDataAndroidSdk.requestHealthDataPermission(activity)
 * }
 * val steps = PolihealthSamsungHealthDataAndroidSdk.readHealthDataSteps(context, days = 7)
 * val heartRate = PolihealthSamsungHealthDataAndroidSdk.readHealthDataHeartRate(context, days = 7)
 * val bloodOxygen = PolihealthSamsungHealthDataAndroidSdk.readHealthDataBloodOxygen(context, days = 7)
 * val skinTemp = PolihealthSamsungHealthDataAndroidSdk.readHealthDataSkinTemperature(context, days = 7)
 * ```
 */
object PolihealthSamsungHealthDataAndroidSdk {

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
     * @throws HealthDataSdkException
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
