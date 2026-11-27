package io.github.seyoon.futureetp.feature.alertsettings

import io.github.seyoon.futureetp.core.api.model.QuietTime
import java.util.Locale

// 알림 거부 시간 규칙(shared `QUIET_STEP_MIN`·`QUIET_MAX_MIN`, D-007, D-026). 현지 자정 기준 분, 30분 단위
const val QUIET_STEP_MIN = 30
private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
const val QUIET_MAX_MIN = MINUTES_PER_DAY - QUIET_STEP_MIN

/** 켤 때 처음 값(screens.md B-a 예시 23:00~07:00) */
val DEFAULT_QUIET_TIME = QuietTime(startMin = 23 * MINUTES_PER_HOUR, endMin = 7 * MINUTES_PER_HOUR)

/** 시각 선택 시트의 48칸(00:00~23:30) */
val QUIET_SLOTS: List<Int> = (0..QUIET_MAX_MIN step QUIET_STEP_MIN).toList()

/** 자정 기준 분 → 24시간제 `HH:MM` */
fun formatQuietMinute(minute: Int): String =
    String.format(Locale.ROOT, "%02d:%02d", minute / MINUTES_PER_HOUR, minute % MINUTES_PER_HOUR)

/** 자정을 넘기면(시작 > 종료) 종료 시각에 `다음 날`을 붙인다 */
val QuietTime.endsNextDay: Boolean get() = startMin > endMin

enum class QuietEdge { START, END }

fun QuietTime.minuteOf(edge: QuietEdge): Int = if (edge == QuietEdge.START) startMin else endMin

/** 시작≠종료라 반대쪽 시각은 고를 수 없다 */
fun QuietTime.blockedFor(edge: QuietEdge): Int =
    minuteOf(
        if (edge ==
            QuietEdge.START
        ) {
            QuietEdge.END
        } else {
            QuietEdge.START
        },
    )

fun QuietTime.with(
    edge: QuietEdge,
    minute: Int,
): QuietTime = if (edge == QuietEdge.START) copy(startMin = minute) else copy(endMin = minute)
