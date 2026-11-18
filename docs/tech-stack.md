# Tech Stack

> Kotlin/Jetpack Compose 앱의 스택. 지금은 후보이고, TODO 1단계에서 하나씩 확정하며 이 표를 고친다.
> 결정 근거는 `decisions.md`(AOS-xxx)에 적고 여기에는 결과만 적는다. 버전은 확정할 때 웹 검색으로 최신 안정판을 확인하고 출처 날짜를 함께 적는다.
> 서버·DB·배치는 `../futureETP/docs/tech-stack.md`(공용).

상태: 후보 (2026-11-18 작성)

| 영역 | 후보 / 선택 | 메모 |
|---|---|---|
| 언어 / 빌드 | Kotlin(최신 안정판은 스캐폴딩 때 확인) · Gradle Kotlin DSL + version catalog(`libs.versions.toml`) · AGP 9.1.1 이상 | JDK 17(D-055) |
| UI | Jetpack Compose + Material 3, BOM 2026.08.00(Compose 1.12) | [Android Developers Blog 2026-08](https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html) |
| 상태 | ViewModel + `StateFlow`, 코루틴 | RN TanStack Query 역할(캐시·staleTime·자동 새로고침)은 repository 계층에서 직접 |
| 내비게이션 | Navigation Compose(타입 안전 route) 또는 Navigation 3 | A → C 스택, C 하단 탭, 탭 밖 C-d-a. 1단계에서 정한다 |
| DI | 수동 DI(앱 컨테이너) 우선, 필요하면 Hilt | 개인 앱 규모라 수동으로 시작 |
| HTTP / JSON | Ktor Client(OkHttp 엔진) 또는 Retrofit + kotlinx.serialization | 타임아웃 15초, 디코딩 실패 → `SCHEMA_MISMATCH` |
| 차트 | Compose `Canvas` 직접 구현(후보), Vico 검토 | C-a 봉 756~3,000개 + 핀치·드래그·십자선. 스파이크로 정한다 |
| 보안 저장소 | Keystore 기반 암호화(Tink + DataStore 등) | 접근 키(D-044)·installation ID. `EncryptedSharedPreferences`는 deprecated라 쓰지 않는다 |
| 기기 설정 저장 | DataStore(Preferences) | 언어·등락 색(D-073) |
| QR | CameraX + ML Kit 바코드 스캐닝 | B-b(D-078) |
| 푸시 | Firebase Cloud Messaging | RN과 같은 Firebase 앱 |
| 햅틱 | `HapticFeedback`(Compose) / `View.performHapticFeedback` | D-109·D-138·D-139 |
| 테스트 / lint | JUnit · coroutines-test · Compose UI test / Android lint · ktlint · detekt |  |

## RN 스택과의 대응

| RN(`../futureETP`) | 이 앱 |
|---|---|
| TanStack Query(서버 상태·캐시·자동 새로고침) | 상태 계층 + repository 캐시 |
| Zustand persist(언어·등락 색) | 기기 설정 저장 |
| Victory Native XL + Skia | 위 "차트" |
| expo-secure-store | 위 "보안 저장소" |
| expo-camera | 위 "QR" |
| i18next | 위 "i18n" |
| EAS Update(OTA) | 없음(재설치) |
