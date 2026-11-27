package io.github.seyoon.futureetp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.seyoon.futureetp.R
import io.github.seyoon.futureetp.core.api.errorTextRes
import io.github.seyoon.futureetp.core.query.Query
import io.github.seyoon.futureetp.core.query.QueryState
import io.github.seyoon.futureetp.core.theme.AppColors
import io.github.seyoon.futureetp.core.theme.FontSize
import io.github.seyoon.futureetp.core.theme.Size
import io.github.seyoon.futureetp.core.theme.Spacing

private val SKELETON_BAR_WIDTHS = listOf(0.9f, 0.6f, 0.75f)

/**
 * 조회 화면 상태 분기(RN `QueryStateView`, architecture.md 화면 표시). 데이터가 한 번이라도 있으면 새로고침이 실패해도
 * 그 데이터를 계속 보여 주고(기준 시각 줄로 오래됨을 알림), 처음 로딩 실패만 오류 화면이다.
 * [skeleton]은 화면 모양에 맞춘 스켈레톤, 없으면 공용 막대 스켈레톤
 */
@Composable
fun <T> QueryStateView(
    state: QueryState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    skeleton: (@Composable () -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    val data = state.data
    when {
        data != null -> content(data)
        state.error != null && !state.isFetching -> ErrorView(state.error, onRetry, modifier)
        skeleton != null -> skeleton()
        else -> LoadingView(modifier)
    }
}

/** 공용 막대 스켈레톤 + 가운데 스피너. 접근성은 진행 표시(불러오는 중) */
@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    val loading = stringResource(R.string.common_loading)
    Box(
        modifier
            .fillMaxSize()
            .background(AppColors.background)
            .semantics(mergeDescendants = true) {
                contentDescription = loading
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SKELETON_BAR_WIDTHS.forEach { width ->
                Box(
                    Modifier
                        .fillMaxWidth(width)
                        .height(Spacing.xl)
                        .background(AppColors.surface, RoundedCornerShape(Spacing.xs)),
                )
            }
        }
        CenterSpinner()
    }
}

/**
 * 첫 조회 중 스켈레톤 위 가운데에 겹치는 큰 스피너(RN `CenterSpinner`). 응답이 늦어도 불러오는 중임이 보이게 한다.
 * 접근성 라벨은 감싸는 스켈레톤이 갖는다. 감싸는 Box 안에서 부른다
 */
@Composable
fun CenterSpinner(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AppColors.textSecondary)
    }
}

/** 안내 문구 + [다시 시도]. 문구는 에러 code로 고르고, AppError가 아니면 INTERNAL 문구다(D-042) */
@Composable
fun ErrorView(
    error: Throwable,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(AppColors.background)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(errorTextRes(error)),
            color = AppColors.text,
            fontSize = FontSize.body,
            textAlign = TextAlign.Center,
        )
        TextButton(
            onClick = onRetry,
            modifier = Modifier.heightIn(min = Size.touchTarget),
            shape = RoundedCornerShape(Spacing.sm),
            colors =
                ButtonDefaults.textButtonColors(
                    containerColor = AppColors.surface,
                    contentColor = AppColors.text,
                ),
        ) {
            Text(
                stringResource(R.string.common_retry),
                fontSize = FontSize.body,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
        }
    }
}

/** 화면이 보이는(STARTED) 동안 [query]를 `nextUpdateAt` + 1분에 자동 새로고침한다(RN `useAutoRefresh`, D-048, D-075) */
@Composable
fun AutoRefreshEffect(query: Query<*>) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(query, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { query.autoRefresh() }
    }
}
