# 결정 기록 (Decision Log)

> Kotlin/Jetpack Compose 앱의 결정과 근거를 시간순으로 추가한다. 번호는 `AOS-xxx`이고 공통 결정 D-xxx(`../futureETP_ReactNative/docs/decisions.md`)와 겹치지 않는다.
> 번복 시 기존 항목은 지우지 말고 상태를 `대체됨`으로 바꾼다. 공통으로 바뀌어야 할 결정(제품·API·화면 동작)은 여기가 아니라 futureETP에 D-xxx로 적는다.

<!--
## AOS-001: <제목>
- 날짜: YYYY-MM-DD
- 상태: 확정 | 검토중 | 대체됨(AOS-xxx)
- 맥락:
- 선택지:
- 결정:
- 근거:
- 영향:
-->

## AOS-001: 공통 문서는 futureETP 원본을 상대경로로 읽는다
- 날짜: 2026-11-18
- 상태: 확정 (사용자 요청, D-141)
- 맥락: 같은 서버로 RN·Flutter·Swift·Kotlin 앱 네 개를 만든다. 제품·화면·API 계약 문서를 저장소마다 둘지 정해야 했다.
- 선택지: (a) futureETP 원본 + 상대경로 참조 (b) 저장소마다 복사 (c) 상위 공통 폴더로 이동
- 결정: (a). 이 저장소는 `CLAUDE.md`·`TODO.md`·`docs/tech-stack.md`·`architecture.md`·`decisions.md`만 갖고, 나머지는 `../futureETP_ReactNative/...`를 읽는다. 두 저장소는 같은 상위 폴더(`_project/futureETP/`)에 나란히 있어야 한다.
- 근거: 복사하면 제품 결정이 바뀔 때 네 곳을 고쳐야 해 어긋난다. 원본 변경은 futureETP가 이 저장소 `TODO.md`의 "원본 변경 반영"에 적는다.
- 영향: `CLAUDE.md` "공통 문서", `TODO.md` "원본 변경 반영".

## AOS-002: 앱 ID를 RN과 같이 쓰고 `appVersion` 접두사로 구분한다
- 날짜: 2026-11-18
- 상태: 확정 (사용자 요청, D-141)
- 맥락: 앱마다 다른 ID면 한 기기에 함께 설치할 수 있지만 서버 푸시 설정(FCM 앱 등록·APNs topic)을 여러 앱으로 늘려야 한다.
- 결정: 앱 ID `io.github.seyoon.futureetp`, dev 변형 `io.github.seyoon.futureetp.dev`. `PUT /v1/devices`의 `appVersion`은 `aos-<버전>`.
- 근거: 서버·Firebase·APNs 설정을 바꾸지 않는다. `appVersion`은 64자 문자열이라 스키마 변경 없이 어느 클라이언트인지 서버 기록에서 알 수 있다.
- 영향: 한 기기에 RN 앱과 함께 설치할 수 없다. 바꿔 설치하면 새 installation ID로 새 기기 행이 생겨 접근 키·기기 설정을 다시 넣는다. 서명 키가 다르면 기존 앱을 지운 뒤 설치해야 한다(Android).

## AOS-003: 빌드 도구와 SDK 버전
- 날짜: 2026-11-27
- 상태: 확정
- 맥락: 1단계에서 빌드 도구 버전을 최신 안정판으로 정한다.
- 결정: Kotlin 2.4.21, Gradle 9.9.2, AGP 9.5.3, Compose BOM 2026.11.01, JDK 17. compileSdk·targetSdk 37, minSdk 29. 버전은 `gradle/libs.versions.toml` 한 곳에만 둔다.
- 근거: 2026-11-27 Maven 메타데이터 기준 최신 안정판. minSdk는 개인 앱(D-011)이라 넓게 받을 필요가 없어 Android 10으로 두고, 오래된 API 분기를 줄인다.
- 영향: `docs/tech-stack.md`. Android Studio 2026.2가 AGP 9.5를 열지 못하면 Studio를 올린다(명령줄 빌드는 상관없음).
- 보충(2026-11-27, 2단계): Compose 1.13(BOM 2026.11.01)이 compileSdk 37.1 이상을 요구해 compileSdk를 37.2(`release(37) { minorApiLevel = 2 }`)로 둔다. targetSdk는 37.

## AOS-004: 내비게이션은 Navigation 3
- 날짜: 2026-11-27
- 상태: 확정 (사용자 선택)
- 선택지: (a) Navigation 3 1.2.2 (b) Navigation Compose 2.10 타입 안전 route
- 결정: (a). 백스택을 `@Serializable` 키 리스트(`NavKey`)로 두고 `NavDisplay`로 그린다. C의 하단 탭 4개는 C 화면 안 상태로 두고, 탭 밖 C-d-a는 백스택 키로 쌓는다.
- 근거: 2025-11 안정화된 Compose 상태 기반 새 권장안이고 Nav2 사용자에게도 이전을 권한다. `UNAUTHORIZED` → B-b 이동(D-079)이나 키 없을 때 첫 화면 B-b를 리스트 조작으로 단순하게 표현한다.
- 영향: `navigation/`. 딥 링크(알림 탭 → C 화면, 5단계)는 백스택 리스트를 직접 만든다.

## AOS-005: DI는 수동 컨테이너
- 날짜: 2026-11-27
- 상태: 확정 (사용자 선택)
- 선택지: (a) 수동 `AppContainer` (b) Hilt 2.60.1 (c) Koin 4.2.2
- 결정: (a). `Application`이 `AppContainer`(API client·repository·저장소)를 갖고, ViewModel은 `viewModelFactory { initializer { ... } }`로 받는다.
- 근거: 화면 8개 안팎 개인 앱이라 KSP·플러그인·빌드 시간을 늘릴 이유가 없다. 테스트는 생성자에 가짜 구현을 넘긴다.
- 영향: 커지면 Hilt로 옮길 수 있게 생성자 주입만 쓴다.

## AOS-006: HTTP는 Ktor Client(OkHttp 엔진) + kotlinx.serialization
- 날짜: 2026-11-27
- 상태: 확정 (사용자 선택)
- 선택지: (a) Ktor 3.6.2 + OkHttp 엔진 (b) Retrofit 3.0 + OkHttp 5.5
- 결정: (a). 타임아웃은 `HttpTimeout` 15초, 재시도는 `HttpRequestRetry`로 `NETWORK`·`TIMEOUT`·`INTERNAL`만 최대 2회, 변경 요청(PATCH·PUT·POST·DELETE)은 재시도하지 않는다(D-072). 봉투 디코딩과 `AppError` 매핑은 client 한 곳에서 한다(D-042).
- 근거: D-072 규칙을 내장 플러그인 설정으로 쓰고, 봉투 처리를 인터셉터·CallAdapter 없이 직접 짠다. `MockEngine`으로 단위 테스트한다.
- 영향: `core/api`. JSON은 `Json { ignoreUnknownKeys = true }`로 서버 필드 추가에 견딘다(계약 테스트는 3단계).

## AOS-007: 보안 저장소는 androidx datastore-tink
- 날짜: 2026-11-27
- 상태: 확정 (사용자 선택)
- 선택지: (a) `androidx.datastore:datastore-tink` 1.3.0 + Tink Android 1.24.0 (b) Preferences DataStore에 Tink AEAD로 직접 암호화
- 결정: (a). 키셋은 `AndroidKeysetManager`로 만들고 Android Keystore 마스터 키로 보호한다. 접근 키와 installation ID만 이 저장소에 둔다(D-043, D-044, D-120).
- 근거: 공식 암호화 DataStore라 암·복호화 코드를 직접 쓰지 않는다. `EncryptedSharedPreferences`는 deprecated.
- 영향: 앱 데이터를 지우면 키셋도 지워져 접근 키를 다시 넣는다(재설치와 같음, AOS-002). 백업에서 제외한다.

## AOS-008: lint·포맷·정적 분석
- 날짜: 2026-11-27
- 상태: 확정 (사용자 선택)
- 선택지: detekt (a) 2.0.0-alpha.6 (b) 쓰지 않음. 1.23.8 안정판은 Kotlin 2.4를 지원한다는 근거가 없다
- 결정: Android lint(경고를 오류로, D-128 대응) + ktlint(`org.jlleitschuh.gradle.ktlint` 14.2.0) + detekt 2.0.0-alpha.6(`dev.detekt`). 모두 `./gradlew check`에 묶는다.
- 근거: detekt 2.0은 Kotlin 2.4·AGP 9·Gradle 9로 빌드·테스트된다. 검사 도구라 앱에 들어가지 않아 alpha 위험이 작다.
- 영향: detekt 2.0 안정판이 나오면 올린다. Kotlin deprecated 경고는 `allWarningsAsErrors`로 막는다(D-128).

## AOS-009: 테스트는 JUnit 4 + Robolectric
- 날짜: 2026-11-27
- 상태: 확정 (사용자 선택)
- 선택지: (a) JUnit 4 하나 + Robolectric (b) JUnit 6 + JUnit 4(vintage)
- 결정: (a). 단위 테스트와 Compose UI 테스트(`createComposeRule`)를 모두 JUnit 4로 `src/test`에서 Robolectric 4.18.1로 돌린다. 코루틴은 kotlinx-coroutines-test.
- 근거: Compose UI test가 JUnit 4 rule 기반이라 한 가지로 통일하면 설정이 하나다. 기기 없이 `check`에서 돈다.
- 영향: 기기에서만 확인되는 것(손동작·성능·QR)은 기기 확인으로 남긴다.
- 보충(2026-11-27, 2단계): Robolectric 4.18은 SDK 36부터 Java 21이 필요하다. JDK 17(D-055)을 유지하려고 Robolectric은 SDK 35로 돌린다(`robolectric.properties`). UI 테스트는 `@Config(qualifiers = "ko")`로 ko 원문을 본다.

## AOS-010: 단일 app 모듈과 Konsist로 import 방향 검사
- 날짜: 2026-11-27
- 상태: 확정 (사용자 선택, D-041 대응)
- 선택지: (a) 단일 `app` 모듈 + Konsist 0.18.1 테스트 (b) Gradle 멀티 모듈
- 결정: (a). 패키지 `feature.*`·`core.*`·`navigation`으로 나누고, Konsist 단위 테스트가 feature 간 import, `core` → `feature`·`navigation`, `feature` → `navigation`을 막는다.
- 근거: 개인 앱에 모듈별 build 설정·convention plugin은 무겁다. 테스트라 `check`에서 함께 돈다.
- 영향: `app/src/test/.../ArchitectureTest.kt`. 빌드 시간이 문제가 되면 멀티 모듈로 나눈다.

## AOS-011: 문구 리소스 기본 언어는 한국어
- 날짜: 2026-11-27
- 상태: 확정 (사용자 선택, D-033 대응)
- 선택지: (a) `values`=ko, `values-en` (b) `values`=en, `values-ko`
- 결정: (a). 앱 안 언어는 `AppCompatDelegate.setApplicationLocales` + `locales_config.xml`(ko·en).
- 근거: RN i18n `fallbackLng: 'ko'`와 같게 기기 언어가 ko·en이 아니면 한국어로 보인다.
- 영향: `res/values/strings.xml`이 ko 원문이다.

## AOS-012: 응답 모델은 손으로 쓰고 계약 복사본·실제 응답 예시로 검사
- 날짜: 2026-11-27
- 상태: 제안 (Claude, 사용자 확인 전)
- 선택지: (a) `@Serializable` 모델을 손으로 쓰고 계약 테스트로 맞춘다 (b) quicktype 26.0.0(`--framework kotlinx`) (c) OpenAPI Generator 7.28.0(`kotlin`, `kotlinx_serialization`, 스키마를 OpenAPI 3.1 components로 감싸서) (d) json-kotlin-schema-codegen 0.123
- 검토(2026-11-27, 계약 복사본으로 실제 생성):
  - (b) 스키마 여러 개를 한 번에 넣으면 내부 오류(`Defined value expected`)라 하나씩 생성해야 하고, 그러면 `Basis`·`Window` 같은 enum이 파일마다 따로 생긴다. 필수 nullable 필드에 `= null` 기본값이 붙어 키가 빠져도 null로 통과한다(SCHEMA_MISMATCH를 못 잡음). `maPeriod`는 `Double`, 정수는 `Long`, `contractMonth`·`at`은 `String`. enum 이름을 속성 이름에서 지어 `alertCommodity` → `AlertCommodity`, `direction` → `Side`가 된다
  - (c) 필수 nullable은 맞게 나오지만 enum이 클래스마다 중첩돼(`DisparityChart.Basis`·`SettingsPatch.Basis`가 다른 타입) 공용 `CommodityCode` 등이 없고, 숫자 enum `maPeriod`는 `BigDecimal` 값에 문자열 인자(`_20("20")`)를 넣는 코드와 문자열 `@SerialName`이 나와 서버 숫자 `20`과 맞지 않는다. `rareLevels`는 `@Contextual BigDecimal`, 안쪽 타입 이름은 `DisparityChartBarsInnerHoldingsInner`. OpenAPI 3.1 지원은 beta 경고
  - (b)(c) 공통: `SettingsPatch`에서 "보내지 않음"과 "null로 지우기"(`alertCommodity: null`, 알림 대상 없음 D-124)를 구분하지 못한다. 생성 코드는 ktlint·detekt 규칙에 맞지 않아 검사에서 빼야 한다. 원본 스키마는 생성 파일이라 `$ref`로 공용 enum을 뽑도록 고칠 수 없다(D-142, 읽기만)
  - (d) Draft 07 기준이고 마지막 릴리스가 2025-07이라 draft 2020-12 계약에는 시험하지 않았다
- 결정: (a). `core/api/model`에 계약 필드·nullable·enum 그대로 쓴다. 날짜·시각·월물은 java.time(`LocalDate`·`Instant`·`YearMonth`·`LocalTime`)으로 디코딩하고, 형식이 다르거나 모르는 enum 값이면 `SCHEMA_MISMATCH`(D-042). 계약 테스트(`ContractTest`)는 networknt json-schema-validator 3.0.9(draft 2020-12)로
  1) 커밋한 계약 복사본(`app/src/test/resources/contract/schemas`, `scripts/sync-contract.sh`)이 원본과 같은지(원본 저장소가 옆에 있을 때),
  2) dev 서버 실제 응답 data(`contract/fixtures`, `scripts/capture-fixtures.sh`, 차트는 마지막 60거래일)가 스키마를 통과하고 모델로 디코딩되며 다시 인코딩하면 같은 JSON인지,
  3) 요청 본문(`SettingsPatch`·`DeviceRegistration`)과 query가 요청 스키마를 통과하는지 본다.
- 근거: 스키마가 작고(응답 10개) zod refine·주석은 생성기로 옮겨지지 않는다. 위 검토처럼 생성 결과를 그대로 쓸 수 없고 손보면 생성의 이점이 사라진다. 계약 변경은 손 작성이어도 `ContractTest`(원본 일치·왕복 비교)가 바로 알려 준다. iOS(IOS-007)·Flutter(FL-007)도 손 작성 + 예시 검사다. 왕복 비교가 빠진 필드를, 스키마 검사가 예시 자체의 계약 위반을 잡는다.
- 영향: 원본 계약이 바뀌면 `ContractTest.schemaCopyMatchesUpstream`이 실패하므로 `scripts/sync-contract.sh` → 모델 수정 → `scripts/capture-fixtures.sh`. 요청 본문은 `requestJson`(null을 빼지 않음)으로 인코딩한다. rare 레벨 query는 `formatLevel`로 숫자 기본 문자열(`1`, `2.5`)을 만든다.
