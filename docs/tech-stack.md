# Tech Stack

> Kotlin/Jetpack Compose 앱의 스택. TODO 1단계에서 확정했다(차트만 4단계 스파이크로).
> 결정 근거는 `decisions.md`(AOS-xxx)에 적고 여기에는 결과만 적는다. 버전은 확정할 때 웹 검색으로 최신 안정판을 확인하고 출처 날짜를 함께 적는다.
> 서버·DB·배치는 `../futureETP_ReactNative/docs/tech-stack.md`(공용).

상태: 확정 (2026-11-27, AOS-003~AOS-011). 버전은 같은 날 Google Maven·Maven Central·Gradle Plugin Portal 메타데이터에서 확인한 최신 안정판

| 영역 | 선택 | 버전 | 메모 |
|---|---|---|---|
| 언어 / 빌드 | Kotlin · Gradle Kotlin DSL + version catalog(`gradle/libs.versions.toml`) · AGP | Kotlin 2.4.21 · Gradle 9.9.2 · AGP 9.5.3 | JDK 17(D-055). compileSdk·targetSdk 37, minSdk 29 (AOS-003) |
| UI | Jetpack Compose + Material 3 | Compose BOM 2026.11.01 · activity-compose 1.13.0 | |
| 상태 | ViewModel + `StateFlow`, 코루틴 | lifecycle 2.11.1 · coroutines 1.11.0 | RN TanStack Query 역할(캐시·staleTime·자동 새로고침)은 repository 계층에서 직접 |
| 내비게이션 | Navigation 3 | navigation3 1.2.2 | 백스택 = 키 리스트(AOS-004) |
| DI | 수동 DI(`AppContainer`) | — | ViewModel은 `viewModelFactory`(AOS-005) |
| HTTP / JSON | Ktor Client(OkHttp 엔진) + kotlinx.serialization | Ktor 3.6.2 · serialization 1.13.0 | `HttpTimeout` 15초·`HttpRequestRetry`, 디코딩 실패 → `SCHEMA_MISMATCH`(AOS-006) |
| 차트 | Compose `Canvas` 직접 구현(후보), Vico 검토 | — | 4단계 C-a 스파이크로 정한다 |
| 보안 저장소 | androidx `datastore-tink` + Tink(Android Keystore 마스터 키) | datastore 1.3.0 · Tink 1.24.0 | 접근 키(D-044)·installation ID(AOS-007). `EncryptedSharedPreferences`는 deprecated라 쓰지 않는다 |
| 기기 설정 저장 | DataStore(Preferences) | datastore 1.3.0 | 언어·등락 색(D-073) |
| i18n | `res/values`(ko 기본)·`values-en` + `AppCompatDelegate.setApplicationLocales` | — | RN `fallbackLng: 'ko'`와 같게(AOS-011) |
| QR | CameraX + ML Kit 바코드 스캐닝 | CameraX 1.7.0 · barcode-scanning 17.3.0 | B-b(D-078). 3단계에서 넣는다 |
| 푸시 | Firebase Cloud Messaging | Firebase BoM 35.2.0 · google-services 4.6.0 | RN과 같은 Firebase 앱. 5단계에서 넣는다 |
| 햅틱 | `HapticFeedback`(Compose) / `View.performHapticFeedback` | — | D-109·D-138·D-139 |
| 테스트 | JUnit 4 + Robolectric(Compose UI test를 JVM에서) + coroutines-test | Robolectric 4.18.1 | AOS-009 |
| lint / 포맷 / 정적 분석 | Android lint · ktlint(`org.jlleitschuh.gradle.ktlint`) · detekt 2.0 alpha | ktlint-gradle 14.2.0 · detekt 2.0.0-alpha.6 | detekt 1.23.8은 Kotlin 2.4 지원 근거가 없어 2.0 alpha(AOS-008) |
| 모듈 / import 방향 | 단일 `app` 모듈 + Konsist 아키텍처 테스트 | Konsist 0.18.1 | AOS-010 |

출처(2026-11-27 확인): [Navigation 3 is stable](https://developer.android.com/blog/posts/jetpack-navigation-3-is-stable), [DataStore 릴리스 노트](https://developer.android.com/jetpack/androidx/releases/datastore), [detekt 2.0.0 changelog](https://detekt.dev/changelog-2.0.0/), [detekt 2.0 migration](https://detekt.dev/docs/introduction/migration), 버전은 `dl.google.com/android/maven2`·`repo1.maven.org`·`plugins.gradle.org`의 `maven-metadata.xml`, Gradle은 `services.gradle.org/versions/current`

## RN 스택과의 대응

| RN(`../futureETP_ReactNative`) | 이 앱 |
|---|---|
| TanStack Query(서버 상태·캐시·자동 새로고침) | 상태 계층 + repository 캐시 |
| Zustand persist(언어·등락 색) | 기기 설정 저장 |
| Victory Native XL + Skia | 위 "차트" |
| expo-secure-store | 위 "보안 저장소" |
| expo-camera | 위 "QR" |
| i18next | 위 "i18n" |
| EAS Update(OTA) | 없음(재설치) |
