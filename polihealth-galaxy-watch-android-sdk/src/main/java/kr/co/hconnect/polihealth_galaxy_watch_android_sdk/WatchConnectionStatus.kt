package kr.co.hconnect.polihealth_galaxy_watch_android_sdk

/**
 * [PolihealthGalaxyWatchAndroidSdk.getConnectionStatus]가 돌려주는, 조회 시점의 워치 연결 상태.
 *
 * @property serviceRunning 워치 수신 서비스([PolihealthGalaxyWatchAndroidSdk.start])가 살아 있는지
 * @property connected      이 SDK가 맺은 워치 BLE 연결이 지금 살아 있는지
 * @property deviceAddress  연결된 워치 주소 (연결 안 됐으면 null)
 * @property deviceName     연결된 워치 이름 (연결 안 됐거나 이름을 못 읽으면 null)
 */
data class WatchConnectionStatus(
    val serviceRunning: Boolean,
    val connected: Boolean,
    val deviceAddress: String?,
    val deviceName: String?,
)
