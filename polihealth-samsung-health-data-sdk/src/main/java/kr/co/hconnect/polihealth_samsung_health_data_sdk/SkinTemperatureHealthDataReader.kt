package kr.co.hconnect.polihealth_samsung_health_data_sdk

import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.entries.SkinTemperature
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * 피부온도(Skin Temperature) 조회 — `readData()` 사용.
 *
 * 걸음수/심박수/산소포화도와 달리 `DataType.SkinTemperatureType.SERIES_DATA` 필드는 단일 값이
 * 아니라 연속 측정 구간 목록(`List<SkinTemperature>`)으로 온다. docs/samsung-health-integration.md
 * 9절의 getValue() 반환 타입 주의사항과 같은 이유로, 실기기 로그로 실제 런타임 타입을 재확인할 것.
 */
internal class SkinTemperatureHealthDataReader(private val store: HealthDataStore) {

    suspend fun readDays(days: Int): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        HealthDataDateWindow.lastDaysInstant(days).flatMap { (from, to) -> readOneDay(from, to) }
    }

    private suspend fun readOneDay(startInstant: Instant, endInstant: Instant): List<Map<String, Any>> {
        val request = DataTypes.SKIN_TEMPERATURE.readDataRequestBuilder
            .setInstantTimeFilter(InstantTimeFilter.of(startInstant, endInstant))
            .build()

        val response = store.readData(request)
        if (response.dataList.isEmpty()) return emptyList()

        return response.dataList.flatMap { point ->
            val rawSeries = point.getValue(DataType.SkinTemperatureType.SERIES_DATA)
            val series = when (rawSeries) {
                is List<*> -> rawSeries.filterIsInstance<SkinTemperature>()
                else -> emptyList()
            }
            series.map { entry ->
                mapOf(
                    "date" to entry.startTime.toString(),
                    "value" to entry.skinTemperature.toDouble(),
                    "minValue" to entry.min.toDouble(),
                    "maxValue" to entry.max.toDouble(),
                )
            }
        }
    }
}
