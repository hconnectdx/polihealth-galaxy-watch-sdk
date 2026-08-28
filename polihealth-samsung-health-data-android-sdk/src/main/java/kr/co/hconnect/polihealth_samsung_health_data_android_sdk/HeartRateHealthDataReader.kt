package kr.co.hconnect.polihealth_samsung_health_data_android_sdk

import com.samsung.android.sdk.health.data.HealthDataStore
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

    suspend fun readDays(days: Int): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        HealthDataDateWindow.lastDaysInstant(days).flatMap { (from, to) -> readOneDay(from, to) }
    }

    private suspend fun readOneDay(startInstant: Instant, endInstant: Instant): List<Map<String, Any>> {
        val request = DataTypes.HEART_RATE.readDataRequestBuilder
            .setInstantTimeFilter(InstantTimeFilter.of(startInstant, endInstant))
            .build()

        val response = store.readData(request)
        if (response.dataList.isEmpty()) return emptyList()

        return response.dataList.map { point ->
            // getValue()는 필드 타입에 따라 안전한 캐스팅이 필요하다. HEART_RATE 계열은 실측상 Float로 온다
            // (9절 참고) — as? Double 로 캐스팅하면 무조건 실패해서 값이 조용히 드롭된다.
            val bpm = (point.getValue(DataType.HeartRateType.HEART_RATE) as? Float)?.toInt()
            val minBpm = (point.getValue(DataType.HeartRateType.MIN_HEART_RATE) as? Float)?.toInt()
            val maxBpm = (point.getValue(DataType.HeartRateType.MAX_HEART_RATE) as? Float)?.toInt()

            buildMap<String, Any> {
                put("date", point.startTime.toString())
                bpm?.let { put("bpm", it) }
                minBpm?.let { put("minBpm", it) }
                maxBpm?.let { put("maxBpm", it) }
            }
        }
    }
}
