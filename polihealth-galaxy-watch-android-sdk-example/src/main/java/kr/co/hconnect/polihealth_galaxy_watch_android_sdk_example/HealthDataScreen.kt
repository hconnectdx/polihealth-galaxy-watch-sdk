package kr.co.hconnect.polihealth_galaxy_watch_android_sdk_example

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kr.co.hconnect.polihealth_galaxy_watch_android_sdk.PolihealthGalaxyWatchAndroidSdk
import kr.co.hconnect.polihealth_galaxy_watch_android_sdk.health.HealthDataSdkException

/**
 * 삼성헬스 Data SDK 연동(걸음수/심박수/산소포화도/피부온도) 수동 테스트 화면.
 *
 * 실제 삼성 기기 + 삼성헬스 앱(로그인된 상태)에서만 의미 있는 결과가 나온다
 * (polihealth-galaxy-watch-android-sdk/docs/samsung-health-integration.md 11절).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDataScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()

    var permissionState by remember { mutableStateOf<Map<String, Boolean>?>(null) }
    var resultLines by remember { mutableStateOf(listOf<String>()) }
    var loading by remember { mutableStateOf(false) }

    fun appendResult(lines: List<String>) {
        resultLines = lines + resultLines
    }

    fun runTask(label: String, task: suspend () -> List<String>) {
        if (loading) return
        loading = true
        scope.launch {
            try {
                appendResult(task())
            } catch (e: HealthDataSdkException) {
                appendResult(listOf("[$label 실패] code=${e.code} msg=${e.message}"))
            } catch (e: Throwable) {
                // NoClassDefFoundError 등 Error 계열은 Exception이 아니라서 별도로 잡아야 화면에 보인다.
                appendResult(listOf("[$label 오류] ${e.javaClass.simpleName}: ${e.message}"))
            } finally {
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("삼성헬스 Data SDK 테스트") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "←", fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PermissionSection(permissionState)

            HorizontalDivider()

            Text(
                text = "권한",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HealthActionButton(
                    text = "권한 확인",
                    modifier = Modifier.weight(1f),
                    enabled = !loading,
                ) {
                    runTask("권한 확인") {
                        val granted = PolihealthGalaxyWatchAndroidSdk.checkHealthDataPermission(context)
                        permissionState = granted
                        listOf("[권한 확인] $granted")
                    }
                }
                HealthActionButton(
                    text = "권한 요청",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF2E7D32),
                    enabled = !loading && activity != null,
                ) {
                    val target = activity ?: return@HealthActionButton
                    runTask("권한 요청") {
                        val granted = PolihealthGalaxyWatchAndroidSdk.requestHealthDataPermission(target)
                        permissionState = granted
                        listOf("[권한 요청] $granted")
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "데이터 조회 (최근 7일)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HealthActionButton(text = "걸음수", modifier = Modifier.weight(1f), enabled = !loading) {
                    runTask("걸음수") {
                        PolihealthGalaxyWatchAndroidSdk.readHealthDataSteps(context).map { "[걸음수] $it" }
                            .ifEmpty { listOf("[걸음수] 데이터 없음") }
                    }
                }
                HealthActionButton(text = "심박수", modifier = Modifier.weight(1f), enabled = !loading) {
                    runTask("심박수") {
                        PolihealthGalaxyWatchAndroidSdk.readHealthDataHeartRate(context).map { "[심박수] $it" }
                            .ifEmpty { listOf("[심박수] 데이터 없음") }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HealthActionButton(text = "산소포화도", modifier = Modifier.weight(1f), enabled = !loading) {
                    runTask("산소포화도") {
                        PolihealthGalaxyWatchAndroidSdk.readHealthDataBloodOxygen(context).map { "[산소포화도] $it" }
                            .ifEmpty { listOf("[산소포화도] 데이터 없음") }
                    }
                }
                HealthActionButton(text = "피부온도", modifier = Modifier.weight(1f), enabled = !loading) {
                    runTask("피부온도") {
                        PolihealthGalaxyWatchAndroidSdk.readHealthDataSkinTemperature(context).map { "[피부온도] $it" }
                            .ifEmpty { listOf("[피부온도] 데이터 없음") }
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "결과",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            ResultSection(resultLines, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PermissionSection(permissionState: Map<String, Boolean>?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "상태",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (permissionState == null) {
            Text(text = "권한 확인 전", color = Color.Gray, fontSize = 14.sp)
        } else {
            permissionState.forEach { (key, granted) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = key, color = Color.Gray, fontSize = 14.sp)
                    Text(
                        text = if (granted) "승인됨" else "미승인",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (granted) Color(0xFF2E7D32) else Color(0xFFB71C1C),
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthActionButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
    ) {
        Text(text = text, fontSize = 14.sp)
    }
}

@Composable
private fun ResultSection(lines: List<String>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
            .padding(8.dp),
    ) {
        if (lines.isEmpty()) {
            item {
                Text(
                    text = "결과가 없습니다",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
        items(lines) { line ->
            Text(
                text = line,
                color = when {
                    "실패" in line || "오류" in line -> Color(0xFFEF5350)
                    else -> Color(0xFFCCCCCC)
                },
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 1.dp),
            )
        }
    }
}
