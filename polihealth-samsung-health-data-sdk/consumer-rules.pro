# Samsung Health Data SDK — 내부적으로 축약된(a, a0, b1 ...) 클래스명 + AIDL(IHealthDataStore) +
# Parcelable(CREATOR 리플렉션) + Binder 로 프로세스 간 통신하므로, 소비 앱에서 R8/minify가 켜지면
# 이 클래스들이 이름 변경/제거되어 NoClassDefFoundError 로 이어질 수 있다. 전체를 keep.
-keep class com.samsung.android.sdk.health.** { *; }
-keepclassmembers class com.samsung.android.sdk.health.** {
    public static final android.os.Parcelable$Creator *;
}
