# 로컬 Maven 저장소 (Samsung Health Data API)

Samsung Health Data API AAR은 Maven Central에 공개되어 있지 않고 파트너에게 개별 배포되는
`.aar` 파일이라 저장소(git)에는 실물 파일을 커밋해두지 않았다(단, 로컬에서 직접 넣은 뒤 커밋하는 것은
`polihealth-galaxy-watch-wearos-sdk`의 `samsung-health-sensor-api-1.4.1.aar`와 동일하게 이 저장소의 기존 관례다).
빌드하려면 파트너로부터 받은 `samsung-health-data-api-1.1.0.aar` 파일(원본 파일명 그대로)을
아래 경로에 직접 추가해야 한다:

```
polihealth-samsung-health-data-sdk/libs/repo/com/samsung/android/sdk/health/samsung-health-data-api/1.1.0/samsung-health-data-api-1.1.0.aar
```

같은 디렉터리의 `samsung-health-data-api-1.1.0.pom`은 이미 구성되어 있으므로 `.aar` 파일만 추가하면 된다.
버전을 올릴 때는 `gradle/libs.versions.toml`의 `samsungHealthDataApi` 값과 이 디렉터리 구조(버전 폴더명 +
pom의 `<version>`)를 함께 맞춰야 한다.

⚠️ 파일명이 정확히 `samsung-health-data-api-1.1.0.aar`이어야 한다. Maven 로컬 저장소는
`<artifactId>-<version>.aar` 형식의 파일명으로만 GAV 좌표를 찾으므로, 파트너가 준 원본 파일명을
그대로 쓰면 된다(리네임 불필요).

자세한 배경은 [docs/samsung-health-integration.md](../docs/samsung-health-integration.md) 4절 참고.
