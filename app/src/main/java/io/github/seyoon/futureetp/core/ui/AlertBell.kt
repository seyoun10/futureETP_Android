package io.github.seyoon.futureetp.core.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.seyoon.futureetp.R
import io.github.seyoon.futureetp.core.theme.FontSize

/** 이 기기의 알림 대상 원자재 표시(A 목록 이름 아래, C title-bar 제목 옆). 크기는 두 곳이 같다(RN `AlertBell`) */
@Composable
fun AlertBell() {
    val label = stringResource(R.string.commodity_list_alert)
    Text("🔔", Modifier.semantics { contentDescription = label }, fontSize = FontSize.caption)
}
