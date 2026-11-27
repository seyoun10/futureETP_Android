package io.github.seyoon.futureetp.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * 주요 사용자 동작(원자재·ETP 선택, 탭·기간·설정 값 변경, 설정 진입, 회전, title-bar 아이콘 버튼)의 짧은 선택 햅틱
 * (RN `tapHaptic`, D-109). 아무것도 바뀌지 않으면 부르지 않는다
 */
@Composable
fun rememberTapHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return { haptic.performHapticFeedback(HapticFeedbackType.SegmentTick) }
}
