package io.github.seyoon.futureetp.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.seyoon.futureetp.R
import io.github.seyoon.futureetp.core.theme.AppColors
import io.github.seyoon.futureetp.core.theme.FontSize
import io.github.seyoon.futureetp.core.theme.Spacing
import io.github.seyoon.futureetp.core.ui.RowDescription

private val FIELD_WIDTH = 48.dp
private val FIELD_MIN_HEIGHT = 36.dp

/**
 * rare-level 5칸(screens.md B, RN `RareLevelsInput`). 키패드는 키패드의 완료 키로만 닫고, 화면을 끌거나 빈 곳·다른 행을 눌러도
 * 닫히지 않는다. 키패드가 닫힐 때 5개가 모두 맞으면 정렬해 저장하고, 아니면 오류를 보이고 저장하지 않는다.
 * 칸을 벗어날 때마다 저장하면 순서대로 입력하는 중에 정렬된 값으로 칸이 바뀐다. 칸 사이를 옮기는 동안은 키패드가 열려 있다.
 * 키패드가 올라오면 입력칸과 그 아래 안내 줄이 가리지 않게 끌어올린다
 * @param levels 조회 중이면 null(입력 막음)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RareLevelsInput(
    levels: List<Double>?,
    onSave: (List<Double>) -> Unit,
    description: String,
) {
    val fields = remember { List(RARE_LEVEL_COUNT) { TextFieldState() } }
    // 고치는 중. 아니면 서버 값을 보여 준다(저장 뒤 정렬된 값으로 돌아간다)
    var editing by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<RareLevelProblem?>(null) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val requester = remember { BringIntoViewRequester() }
    val imeVisible = WindowInsets.isImeVisible

    LaunchedEffect(levels, editing) {
        if (editing) return@LaunchedEffect
        fields.forEachIndexed { i, field ->
            field.setTextAndPlaceCursorAtEnd(levels?.getOrNull(i)?.let(::rareLevelText).orEmpty())
        }
    }
    // 완료 키는 포커스를 빼서 키패드를 닫는다. 시스템 뒤로 버튼으로 키패드만 닫혀도 저장한다
    LaunchedEffect(imeVisible, focused) {
        val keyboardOpen = imeVisible && focused
        if (keyboardOpen || !editing || levels == null) return@LaunchedEffect
        when (val check = checkRareLevels(fields.map { it.text.toString() })) {
            is RareLevelsCheck.Invalid -> problem = check.problem
            is RareLevelsCheck.Valid -> {
                editing = false
                if (check.levels != levels) onSave(check.levels)
            }
        }
    }
    KeepVisibleAboveKeyboard(requester, focused)

    Column(
        Modifier
            .bringIntoViewRequester(requester)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        Row(
            Modifier.onFocusChanged { focused = it.hasFocus },
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.settings_rare_levels),
                modifier = Modifier.weight(1f),
                color = AppColors.text,
                fontSize = FontSize.body,
            )
            fields.forEachIndexed { index, field ->
                LevelField(
                    state = field,
                    label = stringResource(R.string.settings_rare_level_field, index + 1),
                    enabled = levels != null,
                    onEdit = {
                        editing = true
                        problem = null
                    },
                    onDone = { focusManager.clearFocus() },
                )
            }
        }
        RareLevelHint(problem)
        RowDescription(description, Modifier.padding(top = Spacing.xs))
    }
}

/** 키패드가 올라오는 동안 포커스된 칸 묶음(칸 아래 안내 줄까지)이 가리지 않게 끌어올린다(RN `useKeepFocusedInputVisible`) */
@Composable
private fun KeepVisibleAboveKeyboard(
    requester: BringIntoViewRequester,
    focused: Boolean,
) {
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeBottom, focused) { if (focused && imeBottom > 0) requester.bringIntoView() }
}

/** 입력 범위 안내. 벗어나거나 중복이면 같은 자리에 빨간 오류(오른쪽 정렬) */
@Composable
private fun RareLevelHint(problem: RareLevelProblem?) {
    val min = rareLevelText(RARE_LEVEL_MIN)
    val max = rareLevelText(RARE_LEVEL_MAX)
    Text(
        when (problem) {
            RareLevelProblem.INVALID -> stringResource(R.string.settings_rare_level_invalid, min, max)
            RareLevelProblem.DUPLICATE -> stringResource(R.string.settings_rare_level_duplicate, RARE_LEVEL_COUNT)
            null -> stringResource(R.string.settings_rare_level_hint, min, max, RARE_LEVEL_COUNT)
        },
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
        color = if (problem != null) AppColors.error else AppColors.textSecondary,
        fontSize = FontSize.caption,
        textAlign = TextAlign.End,
    )
}

/** 사용자 입력만 받아 [onEdit]을 부르고 글자 수를 막는다(서버 값으로 채울 때는 부르지 않는다) */
private class LevelInput(
    private val onEdit: () -> Unit,
) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        if (length > RARE_LEVEL_MAX_LENGTH) revertAllChanges() else onEdit()
    }
}

/** 숫자 키패드 한 칸. 들어가면 글자를 모두 골라 바로 고쳐 쓸 수 있다 */
@Composable
private fun LevelField(
    state: TextFieldState,
    label: String,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDone: () -> Unit,
) {
    BasicTextField(
        state = state,
        modifier =
            Modifier
                .width(FIELD_WIDTH)
                .onFocusChanged { if (it.isFocused) state.edit { selectAll() } }
                .semantics { contentDescription = label },
        enabled = enabled,
        inputTransformation = remember(onEdit) { LevelInput(onEdit) },
        textStyle = TextStyle(color = AppColors.text, fontSize = FontSize.body, textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        onKeyboardAction = { onDone() },
        lineLimits = TextFieldLineLimits.SingleLine,
        cursorBrush = SolidColor(AppColors.text),
        decorator = { inner ->
            Box(
                Modifier
                    .heightIn(min = FIELD_MIN_HEIGHT)
                    .background(AppColors.surface, RoundedCornerShape(Spacing.sm))
                    .padding(horizontal = Spacing.xs),
                contentAlignment = Alignment.Center,
            ) { inner() }
        },
    )
}
