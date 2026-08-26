package kr.co.hconnect.polihealth_galaxy_watch_android_sdk.health

import android.app.Activity
import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.AuthorizationException
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "HealthDataPermissionManager"

/**
 * 삼성헬스 Data SDK 권한 확인/요청.
 *
 * 연동 범위(docs/samsung-health-integration.md 1절): 걸음수, 심박수, 산소포화도, 피부온도.
 * 그 외 데이터 타입은 요청 세트에 절대 포함하지 않는다 — 파트너 승인을 받지 않은 타입이
 * 하나라도 섞이면 동의 화면조차 뜨지 않고 [HealthDataErrorCode.ACCESS_CONTROL_DENIED]로
 * 요청 전체가 반려된다 (5절 함정 참고).
 */
internal class HealthDataPermissionManager(private val store: HealthDataStore) {

    suspend fun getCurrentAuth(): Set<Permission> = withContext(Dispatchers.IO) {
        store.getGrantedPermissions(REQUIRED_PERMISSIONS)
    }

    suspend fun requestAuth(activity: Activity): Set<Permission> = withContext(Dispatchers.IO) {
        val granted = getCurrentAuth()
        if (granted.containsAll(REQUIRED_PERMISSIONS)) return@withContext granted

        try {
            store.requestPermissions(REQUIRED_PERMISSIONS, activity)
        } catch (e: AuthorizationException) {
            Log.e(TAG, "권한 요청 실패", e)
            throw HealthDataSdkException(
                HealthDataErrorCode.ACCESS_CONTROL_DENIED,
                "삼성헬스 권한 요청이 거부되었습니다. 파트너 승인된 데이터 타입만 요청했는지 확인하세요.",
                e,
            )
        }
        // 동의 화면 처리 후 최신 승인 상태를 다시 조회한다 (requestPermissions 반환값을 신뢰하지 않음).
        getCurrentAuth()
    }

    /** 미승인 항목도 false로 명시적으로 내려줘야 클라이언트가 구분 가능. */
    fun extractPermissions(granted: Set<Permission>): Map<String, Boolean> = mapOf(
        "read_step" to granted.contains(Permission.of(DataTypes.STEPS, AccessType.READ)),
        "read_heartRate" to granted.contains(Permission.of(DataTypes.HEART_RATE, AccessType.READ)),
        "read_oxygenSaturation" to granted.contains(Permission.of(DataTypes.BLOOD_OXYGEN, AccessType.READ)),
        "read_skinTemperature" to granted.contains(Permission.of(DataTypes.SKIN_TEMPERATURE, AccessType.READ)),
    )

    companion object {
        val REQUIRED_PERMISSIONS: Set<Permission> = setOf(
            Permission.of(DataTypes.STEPS, AccessType.READ),
            Permission.of(DataTypes.HEART_RATE, AccessType.READ),
            Permission.of(DataTypes.BLOOD_OXYGEN, AccessType.READ),
            Permission.of(DataTypes.SKIN_TEMPERATURE, AccessType.READ),
        )
    }
}
