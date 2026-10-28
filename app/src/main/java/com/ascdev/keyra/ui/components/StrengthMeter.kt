package com.ascdev.keyra.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ascdev.keyra.R
import com.ascdev.keyra.domain.strength.PasswordStrength
import com.ascdev.keyra.domain.strength.StrengthLevel
import com.ascdev.keyra.ui.theme.safe
import com.ascdev.keyra.ui.theme.warning

@Composable
fun StrengthMeter(strength: PasswordStrength, modifier: Modifier = Modifier) {
    val color = strengthColor(strength.level)
    val label = stringResource(strengthLabel(strength.level))
    val description = stringResource(R.string.strength_description, label)
    Column(modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description }) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            StrengthLevel.entries.forEach { level ->
                val segment by animateColorAsState(
                    if (level <= strength.level) color else MaterialTheme.colorScheme.surfaceContainerHighest,
                    label = "strengthSegment",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(segment, RoundedCornerShape(3.dp)),
                )
            }
        }
        Text(
            text = stringResource(R.string.strength_label_bits, label, strength.entropyBits.toInt()),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
fun strengthColor(level: StrengthLevel): Color = when (level) {
    StrengthLevel.VERY_WEAK -> MaterialTheme.colorScheme.error
    StrengthLevel.WEAK, StrengthLevel.FAIR -> MaterialTheme.colorScheme.warning
    StrengthLevel.STRONG, StrengthLevel.VERY_STRONG -> MaterialTheme.colorScheme.safe
}

fun strengthLabel(level: StrengthLevel): Int = when (level) {
    StrengthLevel.VERY_WEAK -> R.string.strength_very_weak
    StrengthLevel.WEAK -> R.string.strength_weak
    StrengthLevel.FAIR -> R.string.strength_fair
    StrengthLevel.STRONG -> R.string.strength_strong
    StrengthLevel.VERY_STRONG -> R.string.strength_very_strong
}
