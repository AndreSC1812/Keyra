package com.ascdev.keyra.ui.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.toggleable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascdev.keyra.R
import com.ascdev.keyra.domain.generator.GeneratorOptions
import com.ascdev.keyra.ui.components.StrengthMeter
import com.ascdev.keyra.ui.theme.SecretTextStyle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(onBack: () -> Unit, viewModel: GeneratorViewModel = hiltViewModel()) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val label = stringResource(R.string.field_password)
    val copied = stringResource(R.string.copied_password)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.generator_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        GeneratorPanel(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            primaryAction = {
                Button(
                    onClick = {
                        viewModel.copy(label)
                        if (viewModel.showsCopyConfirmation) scope.launch { snackbar.showSnackbar(copied) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_copy))
                }
            },
        )
    }
}

/** Shared by the generator screen and the bottom sheet of the entry editor. */
@Composable
fun GeneratorPanel(
    viewModel: GeneratorViewModel,
    modifier: Modifier = Modifier,
    primaryAction: @Composable (password: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state.password,
                        style = SecretTextStyle,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = viewModel::regenerate) {
                        Icon(Icons.Rounded.Refresh, stringResource(R.string.action_regenerate))
                    }
                }
                StrengthMeter(state.strength, Modifier.padding(top = 16.dp))
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.generator_length), modifier = Modifier.weight(1f))
                    Text(
                        state.options.length.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                    )
                }
                Slider(
                    value = state.options.length.toFloat(),
                    onValueChange = { viewModel.onLengthChange(it.toInt()) },
                    valueRange = GeneratorOptions.MIN_LENGTH.toFloat()..GeneratorOptions.MAX_LENGTH.toFloat(),
                )
                OptionSwitch(R.string.generator_uppercase, state.options.uppercase, viewModel::onUppercaseChange)
                OptionSwitch(R.string.generator_lowercase, state.options.lowercase, viewModel::onLowercaseChange)
                OptionSwitch(R.string.generator_digits, state.options.digits, viewModel::onDigitsChange)
                OptionSwitch(R.string.generator_symbols, state.options.symbols, viewModel::onSymbolsChange)
            }
        }

        primaryAction(state.password)
        FilledTonalButton(onClick = viewModel::regenerate, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_regenerate))
        }
    }
}

@Composable
private fun OptionSwitch(label: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
            .padding(vertical = 6.dp)
            .semantics { role = Role.Switch },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(label), modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
