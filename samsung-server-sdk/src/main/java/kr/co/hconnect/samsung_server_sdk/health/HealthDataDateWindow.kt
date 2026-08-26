package kr.co.hconnect.samsung_server_sdk.health

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 데이터 조회 공용 날짜 윈도우 유틸 (docs/samsung-health-integration.md 6절).
 *
 * 삼성헬스 Data SDK는 커서 페이징을 지원하지 않으므로, 최대 30일까지 하루 단위로
 * `[오늘, 오늘-1, ...]` 순서의 (시작, 끝) 구간 목록을 만들어 반복 조회하는 방식을 쓴다.
 */
internal object HealthDataDateWindow {

    private const val MAX_DAYS = 30

    /** LocalDateTime 기준 구간 목록 (걸음수 등 aggregateData + LocalTimeFilter 에 사용). */
    fun lastDaysLocal(days: Int): List<Pair<LocalDateTime, LocalDateTime>> {
        val targetDays = days.coerceIn(1, MAX_DAYS)
        val today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
        return (0 until targetDays).map { i ->
            val from = today.minusDays(i.toLong())
            val to = from.plusDays(1).minusSeconds(1)
            from to to
        }
    }

    /** Instant 기준 구간 목록 (심박수/산소포화도/피부온도 등 readData + InstantTimeFilter 에 사용). */
    fun lastDaysInstant(days: Int, zoneId: ZoneId = ZoneId.systemDefault()): List<Pair<Instant, Instant>> =
        lastDaysLocal(days).map { (from, to) -> from.atZone(zoneId).toInstant() to to.atZone(zoneId).toInstant() }
}
