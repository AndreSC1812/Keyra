package com.ascdev.keyra.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascdev.keyra.R
import com.ascdev.keyra.domain.model.Category
import com.ascdev.keyra.ui.components.SecretKeyboardOptions
import com.ascdev.keyra.ui.components.StrengthMeter
import com.ascdev.keyra.ui.components.VisibilityToggle
import com.ascdev.keyra.ui.components.icon
import com.ascdev.keyra.ui.components.label
import com.ascdev.keyra.ui.components.secretTransformation
import com.ascdev.keyra.ui.generator.GeneratorPanel
import com.ascdev.keyra.ui.generator.GeneratorViewModel
import com.ascdev.keyra.ui.theme.SecretTextStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryEditScreen(onDone: () -> Unit, viewModel: EntryEditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showGenerator by remember { mutableStateOf(false) }
    val form = state.form

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.edit_new_title else R.string.edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Rounded.Close, stringResource(R.string.action_cancel)) }
                },
                actions = {
                    TextButton(onClick = { viewModel.save(onDone) }, enabled = !state.isSaving && !state.isLoading) {
                        Text(stringResource(R.string.action_save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (state.isLoading) return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = form.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text(stringResource(R.string.field_title)) },
                isError = state.showErrors && form.title.isBlank(),
                supportingText = if (state.showErrors && form.title.isBlank()) {
                    { Text(stringResource(R.string.error_required)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text(stringResource(R.string.field_username)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Column {
                OutlinedTextField(
                    value = form.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text(stringResource(R.string.field_password)) },
                    isError = state.showErrors && form.password.isEmpty(),
                    supportingText = if (state.showErrors && form.password.isEmpty()) {
                        { Text(stringResource(R.string.error_required)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    textStyle = SecretTextStyle,
                    visualTransformation = secretTransformation(state.passwordVisible),
                    keyboardOptions = SecretKeyboardOptions,
                    trailingIcon = {
                        Row {
                            VisibilityToggle(state.passwordVisible, viewModel::togglePasswordVisibility)
                            IconButton(onClick = { showGenerator = true }) {
                                Icon(Icons.Outlined.Password, stringResource(R.string.action_generate))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                state.strength?.let { StrengthMeter(it, Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)) }
            }
            OutlinedTextField(
                value = form.url,
                onValueChange = viewModel::onUrlChange,
                label = { Text(stringResource(R.string.field_url)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.field_category), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { category ->
                    FilterChip(
                        selected = form.category == category,
                        onClick = { viewModel.onCategoryChange(category) },
                        label = { Text(stringResource(category.label)) },
                        leadingIcon = { Icon(category.icon, contentDescription = null) },
                    )
                }
            }
            OutlinedTextField(
                value = form.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text(stringResource(R.string.field_notes)) },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showGenerator) {
        val generatorViewModel: GeneratorViewModel = hiltViewModel()
        ModalBottomSheet(
            onDismissRequest = { showGenerator = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Text(
                stringResource(R.string.generator_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            GeneratorPanel(
                viewModel = generatorViewModel,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                primaryAction = { password ->
                    Button(
                        onClick = {
                            viewModel.onPasswordChange(password)
                            showGenerator = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Text(stringResource(R.string.action_use_password))
                    }
                },
            )
        }
    }
}
