package io.github.seyoon.futureetp.navigation

import androidx.navigation3.runtime.NavKey
import io.github.seyoon.futureetp.core.api.model.CommodityCode
import kotlinx.serialization.Serializable

/** 백스택 키(AOS-004). 화면이 늘면 여기에 키를 더한다 */
@Serializable
sealed interface AppKey : NavKey {
    /** A 원자재 목록. 첫 화면 */
    @Serializable
    data object CommodityList : AppKey

    /** C 원자재 화면 */
    @Serializable
    data class Commodity(
        val code: CommodityCode,
    ) : AppKey

    /** B 설정 */
    @Serializable
    data object Settings : AppKey

    /** B-a 알림 설정 */
    @Serializable
    data object AlertSettings : AppKey

    @Serializable
    data object DataSources : AppKey

    /** B-b 접근 키. 키가 있을 때 B > 접근 키·저장된 키 거절로 들어온다(키가 없을 때는 백스택 밖에서 그린다) */
    @Serializable
    data object AccessKey : AppKey
}
