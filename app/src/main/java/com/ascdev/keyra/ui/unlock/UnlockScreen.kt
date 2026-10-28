package com.ascdev.keyra.ui.unlock

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascdev.keyra.R
import com.ascdev.keyra.ui.components.BiometricAuthenticator
import com.ascdev.keyra.ui.components.KeyraLogo
import com.ascdev.keyra.ui.components.PinDots
import com.ascdev.keyra.ui.components.PinPad
import kotlinx.coroutines.launch

@Composable
fun UnlockScreen(
    modifier: Modifier = Modifier,
    viewModel: UnlockViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current as FragmentActivity
    val scope = rememberCoroutineScope()
    val title = stringResource(R.string.biometric_unlock_title)
    val subtitle = stringResource(R.string.biometric_unlock_subtitle)
    val usePin = stringResource(R.string.biometric_use_pin)

    val promptBiometric: () -> Unit = {
        scope.launch {
            val cipher = viewModel.biometricCipher() ?: return@launch
            BiometricAuthenticator.authenticate(
                activity = activity,
                cipher = cipher,
                title = title,
                subtitle = subtitle,
                negativeText = usePin,
                onSuccess = viewModel::onBiometricAuthenticated,
                onError = { message -> if (message != null) viewModel.onBiometricError() },
            )
        }
    }

    LaunchedEffect(Unit) { viewModel.onShown() }

    // Offer the fingerprint straight away, like any banking or password app.
    LaunchedEffect(state.biometricEnabled) {
        if (state.biometricEnabled) promptBiometric()
    }

    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.6f))
        KeyraLogo(size = 72.dp)
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.unlock_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.unlock_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        PinDots(length = state.pinLength, isError = state.error != null)
        Text(
            text = statusText(state),
            color = if (state.lockoutSeconds > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 16.dp)
                .height(40.dp),
        )
        Spacer(Modifier.weight(0.4f))
        PinPad(
            onDigit = viewModel::onDigit,
            onBackspace = viewModel::onBackspace,
            onConfirm = viewModel::onConfirm,
            confirmEnabled = state.pinLength > 0,
            enabled = !state.isWorking && state.lockoutSeconds == 0,
        )
        Spacer(Modifier.height(20.dp))
        if (state.biometricEnabled) {
            FilledTonalButton(onClick = promptBiometric) {
                Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.unlock_use_biometric))
            }
        } else {
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun statusText(state: UnlockUiState): String = when {
    state.lockoutSeconds > 0 -> stringResource(R.string.unlock_locked_out, state.lockoutSeconds)
    state.error == R.string.unlock_error_wrong_pin && state.attemptsLeft != null && state.attemptsLeft > 0 ->
        pluralStringResource(R.plurals.unlock_attempts_left, state.attemptsLeft, state.attemptsLeft)
    state.error != null -> stringResource(state.error)
    else -> ""
}
