package com.ascdev.keyra.ui.setup

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhonelinkLock
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.ascdev.keyra.ui.theme.safe

@Composable
fun SetupScreen(viewModel: SetupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current as FragmentActivity
    val context = LocalContext.current

    val biometricTitle = stringResource(R.string.biometric_enable_title)
    val biometricSubtitle = stringResource(R.string.biometric_enable_subtitle)
    val cancel = stringResource(R.string.action_cancel)

    AnimatedContent(targetState = state.step, label = "setupStep", modifier = Modifier.safeDrawingPadding()) { step ->
        when (step) {
            SetupStep.WELCOME -> WelcomeStep(onStart = viewModel::start)
            SetupStep.CREATE_PIN, SetupStep.CONFIRM_PIN -> PinStep(
                title = stringResource(
                    if (step == SetupStep.CREATE_PIN) R.string.setup_create_pin_title else R.string.setup_confirm_pin_title,
                ),
                subtitle = stringResource(
                    if (step == SetupStep.CREATE_PIN) R.string.setup_create_pin_subtitle else R.string.setup_confirm_pin_subtitle,
                ),
                state = state,
                onDigit = viewModel::onDigit,
                onBackspace = viewModel::onBackspace,
                onConfirm = viewModel::onConfirm,
                onBack = if (step == SetupStep.CONFIRM_PIN) viewModel::backToCreate else null,
            )
            SetupStep.BIOMETRIC -> BiometricStep(
                available = BiometricAuthenticator.isAvailable(context),
                error = state.error?.let { stringResource(it) },
                onEnable = {
                    runCatching { viewModel.biometricCipher() }
                        .onSuccess { cipher ->
                            BiometricAuthenticator.authenticate(
                                activity = activity,
                                cipher = cipher,
                                title = biometricTitle,
                                subtitle = biometricSubtitle,
                                negativeText = cancel,
                                onSuccess = viewModel::onBiometricAuthenticated,
                                onError = { message ->
                                    if (message != null) viewModel.onBiometricError(R.string.biometric_error_generic)
                                },
                            )
                        }
                        .onFailure { viewModel.onBiometricError(R.string.biometric_error_generic) }
                },
                onSkip = viewModel::skipBiometric,
            )
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        KeyraLogo(size = 104.dp)
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.setup_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(36.dp))
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Feature(Icons.Outlined.WifiOff, R.string.setup_feature_offline)
            Feature(Icons.Outlined.PhonelinkLock, R.string.setup_feature_keystore)
            Feature(Icons.Outlined.Fingerprint, R.string.setup_feature_biometric)
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(R.string.setup_get_started))
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, text: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.safe)
        Spacer(Modifier.width(16.dp))
        Text(stringResource(text), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PinStep(
    title: String,
    subtitle: String,
    state: SetupUiState,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    onBack: (() -> Unit)?,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.6f))
        Icon(
            Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        PinDots(length = state.pinLength, isError = state.error != null)
        Text(
            text = state.error?.let { stringResource(it) }.orEmpty(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 16.dp)
                .height(40.dp),
        )
        Spacer(Modifier.weight(0.4f))
        if (state.isWorking) {
            CircularProgressIndicator(Modifier.padding(bottom = 24.dp))
        }
        PinPad(
            onDigit = onDigit,
            onBackspace = onBackspace,
            onConfirm = onConfirm,
            confirmEnabled = state.pinLength > 0,
            enabled = !state.isWorking,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { onBack?.invoke() }, enabled = onBack != null) {
            Text(if (onBack != null) stringResource(R.string.setup_change_pin) else "")
        }
    }
}

@Composable
private fun BiometricStep(available: Boolean, error: String?, onEnable: () -> Unit, onSkip: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.Outlined.Fingerprint,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.safe,
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.setup_biometric_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(if (available) R.string.setup_biometric_subtitle else R.string.setup_biometric_unavailable),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.weight(1f))
        if (available) {
            Button(onClick = onEnable, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.setup_biometric_enable))
            }
            Spacer(Modifier.height(8.dp))
        }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (available) R.string.setup_biometric_skip else R.string.setup_finish))
        }
    }
}
