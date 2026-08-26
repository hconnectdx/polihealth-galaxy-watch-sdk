package kr.co.hconnect.polihealth_galaxy_watch_wearos_sdk.tracker

import android.content.Context
import com.samsung.android.service.health.tracking.HealthTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kr.co.hconnect.polihealth_galaxy_watch_wearos_sdk.data.SdkTrackingState
import kr.co.hconnect.polihealth_galaxy_watch_wearos_sdk.proto.SensorType

interface TrackerProcessor {
    var context: Context
    var coroutineScope: CoroutineScope
    val trackingState: StateFlow<SdkTrackingState>

    suspend fun startTracking(
        service: HealthTrackingService,
        durationMillis: Long,
        slotMinute: Int?,
        measurementType: Set<SensorType>
    )

    suspend fun startTracking(
        service: HealthTrackingService,
        timestamp: Long?,
        measurementType: Set<SensorType>
    )

    fun finishTracking()
}
