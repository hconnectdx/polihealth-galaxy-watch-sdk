package kr.co.hconnect.polihealth_galaxy_watch_wearos_sdk.data

sealed interface SdkTrackingState {
    data object Idle : SdkTrackingState
    data object Start : SdkTrackingState
    data object Finish : SdkTrackingState
}
