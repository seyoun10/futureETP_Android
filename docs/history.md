# History

> `TODO.md`에서 끝난 블록을 같은 절 이름 아래로 옮겨 둔다. 최신이 아래.

## 원본 변경 반영
- [x] API 계약 JSON Schema 생성(2026-11-19, futureETP D-142): 응답 모델·계약 테스트가 `packages/shared/contract`를 기준으로 한다(2026-11-27, AOS-012)

## 0. 환경
- [x] Android Studio · Zulu JDK 17 · Android SDK · 에뮬레이터는 RN과 함께 쓴다(D-055). 에뮬레이터는 이 저장소 전용 `AOS_Pixel_API_36`(상위 `CLAUDE.md` 기기 표)
- [x] Compose BOM 2026.08.00은 compileSdk 37·AGP 9.1.1 이상이 필요하다. 설치된 SDK 37과 AGP 버전을 확인
- [x] `google-services.json`은 RN(`../futureETP_ReactNative/apps/mobile/google-services.json`)과 같은 Firebase 앱(같은 앱 ID)이다. 커밋하지 않고 복사해 쓴다
  - 결과(2026-11-27): Zulu JDK 17.0.21, Android Studio 2026.2, SDK platforms 34~37.0, build-tools 36.0.0·37.0.0, AVD `AOS_Pixel_API_36`, RN `google-services.json` 있음. Gradle 캐시에 AGP 9.1.1~9.3.1·Kotlin 2.3.20~2.4.20

## 1. 스택 확정
- [x] `docs/tech-stack.md`의 후보를 하나씩 정한다(2026-11-27, AOS-003~AOS-011). 버전은 Maven 메타데이터로 확인

## 2. 스캐폴딩
- [x] 프로젝트 생성, 앱 ID `io.github.seyoon.futureetp`·dev `.dev` 두 변형, 앱 이름·아이콘(RN `apps/mobile/assets` 재사용)
- [x] lint·format·test 설정과 검사 명령 하나(`check`), CLAUDE.md Commands를 실제 명령으로 갱신
  - import 방향 규칙(architecture.md "규칙")을 검사에 넣는다. Konsist 아키텍처 테스트(AOS-010)
- [x] dev 변형이 로컬 dev 서버 `GET /v1/health`를 부르는 최소 화면으로 연결 확인
  - 결과(2026-11-27): flavor `dev`(`futureETP Dev`, `.dev`)·`prod`(`futureETP`, `https://futureetp-api.duckdns.org`). 아이콘은 Flutter 저장소가 RN 에셋으로 만든 적응형 아이콘을 복사(minSdk 29라 PNG mipmap 없음). Compose 1.13이 compileSdk 37.1 이상을 요구해 SDK Platform 37.2를 설치했고, Robolectric은 SDK 36부터 Java 21이 필요해 SDK 35로 돈다(AOS-003, AOS-009). `AOS_Pixel_API_36`에서 dev 서버 health `OK`를 확인

## 3. 공통 기반
- [x] API client: 헤더(Bearer·`X-Device-Id`), 타임아웃 15초, 봉투 디코딩, `AppError`, 재시도 규칙 (D-042, D-072)
  - 결과(2026-11-27): `core/api/ApiClient.request`가 유일한 경로. 키가 없으면 보내지 않고 `UNAUTHORIZED`, 봉투가 아닌 응답은 5xx면 `INTERNAL`·아니면 `SCHEMA_MISMATCH`, 계약에 없는 서버 코드도 `SCHEMA_MISMATCH`. GET만 `NETWORK`·`TIMEOUT`·`INTERNAL`을 1초·2초 뒤 최대 2회 다시 보낸다(RN TanStack Query 기본 backoff). 에러 문구는 `AppErrorCode.messageRes`·`errorTextRes`(RN `errors.*`). 접근 키·기기 ID는 `ApiCredentials`로 받는다
- [x] 응답 모델: `../futureETP_ReactNative/packages/shared/contract/schemas/*.json`과 맞춘 모델 + 계약 테스트 (D-142)
  - 결과(2026-11-27, AOS-012): `core/api/model`에 손으로 쓴 `@Serializable` 모델(날짜·시각·월물은 java.time), `core/api/Endpoints.kt`에 엔드포인트 함수. `ContractTest`가 계약 복사본(futureETP `ed8194a`)과 원본 일치, dev 서버 응답 예시 16개의 스키마 통과·디코딩·왕복 일치, 요청 본문·query의 요청 스키마 통과를 본다. 필드를 하나 빼면 왕복 비교가 실패하는 것을 확인
- [x] installation ID(첫 실행 UUID v4) 생성·보안 저장소 보관 (D-120). `core/storage`의 `SecureStore`(datastore-tink)·`StoredCredentials`가 `AppContainer`의 임시 값을 바꿨다(2026-11-27, AOS-007)
  - 결과: 단위 테스트는 메모리 키셋으로 암호화 저장·복호화 실패 복구·ID 유지·형식 아닌 값 교체·저장소 실패 시 메모리 ID를 본다. `AOS_Pixel_API_36`에서 앱 데이터를 지운 뒤 두 번 실행해 같은 ID가 나오고 `secure.json`이 암호문인 것을 확인(Keystore 경로는 기기에서만 돈다)
- [x] 숫자·날짜 포맷(RN `formatNumber`·`formatDateTime`과 같은 결과) 단위 테스트
  - 결과(2026-11-27): `core/util`의 `formatPrice`·`formatChange`·`formatChangePct`·`formatContractMonth(Short)`·`formatLocalDateTime`·`formatTradeDate`·`compactAmount`(RN `formatAmount`). 반올림은 JS `toFixed`처럼 double의 정확한 값에서 절반 올림(`BigDecimal(double)` + `HALF_UP`, 1.005 → 1.00). 월물·날짜는 java.time 값을 받는다. `FormatTest`가 RN 테스트 기대값과 toFixed 경계값(node로 확인)을 본다
- [x] 세로 잠금은 코드로(`requestedOrientation`, D-076)
  - 결과(2026-11-27): `core/ui/Orientation.kt`의 `lockPortrait`(`SCREEN_ORIENTATION_PORTRAIT`, expo `PORTRAIT_UP`)·`lockLandscape`(`SENSOR_LANDSCAPE`, expo `LANDSCAPE`)·`isLandscape()`(창 크기 너비 > 높이). `MainActivity.onCreate`가 세로로 잠근다. 코드 잠금도 lint `SourceLockedOrientationActivity`가 막아 `lockPortrait`에만 `@SuppressLint`를 단다. `AOS_Pixel_API_36`에서 기기를 가로로 돌려도 앱이 세로로 남는 것을 확인. [⟲]·ETP 탭 세로 복귀는 3단계 마지막 항목
- [x] 접근 키 B-b 붙여넣기·키 확인·첫 화면 분기·거절 이동 (D-044, D-077, D-079)
  - 결과(2026-11-27): `feature/accesskey`(`parseAccessKey`·`AccessKeyViewModel`·`AccessKeyScreen`). 키는 `OutlinedSecureTextField`(`TextObfuscationMode.Hidden`, 입력값은 저장 상태에 남기지 않음)로 받고, 그 키로 `GET /v1/settings`가 성공해야 `saveAccessKey`. `StoredCredentials`가 키를 처음 한 번 읽어 메모리에 두고 `keyState`(`loaded`·`hasKey`·`rejected`)를 낸다. `AppNavigation`은 읽기 전 빈 배경, 키 없으면 백스택 대신 B-b([←] 없음), 있으면 백스택. 저장된 키로 보낸 요청의 서버 `UNAUTHORIZED`는 `ApiClient`가 `ApiCredentials.reportRejected()`로 알리고(새 키 확인·키 없음은 제외), 백스택이 B-b를 얹는다. B-b를 떠나면 거절 표시를 지운다. `AOS_Pixel_API_36` + dev 서버에서 첫 실행 B-b → 틀린 키 "키가 맞지 않습니다" → 맞는 키 저장 뒤 홈, 재실행 때 B-b 건너뜀을 확인. 거절 이동은 인증 화면이 아직 없어 단위 테스트로만 확인. QR은 남은 항목
- [x] 접근 키 B-b QR 읽기 (D-044, D-078, AOS-013)
  - 결과(2026-11-27): CameraX 1.7.0 `Preview`·`ImageAnalysis` + `CameraXViewfinder` + `MlKitAnalyzer`(barcode-scanning 17.3.0, QR만)를 B-b 안 정사각형으로(`QrCameraScanner`). [QR 읽기] → 권한 있으면 바로, 없으면 `RequestPermission(CAMERA)`, 거부·뒤 카메라 없음·연결 실패는 "카메라를 쓸 수 없습니다" 안내. `AccessKeyViewModel.scanned`가 접두사 있는 키만 받아 카메라를 닫고 붙여넣기와 같은 확인·저장, 아니면 "접근 키 QR이 아닙니다" 후 계속 읽기. 매니페스트 `CAMERA` + `android.hardware.camera` `required="false"`, 마이크 권한 없음. 화면 테스트는 `scanner` 슬롯에 가짜 스캐너. `AOS_Pixel_API_36`에서 권한 거부 안내, 허용 뒤 미리보기(가상 장면), [취소] 뒤 카메라 해제를 확인. 실제 키 QR 읽기는 에뮬레이터 장면에 QR을 띄울 수 없어 기기 미확인
- [x] 테마 토큰(다크 전용)·등락 색 설정 저장 (D-031, D-073)
  - 결과(2026-11-27, AOS-014): `core/theme/Tokens.kt`(RN `tokens.ts` 값, 터치 영역만 48dp)·`UpDownPalette.kt`(`upDownPalette`·`changeColor`·`changePairColor`·`LocalUpDownPalette`)·`FutureEtpTheme(upDownStyle)`. `core/settings/DisplaySettingsStore`(Preferences DataStore)가 첫 읽기 때 시스템 지역으로 고른 기본값을 저장하고, `MainActivity`는 첫 값 전 빈 배경. `UpDownPaletteTest`가 RN 테스트 기대값을, `DisplaySettingsStoreTest`가 지역 기본값·첫 기본값 유지·저장·모르는 값 복구를 본다. 설정을 바꾸는 화면은 B 화면 항목
- [x] i18n ko/en, 앱 안 언어 설정을 서버 기기 설정과 맞춤 (D-033, D-073, D-137) + 앱 실행 때 시간대 PATCH (D-084)
  - 결과(2026-11-27, AOS-015): `core/settings/AppLocale`(AppCompat 1.8.0 per-app language)이 첫 실행 때 기기 언어로 ko/en을 저장하고, `feature/settings/DeviceSettingsSync`가 키가 있는 동안 STARTED마다 서버 설정을 받아 시간대·언어가 다르면 한 번의 PATCH로 보낸다(같은 값은 프로세스당 한 번). `AppLocaleTest`·`DeviceSettingsSyncTest`(RN 테스트 경우). `AOS_Pixel_API_36`(기기 en-US)에서 데이터를 지운 첫 실행 뒤 앱 언어 `[en]`, `cmd locale set-app-locales --locales ko`로 B-b가 한국어로 바뀌는 것을 확인. PATCH는 저장된 키가 없어 기기에서 보지 못했고 단위 테스트로만 확인. 언어를 고르는 화면은 B 화면 항목
- [x] 공용 조회 UI: 기준 시각 줄·로딩/오류 분기·`nextUpdateAt` + 1분 자동 새로고침·앱 복귀 재조회·새 접근 키 저장 뒤 다시 조회 (D-048, D-075, D-077)
  - 결과(2026-11-27, AOS-016): `core/query/Query`(RN TanStack Query + `useAutoRefresh` 대응)와 `core/ui`의 `QueryStateView`·`LoadingView`(막대 스켈레톤 + 가운데 스피너)·`ErrorView`·`AutoRefreshEffect`·`FreshnessFooter`·`AsOfFooter`·`FooterLine`. 앱 복귀는 `appReturns`(ProcessLifecycleOwner), 새 키 저장은 `StoredCredentials.keySaves`. 빈 상태는 RN처럼 화면마다 둔다. `QueryTest`가 자동 새로고침 시각·실패 뒤 1분 재시도·staleTime 복귀·새 키 취소 재조회를, `QueryStateViewTest`가 분기와 기준 시각 줄 문구를 본다. 아직 쓰는 화면이 없어 기기 확인은 A 화면에서. C 탭 아래 줄(RN `TabFreshnessFooter`)은 C 화면 항목
