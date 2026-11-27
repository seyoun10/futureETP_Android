package io.github.seyoon.futureetp.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 테마 토큰(D-031, D-073). 다크 전용이라 테마 전환은 없다. 값은 RN `src/theme/tokens.ts`와 같다.
// 차트(Canvas)도 같은 값을 읽는다. 등락 색은 토큰이 아니라 표시 설정에서 고른다(UpDownPalette.kt)

object AppColors {
    val background = Color(0xFF000000)
    val surface = Color(0xFF1E1E1E)
    val surfaceSelected = Color(0xFF3A3A6A)
    val border = Color(0xFF2A2A2A)

    /** 목록 항목 사이 구분선. border보다 밝게 */
    val separator = Color(0xFF4A4A4A)

    /** title-bar 아래·tab-bar 위 구분선 */
    val barSeparator = Color(0xFFFFFFFF)

    /** 차트 위에 겹친 정보 줄 배경(뒤의 rare-level 선·격자가 비쳐 보이게) */
    val overlay = Color.Black.copy(alpha = 0.65f)
    val text = Color(0xFFE0E0E0)
    val textSecondary = Color(0xFF9E9E9E)

    /** 등락 0, 데이터 없음 등 방향이 없는 값 */
    val neutral = Color(0xFF9E9E9E)

    /** 모달 뒤 화면을 어둡게 가리는 배경 */
    val backdrop = Color.Black.copy(alpha = 0.5f)

    /** 오류 안내 문구(B-b 키 거절, B·B-a 저장 실패, rare-level 입력 오류) */
    val error = Color(0xFFEF5350)

    /** [⚙] 알림 꺼짐 점 */
    val badge = Color(0xFFEF5350)

    /** 휴장 달력: 오늘 테두리 */
    val today = Color(0xFFFFFFFF)
}

/** 휴장 달력 시장별 휴장 점(등락 색·차트 선과 따로 본다) */
object MarketColors {
    val cme = Color(0xFFFFA726)
    val krx = Color(0xFF4FC3F7)
    val nyse = Color(0xFF81C784)
}

object ChartColors {
    val grid = Color(0xFF2A2A2A)
    val tick = Color(0xFF9E9E9E)
    val crosshair = Color(0xFFFFFFFF)

    /** 십자선 점선은 라벨 상자(흰색)보다 옅게 해 봉·선을 덜 가린다 */
    val crosshairLine = Color.White.copy(alpha = 0.6f)
    val crosshairText = Color(0xFF000000)

    /** 마지막 봉 종가 라벨·점선(흰색)과 보이는 마지막 봉 종가 라벨·점선(회색)(screens.md C-a) */
    val lastPrice = Color(0xFFFFFFFF)
    val currentPrice = Color(0xFF9E9E9E)

    /** 노랑은 이평선 전용(screens.md C-a) */
    val movingAverage = Color(0xFFFFD54F)

    /** rare-level 선은 레벨 값이 아니라 순위로 고른다. 0 = 가장 덜 드문 레벨(옅음) → 4 = 가장 드문 레벨(진함) */
    val rareLevels =
        listOf(Color(0xFF4A3D66), Color(0xFF62508A), Color(0xFF7C62B0), Color(0xFF9A72D8), Color(0xFFB884FF))

    /** 롤 표시는 청록(이평선 노랑·rare-level 보라·등락 색과 겹치지 않음). 롤 기간 음영, 대표월물 전환일 세로 점선 */
    val rollShade = Color(red = 77, green = 208, blue = 225).copy(alpha = 0.12f)
    val rollSwitch = Color(red = 77, green = 208, blue = 225).copy(alpha = 0.45f)

    /** 보이는 구간 최고가·최저가 꼬리표 */
    val extremeTag = Color(0xFFE0E0E0)

    /** C-b 커브: 기준일은 흰색 실선, 비교일은 회색 점선(screens.md C-b) */
    val curveBase = Color(0xFFFFFFFF)
    val curveCurrent = Color(0xFF9E9E9E)

    /** C-c 월물 선은 만기순 순번으로 고른다(빨·주·노·초·파 무지개 순서). 6번째는 대표월물 안전장치 월물(D-092) */
    val contractLines =
        listOf(
            Color(0xFFEF5350),
            Color(0xFFFFA726),
            Color(0xFFFFEE58),
            Color(0xFF66BB6A),
            Color(0xFF42A5F5),
            Color(0xFF90A4AE),
        )
}

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

object FontSize {
    val caption = 12.sp
    val body = 14.sp
    val title = 17.sp
    val headline = 22.sp
}

object Radius {
    val sm = 4.dp
    val md = 6.dp
}

object Size {
    /** 최소 터치 영역. RN은 Apple HIG 44pt, Android는 Material 접근성 기준 48dp(AOS-014) */
    val touchTarget = 48.dp
}
