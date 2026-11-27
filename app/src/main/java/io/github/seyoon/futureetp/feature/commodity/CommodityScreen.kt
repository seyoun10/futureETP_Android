package io.github.seyoon.futureetp.feature.commodity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.seyoon.futureetp.core.api.model.CommodityCode
import io.github.seyoon.futureetp.core.theme.AppColors
import io.github.seyoon.futureetp.core.ui.AlertBell
import io.github.seyoon.futureetp.core.ui.ScreenHeader
import io.github.seyoon.futureetp.core.ui.SettingsHeaderButton
import io.github.seyoon.futureetp.core.ui.nameRes

@Composable
fun CommodityRoute(
    viewModel: CommodityViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    CommodityScreen(viewModel.code, title, onBack, onOpenSettings)
}

/** C 원자재 화면 틀. title-bar `[←] 원자재명 (심볼) (🔔) [⚙]`(RN `CommodityTitleBar`). [⟲]·탭은 C-a와 함께 붙인다 */
@Composable
fun CommodityScreen(
    code: CommodityCode,
    title: CommodityTitleState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(AppColors.background)) {
        ScreenHeader(
            "${stringResource(code.nameRes)} (${code.name})",
            onBack = onBack,
            titleAccessory = if (title.isAlert) ({ AlertBell() }) else null,
        ) {
            SettingsHeaderButton(title.alertOff, onOpenSettings)
        }
        Box(Modifier.weight(1f))
    }
}
