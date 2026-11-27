# History

> `TODO.md`에서 끝난 블록을 같은 절 이름 아래로 옮겨 둔다. 최신이 아래.

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
