package kr.co.hconnect.polihealth_samsung_health_data_sdk

import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * 혈중 산소포화도(Blood Oxygen) 조회 — `readData()` 사용 (docs/samsung-health-integration.md 8절).
 */
internal class BloodOxygenHealthDataReader(private val store: HealthDataStore) {

    suspend fun readDays(days: Int): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        HealthDataDateWindow.lastDaysInstant(days).flatMap { (from, to) -> readOneDay(from, to) }
    }

    private suspend fun readOneDay(startInstant: Instant, endInstant: Instant): List<Map<String, Any>> {
        val request = DataTypes.BLOOD_OXYGEN.readDataRequestBuilder
            .setInstantTimeFilter(InstantTimeFilter.of(startInstant, endInstant))
            .build()

        val response = store.readData(request)
        if (response.dataList.isEmpty()) return emptyList()

        return response.dataList.map { point ->
            // OXYGEN_SATURATION 계열도 실측상 Float로 온다 (9절 참고).
            val value = (point.getValue(DataType.BloodOxygenType.OXYGEN_SATURATION) as? Float)?.toDouble()
            val minValue = (point.getValue(DataType.BloodOxygenType.MIN_OXYGEN_SATURATION) as? Float)?.toDouble()
            val maxValue = (point.getValue(DataType.BloodOxygenType.MAX_OXYGEN_SATURATION) as? Float)?.toDouble()

            buildMap<String, Any> {
                put("date", point.startTime.toString())
                value?.let { put("value", it) }
                minValue?.let { put("minValue", it) }
                maxValue?.let { put("maxValue", it) }
            }
        }
    }
}
