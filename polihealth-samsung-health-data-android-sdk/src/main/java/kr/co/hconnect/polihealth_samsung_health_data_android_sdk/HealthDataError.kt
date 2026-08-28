package kr.co.hconnect.polihealth_samsung_health_data_android_sdk

/**
 * 삼성헬스 Data SDK 연동 중 발생할 수 있는 예외 상황을 구분하는 코드.
 *
 * [HealthDataStoreProvider], [HealthDataPermissionManager]가 사전 조건 검사 결과나
 * `com.samsung.android.sdk.health.data.error.ResolvablePlatformException`의 errorCode를
 * 이 코드로 매핑해 던진다. 클라이언트는 이 코드로 상황별 안내 문구를 분기할 수 있다.
 *
 * 코드별 상세 설명·권장 조치는 [describe]를 참고. **[describe]가 반환하는 문자열은 개발자가
 * 로그/이슈 트래킹에서 원인을 바로 파악하기 위한 것이지, 최종 사용자에게 그대로 노출할 문구가
 * 아니다** — 사용자에게 보여줄 안내 문구·UI(다이얼로그, 버튼으로 삼성헬스 앱 실행 등)는 이 코드를
 * 소비하는 앱이 각자의 UX/언어에 맞게 설계해야 한다.
 */
enum class HealthDataErrorCode {
    /** 삼성 기기가 아님 (Build.MANUFACTURER/BRAND != samsung). */
    NOT_SAMSUNG_DEVICE,

    /** 삼성헬스 앱이 설치되어 있지 않음 (Samsung ErrorCode.ERR_PLATFORM_NOT_INSTALLED = 3000). */
    SHEALTH_NOT_INSTALLED,

    /** 삼성헬스 앱 버전이 요구 버전(6.30.2)보다 낮음 (Samsung ErrorCode.ERR_OLD_VERSION_PLATFORM = 3001). */
    SHEALTH_OUTDATED,

    /** 삼성헬스 앱이 비활성화(사용 안 함) 상태 (Samsung ErrorCode.ERR_PLATFORM_DISABLED = 3002). */
    SHEALTH_DISABLED,

    /**
     * 삼성헬스 앱은 설치·활성화돼 있지만 OOBE(Out-Of-Box Experience, 최초 실행 시 거치는 약관 동의/
     * 로그인 등 초기 설정)를 아직 완료하지 않음 (Samsung ErrorCode.ERR_PLATFORM_NOT_INITIALIZED = 3003).
     * `HealthDataService.getStore()`는 성공하고, 실제 데이터 조회/권한 조회 시점에 이 코드로 실패한다.
     */
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
 * 개발자 참고용 설명 문자열 — "무엇이 문제인지"와 "무엇을 해야 해결되는지"를 함께 담는다.
 * 로그/`HealthDataSdkException.message`에 쓰기 위한 것으로, 최종 사용자용 문구가 아니다.
 */
fun HealthDataErrorCode.describe(): String = when (this) {
    HealthDataErrorCode.NOT_SAMSUNG_DEVICE ->
        "이 기기는 삼성 기기가 아닙니다. 삼성헬스 Data SDK는 삼성 기기에서만 동작합니다."

    HealthDataErrorCode.SHEALTH_NOT_INSTALLED ->
        "삼성헬스 앱이 설치되어 있지 않습니다. Play 스토어에서 '삼성 헬스' 앱을 설치해야 합니다."

    HealthDataErrorCode.SHEALTH_OUTDATED ->
        "삼성헬스 앱 버전이 요구 버전(6.30.2 이상)보다 낮습니다. Play 스토어에서 삼성헬스 앱을 최신 버전으로 업데이트해야 합니다."

    HealthDataErrorCode.SHEALTH_DISABLED ->
        "삼성헬스 앱이 비활성화(사용 안 함) 상태입니다. 설정 > 앱 목록에서 삼성헬스 앱을 다시 활성화해야 합니다."

    HealthDataErrorCode.SHEALTH_NOT_INITIALIZED ->
        "삼성헬스 앱의 초기 설정(OOBE)이 완료되지 않았습니다. 사용자가 삼성헬스 앱을 직접 실행해서 " +
            "약관 동의·로그인 등 최초 설정을 완료해야 이 SDK로 데이터를 읽을 수 있습니다."

    HealthDataErrorCode.ACCESS_CONTROL_DENIED ->
        "삼성헬스 권한 요청이 거부되었습니다. 파트너 승인을 받지 않은 데이터 타입이 요청 세트에 " +
            "포함되어 있을 가능성이 높습니다 — 승인된 타입(걸음수/심박수/산소포화도/피부온도)만 " +
            "요청했는지 확인하세요."

    HealthDataErrorCode.UNKNOWN ->
        "원인을 특정할 수 없는 오류입니다. cause 예외의 스택트레이스를 확인하세요."
}

/**
 * [HealthDataErrorCode]를 담은 polihealth-samsung-health-data-android-sdk 공용 예외.
 *
 * Samsung Health Data SDK의 예외 타입을 그대로 외부로 노출하지 않고 이 예외로 감싸서 던진다
 * (health-data-api는 implementation 스코프라 공개 API 시그니처에 등장할 수 없다).
 */
class HealthDataSdkException(
    val code: HealthDataErrorCode,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
