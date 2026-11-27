package io.github.seyoon.futureetp.core.data

import io.github.seyoon.futureetp.core.api.model.Basis
import io.github.seyoon.futureetp.core.api.model.ChartWindow
import io.github.seyoon.futureetp.core.api.model.DeviceSettings
import io.github.seyoon.futureetp.core.api.model.MaPeriod
import io.github.seyoon.futureetp.core.api.model.SettingsPatch
import io.github.seyoon.futureetp.core.query.Query
import io.github.seyoon.futureetp.core.query.QueryState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 이격도차트 설정 기본값(shared `DISPARITY_SETTINGS_DEFAULTS`, 서버 컬럼 기본값과 같음) */
val DISPARITY_SETTINGS_DEFAULTS =
    SettingsPatch(
        basis = Basis.FRONT,
        maPeriod = MaPeriod.MA100,
        window = ChartWindow.Y10,
        rareLevels = listOf(1.0, 2.0, 4.0, 8.0, 16.0),
    )

/** 보낸 필드만 바꾼 설정 */
fun DeviceSettings.applying(patch: SettingsPatch): DeviceSettings =
    copy(
        locale = patch.locale ?: locale,
        timezone = patch.timezone ?: timezone,
        basis = patch.basis ?: basis,
        maPeriod = patch.maPeriod ?: maPeriod,
        window = patch.window ?: window,
        rareLevels = patch.rareLevels ?: rareLevels,
        alertCommodity = if (patch.alertCommodity != null) patch.alertCommodity.value else alertCommodity,
        quietTime = if (patch.quietTime != null) patch.quietTime.value else quietTime,
    )

private class PendingPatch(
    val patch: SettingsPatch,
)

/**
 * 서버 기기 설정 하나(RN `useSettings`·`useUpdateSettings`). B·B-a·A·C가 같은 값을 본다.
 * - 조회 값([Query])에는 서버가 확인한 값만 두고, 아직 끝나지 않은 변경을 보낸 순서대로 얹어 보여 준다(낙관적 갱신).
 *   연달아 바꾸다 일부가 실패해도 되돌릴 값을 따로 기억하지 않는다
 * - PATCH는 보낸 순서대로 하나씩 간다. 응답은 그 시점의 서버 값이라 받는 대로 조회 값에 넣는다
 * - 실패하면 그 변경만 빠지고 [saveError]가 남는다. 서버에 반영됐을 수도 있어 서버 값을 다시 조회한다
 * - 알림 대상을 바꾸면 A 목록의 🔔도 바뀌므로 [onAlertCommodityChanged]를 부른다(D-124)
 */
class SettingsRepository(
    scope: CoroutineScope,
    fetch: suspend () -> DeviceSettings,
    private val patch: suspend (SettingsPatch) -> DeviceSettings,
    appReturns: Flow<Unit> = emptyFlow(),
    invalidations: Flow<Unit> = emptyFlow(),
    now: () -> Long = System::currentTimeMillis,
    private val onAlertCommodityChanged: () -> Unit = {},
) {
    private val query = Query(scope, fetch, appReturns = appReturns, invalidations = invalidations, now = now)
    private val pending = MutableStateFlow<List<PendingPatch>>(emptyList())
    private val queue = Channel<PendingPatch>(Channel.UNLIMITED)
    private val _saveError = MutableStateFlow<Throwable?>(null)

    /** 마지막 변경의 저장 실패. 새 변경을 보내면 지운다(RN mutation 상태) */
    val saveError: StateFlow<Throwable?> = _saveError.asStateFlow()

    val state: StateFlow<QueryState<DeviceSettings>> =
        combine(query.state, pending) { s, patches ->
            s.copy(data = s.data?.let { data -> patches.fold(data) { acc, p -> acc.applying(p.patch) } })
        }.stateIn(scope, SharingStarted.Eagerly, QueryState())

    init {
        scope.launch { for (p in queue) send(p) }
    }

    /** 화면에 들어올 때 다시 조회한다(RN refetchOnMount, staleTime 0). 진행 중이면 그대로 둔다 */
    fun refresh() = query.refetch(cancelRunning = false)

    fun retry() = query.refetch()

    fun update(patch: SettingsPatch) {
        val p = PendingPatch(patch)
        _saveError.value = null
        pending.update { it + p }
        queue.trySend(p)
    }

    private suspend fun send(p: PendingPatch) {
        // 진행 중인 조회가 PATCH보다 늦게 끝나 옛 값으로 덮지 않게 끊는다
        query.cancel()
        try {
            val settings = patch(p.patch)
            // 그사이 시작된 조회는 PATCH 반영 전 값을 읽었을 수 있어 끊고 응답 값을 넣는다
            query.cancel()
            query.setData(settings)
            if (p.patch.alertCommodity != null) onAlertCommodityChanged()
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            _saveError.value = e
            // 끊은 첫 조회를 다시 시작하고, 응답만 못 받았을 수도 있어 서버 값을 다시 맞춘다
            query.refetch()
        } finally {
            pending.update { list -> list.filterNot { it === p } }
        }
    }
}
