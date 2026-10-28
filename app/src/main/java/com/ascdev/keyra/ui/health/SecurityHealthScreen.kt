package com.ascdev.keyra.ui.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascdev.keyra.R
import com.ascdev.keyra.domain.model.VaultEntry
import com.ascdev.keyra.ui.components.CategoryBadge
import com.ascdev.keyra.ui.theme.safe
import com.ascdev.keyra.ui.theme.warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityHealthScreen(
    onBack: () -> Unit,
    onEntryClick: (Long) -> Unit,
    viewModel: SecurityHealthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.health_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (state.isLoading) return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                val colors = MaterialTheme.colorScheme
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(Icons.Outlined.GppGood, state.healthy, R.string.health_safe, colors.safe, Modifier.weight(1f))
                    StatCard(Icons.Outlined.WarningAmber, state.report.weak.size, R.string.health_weak, colors.warning, Modifier.weight(1f))
                    StatCard(
                        Icons.Outlined.ContentCopy,
                        state.report.reusedIds.size,
                        R.string.health_reused,
                        colors.warning,
                        Modifier.weight(1f),
                    )
                }
            }
            if (state.report.issueCount == 0) {
                item { AllGood(hasEntries = state.total > 0) }
            }
            if (state.report.reusedGroups.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.health_reused_section)) }
                state.report.reusedGroups.forEach { group ->
                    item {
                        Text(
                            pluralStringResource(R.plurals.health_reused_group, group.size, group.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        )
                    }
                    items(group, key = { "reused-${it.id}" }) { IssueRow(it) { onEntryClick(it.id) } }
                }
            }
            if (state.report.weak.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.health_weak_section)) }
                items(state.report.weak, key = { "weak-${it.id}" }) { IssueRow(it) { onEntryClick(it.id) } }
            }
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, value: Int, label: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(18.dp), modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(Modifier.height(10.dp))
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, color = color)
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, start = 4.dp),
    )
}

@Composable
private fun IssueRow(entry: VaultEntry, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(entry.category, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.titleSmall)
                if (entry.username.isNotBlank()) {
                    Text(entry.username, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun AllGood(hasEntries: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.GppGood, contentDescription = null, tint = MaterialTheme.colorScheme.safe, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(if (hasEntries) R.string.health_all_good else R.string.health_no_entries),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
