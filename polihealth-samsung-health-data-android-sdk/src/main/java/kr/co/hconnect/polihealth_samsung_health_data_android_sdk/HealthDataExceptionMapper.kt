package kr.co.hconnect.polihealth_samsung_health_data_android_sdk

import android.app.Activity
import android.util.Log
import com.samsung.android.sdk.health.data.error.ErrorCode
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException

private const val TAG = "HealthDataExceptionMapper"

/** ErrorCode는 enum이 아니라 Int 상수 모음이고, HealthDataException.errorCode도 Int?(nullable)로 온다. */
internal fun mapSamsungErrorCode(errorCode: Int?): HealthDataErrorCode = when (errorCode) {
    ErrorCode.ERR_PLATFORM_NOT_INSTALLED -> HealthDataErrorCode.SHEALTH_NOT_INSTALLED
    ErrorCode.ERR_OLD_VERSION_PLATFORM -> HealthDataErrorCode.SHEALTH_OUTDATED
    ErrorCode.ERR_PLATFORM_DISABLED -> HealthDataErrorCode.SHEALTH_DISABLED
    ErrorCode.ERR_PLATFORM_NOT_INITIALIZED -> HealthDataErrorCode.SHEALTH_NOT_INITIALIZED
    else -> HealthDataErrorCode.UNKNOWN
}

/**
 * Samsung Health Data SDK 호출(HealthDataStore의 suspend fun들)을 감싸서
 * `ResolvablePlatformException`/`HealthDataException`을 [HealthDataSdkException]으로 통일해 던진다.
 *
 * `HealthDataStore.getGrantedPermissions()`/`readData()`/`aggregateData()` 등은 [HealthDataStoreProvider]가
 * `HealthDataStore` 핸들 자체를 얻는 시점(`HealthDataService.getStore()`)과는 별개로, 실제 데이터를
 * 조회하는 이 시점에서도 OOBE 미완료(`ERR_PLATFORM_NOT_INITIALIZED`) 등으로 실패할 수 있다 — 두 지점 모두
 * 감싸야 한다.
 *
 * ⚠️ [HealthDataPermissionManager.requestAuth]의 `requestPermissions()` 호출처럼 `AuthorizationException`을
 * 별도로 구분해야 하는 곳에서는 이 함수를 쓰지 말 것 — `AuthorizationException`도 `HealthDataException`의
 * 하위 타입이라 이 함수의 catch에 먼저 잡혀 `ACCESS_CONTROL_DENIED` 대신 `UNKNOWN`으로 뭉개진다.
 */
internal suspend fun <T> withHealthDataErrorMapping(activity: Activity? = null, block: suspend () -> T): T =
    try {
        block()
    } catch (e: ResolvablePlatformException) {
        val code = mapSamsungErrorCode(e.errorCode)
        Log.e(TAG, "삼성헬스 연동 실패 (해결 가능): ${e.errorCode}", e)
        if (activity != null && e.hasResolution) {
            // 삼성헬스 설치/업데이트/활성화 안내 화면을 띄운다.
            e.resolve(activity)
        }
        throw HealthDataSdkException(code, "${code.describe()} (Samsung ErrorCode=${e.errorCode})", e)
    } catch (e: HealthDataException) {
        Log.e(TAG, "삼성헬스 연동 중 알 수 없는 오류", e)
        throw HealthDataSdkException(HealthDataErrorCode.UNKNOWN, HealthDataErrorCode.UNKNOWN.describe(), e)
    }
