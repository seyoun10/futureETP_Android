package io.github.seyoon.futureetp.feature.devhealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.seyoon.futureetp.R

@Composable
fun DevHealthRoute(
    viewModel: DevHealthViewModel,
    serverUrl: String,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DevHealthScreen(state = state, serverUrl = serverUrl, onRetry = viewModel::refresh)
}

@Composable
fun DevHealthScreen(
    state: DevHealthState,
    serverUrl: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.safeDrawingPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.dev_health_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.dev_health_server, serverUrl))
            Text(
                when (state) {
                    DevHealthState.Loading -> stringResource(R.string.dev_health_loading)
                    is DevHealthState.Ok ->
                        stringResource(
                            R.string.dev_health_ok,
                            state.commit ?: stringResource(R.string.dev_health_no_commit),
                        )
                    is DevHealthState.Failed -> stringResource(R.string.dev_health_error, state.reason)
                },
            )
            Button(onClick = onRetry, enabled = state != DevHealthState.Loading) {
                Text(stringResource(R.string.dev_health_retry))
            }
        }
    }
}
