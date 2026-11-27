package io.github.seyoon.futureetp.feature.datasources

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.seyoon.futureetp.R
import io.github.seyoon.futureetp.core.theme.AppColors
import io.github.seyoon.futureetp.core.theme.FontSize
import io.github.seyoon.futureetp.core.theme.Spacing
import io.github.seyoon.futureetp.core.ui.ScreenHeader
import io.github.seyoon.futureetp.core.ui.SectionHeader

private data class SourceRow(
    @param:StringRes val label: Int,
    @param:StringRes val value: Int,
)

private data class SourceSection(
    @param:StringRes val title: Int,
    val rows: List<SourceRow>,
)

// 줄 순서는 screens.md 데이터 출처 그대로다. 소스가 바뀌면 문구(data_sources_*)도 바꾼다
private val SECTIONS =
    listOf(
        SourceSection(
            R.string.data_sources_section_futures,
            listOf(
                SourceRow(R.string.data_sources_exchange_label, R.string.data_sources_exchange_value),
                SourceRow(R.string.data_sources_settlement_label, R.string.data_sources_settlement_value),
                SourceRow(R.string.data_sources_intraday_label, R.string.data_sources_intraday_value),
                SourceRow(R.string.data_sources_history_label, R.string.data_sources_history_value),
            ),
        ),
        SourceSection(
            R.string.data_sources_section_etp,
            listOf(
                SourceRow(R.string.data_sources_etp_kr_label, R.string.data_sources_etp_kr_value),
                SourceRow(R.string.data_sources_etp_us_label, R.string.data_sources_etp_us_value),
            ),
        ),
    )

/** B > 데이터 출처. 업체 약관의 출처 표기를 지키는 읽기 전용 화면이다(product.md, D-011). 상표·로고 없이 이름만 쓴다 */
@Composable
fun DataSourcesScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(AppColors.background)) {
        ScreenHeader(stringResource(R.string.settings_data_sources), onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).windowInsetsPadding(WindowInsets.navigationBars)) {
            SECTIONS.forEach { section ->
                SectionHeader(stringResource(section.title))
                section.rows.forEach { row ->
                    Column {
                        Row(
                            Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            Text(stringResource(row.label), color = AppColors.textSecondary, fontSize = FontSize.body)
                            Text(
                                stringResource(row.value),
                                modifier = Modifier.weight(1f),
                                color = AppColors.text,
                                fontSize = FontSize.body,
                                textAlign = TextAlign.End,
                            )
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.separator))
                    }
                }
            }
            Text(
                stringResource(R.string.data_sources_notice),
                modifier = Modifier.padding(Spacing.lg),
                color = AppColors.textSecondary,
                fontSize = FontSize.caption,
            )
        }
    }
}
