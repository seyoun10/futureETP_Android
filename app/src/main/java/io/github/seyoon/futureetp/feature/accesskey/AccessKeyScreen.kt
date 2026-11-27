package io.github.seyoon.futureetp.feature.accesskey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.seyoon.futureetp.R
import io.github.seyoon.futureetp.core.api.AppErrorCode

private val TOUCH_TARGET = 48.dp

/**
 * @param rejected 저장된 키를 서버가 거절해서 왔음. 새 키를 확인하기 전까지 "키가 맞지 않습니다"를 보인다(D-079)
 * @param onBack null이면 [←]가 없다(키가 없어 첫 화면으로 뜬 경우, D-077)
 * @param onLeave 화면을 떠날 때. 거절 표시를 지운다(D-079)
 */
@Composable
fun AccessKeyRoute(
    viewModel: AccessKeyViewModel,
    rejected: Boolean,
    onBack: (() -> Unit)?,
    onSaved: () -> Unit,
    onLeave: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    DisposableEffect(Unit) { onDispose(onLeave) }
    AccessKeyScreen(state = state, rejected = rejected, onBack = onBack, onSubmit = viewModel::submit)
}

@Composable
fun AccessKeyScreen(
    state: AccessKeyUiState,
    rejected: Boolean,
    onBack: (() -> Unit)?,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 키 값은 저장된 상태(Bundle)에 남기지 않는다(rememberTextFieldState는 저장한다)
    val input = remember { TextFieldState() }
    val text = input.text.toString()
    val canSubmit = !state.verifying && text.isNotBlank()
    val submit = { if (canSubmit) onSubmit(text) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            Header(onBack)
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.access_key_guide))
                OutlinedSecureTextField(
                    state = input,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.verifying,
                    placeholder = { Text(stringResource(R.string.access_key_paste_placeholder)) },
                    // 키 값은 화면에 보여 주지 않는다. 마지막 글자도 드러내지 않는다(screens.md B-b)
                    textObfuscationMode = TextObfuscationMode.Hidden,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    onKeyboardAction = { submit() },
                )
                SubmitButton(verifying = state.verifying, enabled = canSubmit, onClick = submit)
                errorText(state, rejected)?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(onBack: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = TOUCH_TARGET).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            val back = stringResource(R.string.common_back)
            TextButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = back }) {
                Text("←", style = MaterialTheme.typography.titleLarge)
            }
        }
        Text(
            stringResource(R.string.access_key_title),
            modifier = Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun SubmitButton(
    verifying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = TOUCH_TARGET)) {
        if (verifying) {
            val label = stringResource(R.string.access_key_verifying)
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp).semantics { contentDescription = label },
                strokeWidth = 2.dp,
            )
        } else {
            Text(stringResource(R.string.access_key_submit))
        }
    }
}

/** 앱 안내 → 새 키 확인 실패 → (확인 중이 아닐 때) 저장된 키 거절 순(RN AccessKeyScreen) */
@Composable
private fun errorText(
    state: AccessKeyUiState,
    rejected: Boolean,
): String? =
    when {
        state.notice == AccessKeyNotice.BAD_FORMAT -> stringResource(R.string.access_key_bad_format)
        state.failure == AppErrorCode.UNAUTHORIZED -> stringResource(R.string.access_key_rejected)
        state.failure != null -> stringResource(state.failure.messageRes)
        rejected && !state.verifying -> stringResource(R.string.access_key_rejected)
        else -> null
    }
