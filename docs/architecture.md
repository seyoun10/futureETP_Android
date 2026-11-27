# Architecture

> Kotlin/Jetpack Compose 앱의 구조. API 규약·화면 동작은 원본(`../futureETP_ReactNative/docs/architecture.md`·`wireframes/screens.md`)을 따르고, 여기에는 이 앱의 구조와 대응만 적는다.
> 2단계 스캐폴딩(2026-11-27)으로 만든 구조. 아직 없는 폴더는 해당 단계에서 만든다.

## 디렉터리 구조

```
app/src/main/java/io/github/seyoon/futureetp/
├─ MainActivity.kt / FutureEtpApplication.kt / AppContainer.kt   진입점, 앱 컨테이너(수동 DI, AOS-005)
├─ navigation/                   Navigation 3 NavDisplay(`AppNavigation`), 백스택 키 `AppKey`(AOS-004)
├─ feature/
│  ├─ devhealth/                 2단계 임시 health 화면(B 화면과 함께 지움)
│  ├─ commoditylist/             A
│  ├─ settings/ alertsettings/ accesskey/ datasources/   B, B-a, B-b
│  └─ commodity/                 C: disparitychart(C-a)·contractchart(C-c)·curve(C-b)·etp(C-d, C-d-a)
├─ core/
│  ├─ api/                       HTTP 클라이언트(`ApiClient`), AppError, 엔드포인트(`Endpoints.kt`)
│  │  └─ model/                  계약 `schemas/*.json`과 맞춘 @Serializable 응답·요청·query 모델(AOS-012)
│  ├─ data/                      repository(조회 캐시·자동 새로고침)
│  ├─ chart/                     Canvas 차트·뷰포트
│  ├─ ui/                        공용 Composable(ScreenHeader, FreshnessLine, QueryStateView, SegmentedControl)
│  ├─ storage/                   보안 저장소(datastore-tink): 접근 키·installation ID, 요청 헤더 값 `StoredCredentials`(AOS-007)
│  ├─ settings/                  기기 UI 설정(Preferences DataStore): 등락 색 `DisplaySettingsStore`(AOS-014)
│  ├─ theme/                     테마 토큰(다크 전용, `Tokens.kt`), 등락 색 `UpDownPalette.kt`, `FutureEtpTheme`
│  ├─ push/                      FCM 토큰 등록, 알림 탭 이동
│  └─ util/                      숫자·날짜 포맷, 햅틱
app/src/main/res/                values(ko 원문)·values-en, 적응형 아이콘, 백업 제외 규칙
app/src/dev/AndroidManifest.xml  dev만 평문 HTTP 허용
app/src/test/                    JUnit 4 + Robolectric(SDK 35) + Konsist `ArchitectureTest` + 계약 테스트 `ContractTest`
app/src/test/resources/contract/ 계약 복사본 `schemas/`, dev 서버 응답 예시 `fixtures/`(AOS-012)
scripts/                         `sync-contract.sh`(계약 복사), `capture-fixtures.sh`(응답 예시 저장)
gradle/libs.versions.toml        버전(AOS-003) · config/detekt/detekt.yml · .editorconfig(ktlint)
docs/                            tech-stack, architecture, decisions
```

규칙
- 화면 → ViewModel + `StateFlow` → repository → API client 한 방향. 화면은 HTTP·저장소를 직접 부르지 않는다.
- 화면 전용 코드는 해당 feature 안에, 두 곳 이상에서 쓰면 공용 계층(`core`)으로 옮긴다.
- import 방향(RN D-041, 2026-11-19 lint로 강제): feature끼리 import하지 않는다(컴포넌트·헬퍼·조회도 다른 feature가 쓰면 `core`로 옮긴다). `core`는 features·`navigation`을 import하지 않고, features는 `navigation`(라우트)을 import하지 않는다. `ArchitectureTest`(Konsist)가 `./gradlew check`에서 강제한다(AOS-010).
- RN에서 공용으로 옮긴 것: `SectionHeader`·`SettingsHeaderButton`(공용 UI), 에러 code → 사용자 문구 함수 `errorText`(API의 `AppError` 옆), ETP 정렬 기준 `ETP_SORTS`(util).
- 월물 심볼은 서버 표준형(`CLZ26`)만 쓴다. 표시 표기(`DEC26`)는 포맷 함수가 만든다.

## 레이어

| 계층 | 맡는 것 | 하지 않는 것 |
|---|---|---|
| 화면(Composable 화면) | 그리기, 사용자 입력, 상태 구독 | HTTP, 저장소, 계산 |
| 상태(ViewModel + `StateFlow`) | 화면 상태(로딩·데이터·오류), 사용자 동작 처리, 자동 새로고침 타이머 | HTTP 세부 |
| repository | 조회 캐시(staleTime·gcTime), 무한 조회(`before`·`hasMore`), 낙관적 PATCH | 화면 문구 |
| API client | 헤더, 타임아웃, 봉투 디코딩, `AppError`, 재시도 | 캐시 |

- 캐시 규칙은 RN과 같다: C 차트 조회는 `staleTime`을 `nextUpdateAt`까지, `gcTime` 1시간. C-a·C-c는 3년(756거래일) 단위로 받고 가장 과거 봉이 보이면 이전 3년을 더 받는다(D-104). 커브는 캐시하지 않는다(D-046).

## RN 구조 대응

| RN(`../futureETP_ReactNative/apps/mobile/src`) | 이 앱 |
|---|---|
| `src/app/`(Expo Router) | `navigation/`(Navigation 3) |
| `src/features/*` | `feature/*`(화면 + ViewModel) |
| `src/api`(fetch 래퍼·AppError·TanStack Query) | `core/api` + `core/data`(repository) |
| `src/charts`(Victory + Skia) | `core/chart`(Canvas) |
| `src/stores`(Zustand persist) | `core/settings`(DataStore) |
| `src/theme` | `core/theme` |
| `src/i18n` | `res/values*/strings.xml` |
| `src/push` | `core/push` |
| `src/lib` | `core/util` |

## D-번호 적용

공통 결정은 `../futureETP_ReactNative/docs/decisions.md`. 이 앱에 어떻게 적용하는지만 나눈다.

- 그대로 적용: 제품·화면·API·UX 결정. 예) D-001~D-026(제품), D-036·D-042·D-044·D-048·D-072(API·에러·접근 키·자동 새로고침), D-075~D-086, D-090~D-093, D-102~D-125, D-137~D-139(화면 동작)
- 플랫폼 수단으로 바꿔 적용: D-030(상태·패칭), D-031(스타일·테마), D-033(i18n), D-041(레이어), D-043·D-052(변형), D-073(표시 설정 저장), D-087(차트 부품), D-128(deprecated API 금지 → 이 언어의 deprecated 경고를 오류로)
- 적용 안 함(RN·Expo 전용): D-027·D-029·D-032·D-050(Expo·Router·Victory·스캐폴딩), D-039의 EAS Update, D-054·D-140(jest-expo·Jest 캐시)
- 판단이 애매하면 해당 D 항목을 읽고 `decisions.md`에 AOS-xxx로 적용 방식을 적는다.
