package io.github.seyoon.futureetp.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.seyoon.futureetp.AppContainer
import io.github.seyoon.futureetp.feature.devhealth.DevHealthRoute
import io.github.seyoon.futureetp.feature.devhealth.DevHealthViewModel

@Composable
fun AppNavigation(container: AppContainer) {
    val backStack = rememberNavBackStack(AppKey.DevHealth)
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
            },
    )
}
