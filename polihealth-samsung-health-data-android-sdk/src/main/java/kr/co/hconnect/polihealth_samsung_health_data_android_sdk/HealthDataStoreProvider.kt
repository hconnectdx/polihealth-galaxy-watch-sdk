package kr.co.hconnect.polihealth_samsung_health_data_android_sdk

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException

private const val TAG = "HealthDataStoreProvider"

/** 삼성헬스(폰) 앱 패키지명. */
private const val SHEALTH_PACKAGE = "com.sec.android.app.shealth"

/**
 * [HealthDataStore] 연결을 담당한다.
 *
 * docs/samsung-health-integration.md 3절 요구사항대로, SDK 호출 전에 삼성 기기 여부 /
 * 삼성헬스 앱 설치 여부를 먼저 확인해 명확한 [HealthDataErrorCode]로 알려준다.
 * 그 외 SDK 자체 연결 실패(`ResolvablePlatformException`)는 errorCode를 매핑해 재던진다.
 */
internal class HealthDataStoreProvider(context: Context) {

    private val appContext = context.applicationContext

    @Volatile
    private var store: HealthDataStore? = null

    /**
     * 연결된 [HealthDataStore]를 반환한다. 최초 호출 시에만 실제 연결을 시도하고 캐시한다.
     *
     * @param activity 삼성헬스 설치/업데이트/활성화가 필요한 "해결 가능한" 오류일 때,
     *                 넘겨주면 해당 안내 화면을 바로 띄운다(`ResolvablePlatformException.resolve`).
     * @throws HealthDataSdkException 사전 조건 미충족 또는 SDK 연결 실패 시
     */
    fun getStore(activity: Activity? = null): HealthDataStore {
        store?.let { return it }

        ensureDevicePreconditions()

        return try {
            HealthDataService.getStore(appContext).also { store = it }
        } catch (e: ResolvablePlatformException) {
            val code = mapSamsungErrorCode(e.errorCode)
            Log.e(TAG, "HealthDataStore 연결 실패 (해결 가능): ${e.errorCode}", e)
            if (activity != null && e.hasResolution) {
                // 삼성헬스 설치/업데이트/활성화 안내 화면을 띄운다.
                e.resolve(activity)
            }
            throw HealthDataSdkException(code, "삼성헬스 연결에 문제가 있습니다: ${e.errorCode}", e)
        } catch (e: HealthDataException) {
            Log.e(TAG, "HealthDataStore 연결 실패", e)
            throw HealthDataSdkException(
                HealthDataErrorCode.UNKNOWN,
                "삼성헬스 연결 중 알 수 없는 오류가 발생했습니다.",
                e,
            )
        }
    }

    /** 삼성 기기 여부 + 삼성헬스 앱 설치 여부를 SDK 호출 전에 먼저 판단한다. */
    private fun ensureDevicePreconditions() {
        if (!isSamsungDevice()) {
            throw HealthDataSdkException(
                HealthDataErrorCode.NOT_SAMSUNG_DEVICE,
                "삼성 기기에서만 삼성헬스 Data SDK를 사용할 수 있습니다. (MANUFACTURER=${Build.MANUFACTURER})",
            )
        }
        if (!isShealthInstalled()) {
            throw HealthDataSdkException(
                HealthDataErrorCode.SHEALTH_NOT_INSTALLED,
                "삼성헬스 앱이 설치되어 있지 않습니다.",
            )
        }
    }

    private fun isSamsungDevice(): Boolean =
        Build.MANUFACTURER.equals("samsung", ignoreCase = true) ||
            Build.BRAND.equals("samsung", ignoreCase = true)

    private fun isShealthInstalled(): Boolean = try {
        appContext.packageManager.getPackageInfo(SHEALTH_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
