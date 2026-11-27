package io.github.seyoon.futureetp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.seyoon.futureetp.AppContainer
import io.github.seyoon.futureetp.BuildConfig
import io.github.seyoon.futureetp.R
import io.github.seyoon.futureetp.core.api.settings
import io.github.seyoon.futureetp.feature.accesskey.AccessKeyRoute
import io.github.seyoon.futureetp.feature.accesskey.AccessKeyViewModel
import io.github.seyoon.futureetp.feature.alertsettings.AlertSettingsRoute
import io.github.seyoon.futureetp.feature.alertsettings.AlertSettingsViewModel
import io.github.seyoon.futureetp.feature.datasources.DataSourcesScreen
import io.github.seyoon.futureetp.feature.settings.DisplayControls
import io.github.seyoon.futureetp.feature.settings.SettingsNav
import io.github.seyoon.futureetp.feature.settings.SettingsRoute
import io.github.seyoon.futureetp.feature.settings.SettingsViewModel
import kotlinx.coroutines.flow.map

/**
 * 저장된 키를 읽기 전에는 빈 배경, 키가 없으면 백스택 대신 B-b만([←] 없음), 키가 있으면 백스택을 그린다(D-077).
 * 저장하면 키 유무가 바뀌어 백스택(A)으로 바뀐다
 */
@Composable
fun AppNavigation(container: AppContainer) {
    val keyState by container.credentials.keyState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { container.credentials.loadAccessKey() }
    when {
        !keyState.loaded -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        !keyState.hasKey -> AccessKeyEntry(container, rejected = false, onBack = null, onDone = {})
        else -> MainBackStack(container, keyState.rejected)
    }
}

@Composable
private fun MainBackStack(
    container: AppContainer,
    rejected: Boolean,
) {
    val backStack = rememberNavBackStack(AppKey.Settings)
    // 키가 있는 동안 앱이 화면에 올라올 때마다 서버 기기 설정의 시간대·언어를 맞춘다(RN 루트 _layout DeviceSettingsSync, D-137)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { container.deviceSettingsSync.sync() }
    }
    // 저장된 키를 서버가 거절하면 어느 화면에서든 B-b로 보낸다. 이미 B-b면 그대로다(D-079)
    LaunchedEffect(rejected) {
        if (rejected && backStack.lastOrNull() != AppKey.AccessKey) backStack.add(AppKey.AccessKey)
    }
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<AppKey.Settings> {
                    SettingsEntry(
                        container,
                        SettingsNav(
                            onBack = if (backStack.size > 1) ({ backStack.removeLastOrNull() }) else null,
                            onOpenAlerts = { backStack.add(AppKey.AlertSettings) },
                            onOpenAccessKey = { backStack.add(AppKey.AccessKey) },
                            onOpenDataSources = { backStack.add(AppKey.DataSources) },
                        ),
                    )
                }
                entry<AppKey.AlertSettings> {
                    val vm: AlertSettingsViewModel =
                        viewModel(
                            factory =
                                viewModelFactory {
                                    initializer {
                                        AlertSettingsViewModel(
                                            container.settingsRepository,
                                        )
                                    }
                                },
                        )
                    AlertSettingsRoute(vm, onBack = { backStack.removeLastOrNull() })
                }
                entry<AppKey.DataSources> { DataSourcesScreen(onBack = { backStack.removeLastOrNull() }) }
                entry<AppKey.AccessKey> {
                    val back = { backStack.removeLastOrNull() }
                    AccessKeyEntry(container, rejected, onBack = { back() }, onDone = { back() })
                }
            },
    )
}

@Composable
private fun AccessKeyEntry(
    container: AppContainer,
    rejected: Boolean,
    onBack: (() -> Unit)?,
    onDone: () -> Unit,
) {
    val vm: AccessKeyViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer {
                        AccessKeyViewModel(
                            verify = { key -> container.apiClient.settings(token = key) },
                            save = container.credentials::saveAccessKey,
                        )
                    }
                },
        )
    AccessKeyRoute(
        viewModel = vm,
        rejected = rejected,
        onBack = onBack,
        onSaved = onDone,
        onLeave = container.credentials::clearRejected,
    )
}

@Composable
private fun SettingsEntry(
    container: AppContainer,
    nav: SettingsNav,
) {
    val vm: SettingsViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer {
                        SettingsViewModel(
                            container.settingsRepository,
                            DisplayControls(
                                upDownStyle = container.displaySettings.settings.map { it.upDownStyle },
                                setUpDownStyle = container.displaySettings::setUpDownStyle,
                                resetUpDownStyle = container.displaySettings::reset,
                                language = { container.appLocale.current },
                                setLanguage = container.appLocale::set,
                                defaultLanguage = { container.appLocale.deviceDefault },
                            ),
                        )
                    }
                },
        )
    val keyState by container.credentials.keyState.collectAsStateWithLifecycle()
    // 같은 앱 ID를 쓰는 형제 앱·변형 구분과 앱 버전·빌드 번호(OTA 없음, D-141)
    val bundle =
        stringResource(R.string.settings_bundle, BuildConfig.FLAVOR, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    SettingsRoute(vm, hasAccessKey = keyState.hasKey, bundleText = bundle, nav = nav)
}
