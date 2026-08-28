package kr.co.hconnect.polihealth_samsung_health_data_android_sdk

import android.app.Activity
import android.content.Context

/**
 * 삼성헬스 Data SDK 연동의 내부 진입점.
 *
 * [PolihealthSamsungHealthDataAndroidSdk]가 이 클래스를 감싸 공개 API로 노출한다.
 * 연동 범위: 걸음수, 심박수, 산소포화도, 피부온도 (docs/samsung-health-integration.md 1절).
 */
internal class HealthDataManager(context: Context) {

    private val storeProvider = HealthDataStoreProvider(context)

    suspend fun checkPermission(): Map<String, Boolean> {
        val permissionManager = HealthDataPermissionManager(storeProvider.getStore())
        return permissionManager.extractPermissions(permissionManager.getCurrentAuth())
    }

    suspend fun requestPermission(activity: Activity): Map<String, Boolean> {
        val permissionManager = HealthDataPermissionManager(storeProvider.getStore(activity))
        return permissionManager.extractPermissions(permissionManager.requestAuth(activity))
    }

    suspend fun readSteps(days: Int): List<Map<String, Any>> =
        StepsHealthDataReader(storeProvider.getStore()).readDays(days)

    suspend fun readHeartRate(days: Int): List<Map<String, Any>> =
        HeartRateHealthDataReader(storeProvider.getStore()).readDays(days)

    suspend fun readBloodOxygen(days: Int): List<Map<String, Any>> =
        BloodOxygenHealthDataReader(storeProvider.getStore()).readDays(days)

    suspend fun readSkinTemperature(days: Int): List<Map<String, Any>> =
        SkinTemperatureHealthDataReader(storeProvider.getStore()).readDays(days)
}
