package io.github.seyoon.futureetp.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 백스택 키(AOS-004). 화면이 늘면 여기에 키를 더한다 */
@Serializable
sealed interface AppKey : NavKey {
    /** B 설정. A를 만들기 전까지 첫 화면이다 */
    @Serializable
    data object Settings : AppKey

    @Serializable
    data object DataSources : AppKey

    /** B-b 접근 키. 키가 있을 때 B > 접근 키·저장된 키 거절로 들어온다(키가 없을 때는 백스택 밖에서 그린다) */
    @Serializable
    data object AccessKey : AppKey
}
