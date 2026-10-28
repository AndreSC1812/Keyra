package com.ascdev.keyra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascdev.keyra.ui.theme.Indigo
import com.ascdev.keyra.ui.theme.Mint

@Composable
fun KeyraLogo(modifier: Modifier = Modifier, size: Dp = 88.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(Brush.linearGradient(listOf(Indigo, Mint)), RoundedCornerShape(size / 3.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
            modifier = Modifier.size(size * 0.62f),
        )
        Icon(
            Icons.Rounded.Key,
            contentDescription = null,
            tint = Mint,
            modifier = Modifier.size(size * 0.3f),
        )
    }
}
