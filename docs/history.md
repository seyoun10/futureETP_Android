# History

> `TODO.md`에서 끝난 블록을 같은 절 이름 아래로 옮겨 둔다. 최신이 아래.

## 0. 환경
- [x] Android Studio · Zulu JDK 17 · Android SDK · 에뮬레이터는 RN과 함께 쓴다(D-055). 에뮬레이터는 이 저장소 전용 `AOS_Pixel_API_36`(상위 `CLAUDE.md` 기기 표)
- [x] Compose BOM 2026.08.00은 compileSdk 37·AGP 9.1.1 이상이 필요하다. 설치된 SDK 37과 AGP 버전을 확인
- [x] `google-services.json`은 RN(`../futureETP_ReactNative/apps/mobile/google-services.json`)과 같은 Firebase 앱(같은 앱 ID)이다. 커밋하지 않고 복사해 쓴다
  - 결과(2026-11-27): Zulu JDK 17.0.21, Android Studio 2026.2, SDK platforms 34~37.0, build-tools 36.0.0·37.0.0, AVD `AOS_Pixel_API_36`, RN `google-services.json` 있음. Gradle 캐시에 AGP 9.1.1~9.3.1·Kotlin 2.3.20~2.4.20
