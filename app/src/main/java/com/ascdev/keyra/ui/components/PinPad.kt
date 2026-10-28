package com.ascdev.keyra.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ascdev.keyra.R
import com.ascdev.keyra.domain.pin.PinValidator

@Composable
fun PinDots(length: Int, modifier: Modifier = Modifier, isError: Boolean = false) {
    val slots = maxOf(length, PinValidator.MIN_LENGTH)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(slots) { index ->
            val filled = index < length
            val color by animateColorAsState(
                when {
                    isError -> MaterialTheme.colorScheme.error
                    filled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                },
                label = "pinDot",
            )
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            )
        }
    }
}

@Composable
fun PinPad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val rows = listOf("123", "456", "789")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { digit -> DigitKey(digit, enabled) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(KeySize), contentAlignment = Alignment.Center) {
                IconButton(onClick = onBackspace, enabled = enabled, modifier = Modifier.size(KeySize)) {
                    Icon(Icons.AutoMirrored.Outlined.Backspace, stringResource(R.string.pin_delete))
                }
            }
            DigitKey('0', enabled) { onDigit('0') }
            Box(Modifier.size(KeySize), contentAlignment = Alignment.Center) {
                FilledIconButton(
                    onClick = onConfirm,
                    enabled = enabled && confirmEnabled,
                    modifier = Modifier.size(KeySize - 8.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(Icons.Rounded.Check, stringResource(R.string.pin_confirm))
                }
            }
        }
    }
}

@Composable
private fun DigitKey(digit: Char, enabled: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(KeySize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = CircleShape,
    ) {
        Text(
            text = digit.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val KeySize = 72.dp
