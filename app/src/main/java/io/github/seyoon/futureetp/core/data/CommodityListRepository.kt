package io.github.seyoon.futureetp.core.data

import io.github.seyoon.futureetp.core.api.model.CommodityList
import io.github.seyoon.futureetp.core.query.Query
import io.github.seyoon.futureetp.core.query.QueryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * A 원자재 목록 조회(RN `useCommodities`, queryKey `commodities.list`). 화면 사이에 남는다.
 * - 자동 새로고침은 `freshness.nextUpdateAt` 기준(D-048, D-075), 앱 복귀 때 다시(staleTime 0, D-030)
 * - [invalidations]: 새 접근 키 저장, 알림 대상 변경(🔔가 바뀐다, D-124)
 */
class CommodityListRepository(
    scope: CoroutineScope,
    fetch: suspend () -> CommodityList,
    appReturns: Flow<Unit> = emptyFlow(),
    invalidations: Flow<Unit> = emptyFlow(),
    now: () -> Long = System::currentTimeMillis,
) {
    val query: Query<CommodityList> =
        Query(
            scope,
            fetch,
            nextUpdateAt = { it.freshness.nextUpdateAt },
            appReturns = appReturns,
            invalidations = invalidations,
            now = now,
        )

    val state: StateFlow<QueryState<CommodityList>> = query.state

    fun retry() = query.refetch()

    /** 당겨서 새로고침. 성공하면 true */
    suspend fun pullRefresh(): Boolean = query.refetchAndAwait()
}
