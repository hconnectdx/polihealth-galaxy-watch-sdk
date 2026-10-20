package kr.co.hconnect.polihealth_samsung_health_data_android_sdk_example

import android.content.Context
import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.data.entries.OxygenSaturation
import com.samsung.android.sdk.health.data.data.entries.SkinTemperature
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * [디버그 전용] 수면 중 산소포화도·피부온도가 실제로 몇 분 간격으로 저장되는지 확인한다.
 * 사내 SDK는 레코드 대표값만 읽으므로, 여기서는 삼성헬스 Data SDK로 레코드와 SERIES_DATA를 그대로 읽어
 * 레코드 시작~종료, series 항목 간격을 계산해 logcat(tag=IntervalProbe)과 화면에 남긴다.
 */
object IntervalProbe {
    private const val TAG = "IntervalProbe"
    private val fmt = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    private data class Entry(val start: Instant, val end: Instant, val value: Float)

    suspend fun run(context: Context, days: Long = 30): List<String> {
        val store = HealthDataService.getStore(context.applicationContext)
        val to = Instant.now()
        val from = to.minus(Duration.ofDays(days))
        val out = mutableListOf<String>()

        val oxygen = store.readData(
            DataTypes.BLOOD_OXYGEN.readDataRequestBuilder.setInstantTimeFilter(InstantTimeFilter.of(from, to)).build()
        ).dataList
        out += analyze("산소포화도", oxygen) { point ->
            seriesOf(point.getValue(DataType.BloodOxygenType.SERIES_DATA)) { e: OxygenSaturation ->
                Entry(e.startTime, e.endTime, e.oxygenSaturation)
            }
        }

        val skin = store.readData(
            DataTypes.SKIN_TEMPERATURE.readDataRequestBuilder.setInstantTimeFilter(InstantTimeFilter.of(from, to)).build()
        ).dataList
        out += analyze("피부온도", skin) { point ->
            seriesOf(point.getValue(DataType.SkinTemperatureType.SERIES_DATA)) { e: SkinTemperature ->
                Entry(e.startTime, e.endTime, e.skinTemperature)
            }
        }

        out.forEach { Log.i(TAG, it) }
        return out
    }

    private inline fun <reified T> seriesOf(raw: Any?, map: (T) -> Entry): List<Entry> =
        (raw as? List<*>)?.filterIsInstance<T>()?.map(map).orEmpty()

    private fun analyze(label: String, points: List<HealthDataPoint>, series: (HealthDataPoint) -> List<Entry>): List<String> {
        val lines = mutableListOf("===== $label: 레코드 ${points.size}건 (최근 30일)")
        val sorted = points.sortedBy { it.startTime }
        sorted.forEachIndexed { i, p ->
            val end = p.endTime ?: p.startTime
            val s = series(p).sortedBy { it.start }
            val gaps = s.zipWithNext { a, b -> Duration.between(a.start, b.start).seconds }
            val lens = s.map { Duration.between(it.start, it.end).seconds }
            lines += "[$label #$i] ${fmt.format(p.startTime)} ~ ${fmt.format(end)} " +
                "(${Duration.between(p.startTime, end).toMinutes()}분) series=${s.size}건 " +
                "간격(초) ${stat(gaps)} · 항목길이(초) ${stat(lens)} · 기기=${p.dataSource?.deviceId}"
            s.take(4).forEach { e -> lines += "    · ${fmt.format(e.start)}~${fmt.format(e.end)} ${e.value}" }
        }
        val recordGaps = sorted.zipWithNext { a, b -> Duration.between(a.startTime, b.startTime).seconds }
        lines += "[$label] 레코드 시작 간격(초) ${stat(recordGaps)}"
        return lines
    }

    /** 최소/중앙/최대 */
    private fun stat(values: List<Long>): String {
        if (values.isEmpty()) return "-"
        val s = values.sorted()
        return "min=${s.first()} med=${s[s.size / 2]} max=${s.last()} (n=${s.size})"
    }
}
