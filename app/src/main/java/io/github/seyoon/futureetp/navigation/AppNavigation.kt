package io.github.seyoon.futureetp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.seyoon.futureetp.AppContainer
import io.github.seyoon.futureetp.core.api.settings
import io.github.seyoon.futureetp.feature.accesskey.AccessKeyRoute
import io.github.seyoon.futureetp.feature.accesskey.AccessKeyViewModel
import io.github.seyoon.futureetp.feature.devhealth.DevHealthRoute
import io.github.seyoon.futureetp.feature.devhealth.DevHealthViewModel

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
    val backStack = rememberNavBackStack(AppKey.DevHealth)
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
                entry<AppKey.DevHealth> {
                    val vm: DevHealthViewModel =
                        viewModel(
                            factory =
                                viewModelFactory {
                                    initializer { DevHealthViewModel(container.apiClient::health) }
                                },
                        )
                    DevHealthRoute(vm, container.apiBaseUrl)
                }
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
