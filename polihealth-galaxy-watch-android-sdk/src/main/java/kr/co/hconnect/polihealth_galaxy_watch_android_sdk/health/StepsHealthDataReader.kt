package kr.co.hconnect.polihealth_galaxy_watch_android_sdk.health

import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * 걸음 수(Steps) 조회 — `aggregateData()` 사용 (docs/samsung-health-integration.md 7절).
 *
 * `DataTypes.STEPS`는 `readData()`를 지원하지 않고 `AggregateOperation`(TOTAL)만 제공하므로
 * 하루 단위 합계를 [DataType.StepsType.TOTAL]로 구한다.
 */
internal class StepsHealthDataReader(private val store: HealthDataStore) {

    suspend fun readDays(days: Int): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        HealthDataDateWindow.lastDaysLocal(days).mapNotNull { (from, to) -> readOneDay(from, to) }
    }

    private suspend fun readOneDay(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
    ): Map<String, Any>? {
        val request = DataType.StepsType.TOTAL.requestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(startDateTime, endDateTime))
            .build()

        val response = store.aggregateData(request)
        val totalSteps = response.dataList.sumOf { (it.value as? Long) ?: 0L }

        if (totalSteps == 0L) return null
        return mapOf("date" to startDateTime.toString(), "step" to totalSteps.toInt())
    }
}
