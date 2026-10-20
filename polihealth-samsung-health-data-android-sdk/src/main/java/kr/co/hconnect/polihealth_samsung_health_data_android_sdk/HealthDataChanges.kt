package kr.co.hconnect.polihealth_samsung_health_data_android_sdk

/**
 * 변경분 조회(`readChanges`) 결과.
 *
 * @property upserts 조회 구간 안에 추가·수정된 데이터. 키는 같은 항목의 일반 조회 결과와 같고,
 *   삼성헬스 데이터 ID인 `uid`가 추가된다.
 * @property deletedUids 조회 구간 안에 삼성헬스에서 삭제된 데이터의 `uid`
 */
data class HealthDataChanges(
    val upserts: List<Map<String, Any>>,
    val deletedUids: List<String>,
)
