package kr.co.hconnect.polihealth_samsung_health_data_sdk

/**
 * 삼성헬스 Data SDK 연동 중 발생할 수 있는 예외 상황을 구분하는 코드.
 *
 * [HealthDataStoreProvider], [HealthDataPermissionManager]가 사전 조건 검사 결과나
 * `com.samsung.android.sdk.health.data.error.ResolvablePlatformException`의 errorCode를
 * 이 코드로 매핑해 던진다. 클라이언트는 이 코드로 상황별 안내 문구를 분기할 수 있다.
 */
enum class HealthDataErrorCode {
    /** 삼성 기기가 아님 (Build.MANUFACTURER/BRAND != samsung). */
    NOT_SAMSUNG_DEVICE,

    /** 삼성헬스 앱이 설치되어 있지 않음. */
    SHEALTH_NOT_INSTALLED,

    /** 삼성헬스 앱 버전이 요구 버전(6.30.2)보다 낮음. */
    SHEALTH_OUTDATED,

    /** 삼성헬스 앱이 비활성화(사용 안 함) 상태. */
    SHEALTH_DISABLED,

    /** 삼성헬스 앱에 사용자가 아직 로그인/초기 설정을 하지 않음. */
    SHEALTH_NOT_INITIALIZED,

    /**
     * 요청한 권한 세트에 파트너(이 앱)가 승인받지 못한 데이터 타입이 하나라도 포함되어
     * 삼성 측에서 동의 화면조차 띄우지 않고 요청 전체를 반려한 경우 (ERR_ACCESS_CONTROL, 2003).
     */
    ACCESS_CONTROL_DENIED,

    /** 위에 해당하지 않는 알 수 없는 오류. */
    UNKNOWN,
}

/**
 * [HealthDataErrorCode]를 담은 polihealth-samsung-health-data-sdk 공용 예외.
 *
 * Samsung Health Data SDK의 예외 타입을 그대로 외부로 노출하지 않고 이 예외로 감싸서 던진다
 * (health-data-api는 implementation 스코프라 공개 API 시그니처에 등장할 수 없다).
 */
class HealthDataSdkException(
    val code: HealthDataErrorCode,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
