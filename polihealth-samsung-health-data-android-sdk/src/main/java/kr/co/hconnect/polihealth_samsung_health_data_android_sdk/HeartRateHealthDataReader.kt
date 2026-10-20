package kr.co.hconnect.polihealth_samsung_health_data_android_sdk

import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.ChangeType
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * 심박수(Heart Rate) 조회 — `readData()` 사용 (docs/samsung-health-integration.md 8절).
 */
internal class HeartRateHealthDataReader(private val store: HealthDataStore) {

    private companion object {
        /** 페이지 토큰이 끝나지 않는 비정상 응답에 대비한 최대 페이지 수 */
        const val MAX_CHANGE_PAGES = 100
    }

    suspend fun readDays(days: Int): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        HealthDataDateWindow.lastDaysInstant(days).flatMap { (from, to) -> readOneDay(from, to) }
    }

    /**
     * [since]~[until] 사이에 삼성헬스에서 추가·수정·삭제된 심박수만 조회한다 — `readChanges()` 사용.
     *
     * 측정 시각이 아니라 **변경 시각** 기준이라, 워치가 늦게 동기화해서 들어온 과거 측정값도 잡힌다.
     * 30일치를 하루씩 다시 읽는 [readDays]는 심박수만 실측 약 8초가 걸려서(2026-10-20, 480건),
     * 두 번째 동기화부터는 이 함수로 바뀐 것만 읽는다.
     */
    suspend fun readChanges(since: Instant, until: Instant): HealthDataChanges = withContext(Dispatchers.IO) {
        val upserts = mutableListOf<Map<String, Any>>()
        val deletedUids = mutableListOf<String>()
        var pageToken: String? = null
        var pageCount = 0

        do {
            val builder = DataTypes.HEART_RATE.changedDataRequestBuilder
                .setChangeTimeFilter(InstantTimeFilter.of(since, until))
            pageToken?.let { builder.setPageToken(it) }

            val response = withHealthDataErrorMapping { store.readChanges(builder.build()) }
            response.dataList.forEach { change ->
                when (change.changeType) {
                    ChangeType.UPSERT -> change.upsertDataPoint?.let { point ->
                        upserts += toMap(point) + ("uid" to point.uid)
                    }
                    ChangeType.DELETE -> change.deleteDataUid?.let { deletedUids += it }
                    else -> Unit
                }
            }

            pageToken = response.pageToken?.takeIf { it.isNotEmpty() }
        } while (pageToken != null && ++pageCount < MAX_CHANGE_PAGES)

        HealthDataChanges(upserts, deletedUids)
    }

    private suspend fun readOneDay(startInstant: Instant, endInstant: Instant): List<Map<String, Any>> {
        val request = DataTypes.HEART_RATE.readDataRequestBuilder
            .setInstantTimeFilter(InstantTimeFilter.of(startInstant, endInstant))
            .build()

        val response = withHealthDataErrorMapping { store.readData(request) }
        if (response.dataList.isEmpty()) return emptyList()

        return response.dataList.map { toMap(it) }
    }

    private fun toMap(point: HealthDataPoint): Map<String, Any> {
        // getValue()는 필드 타입에 따라 안전한 캐스팅이 필요하다. HEART_RATE 계열은 실측상 Float로 온다
        // (9절 참고) — as? Double 로 캐스팅하면 무조건 실패해서 값이 조용히 드롭된다.
        val bpm = (point.getValue(DataType.HeartRateType.HEART_RATE) as? Float)?.toInt()
        val minBpm = (point.getValue(DataType.HeartRateType.MIN_HEART_RATE) as? Float)?.toInt()
        val maxBpm = (point.getValue(DataType.HeartRateType.MAX_HEART_RATE) as? Float)?.toInt()

        return buildMap {
            put("date", point.startTime.toString())
            bpm?.let { put("bpm", it) }
            minBpm?.let { put("minBpm", it) }
            maxBpm?.let { put("maxBpm", it) }
        }
    }
}
