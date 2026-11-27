package io.github.seyoon.futureetp.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 백스택 키(AOS-004). 화면이 늘면 여기에 키를 더한다 */
@Serializable
sealed interface AppKey : NavKey {
    @Serializable
    data object DevHealth : AppKey
}
