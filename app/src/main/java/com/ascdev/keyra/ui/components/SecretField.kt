package com.ascdev.keyra.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.ascdev.keyra.R

fun secretTransformation(visible: Boolean): VisualTransformation =
    if (visible) VisualTransformation.None else PasswordVisualTransformation()

/** Keyboard options that keep secrets out of the keyboard's suggestions and learning. */
val SecretKeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)

@Composable
fun VisibilityToggle(visible: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = stringResource(
                if (visible) R.string.action_hide_password else R.string.action_show_password,
            ),
        )
    }
}

/** Replaces a secret with bullets for read-only display. */
fun maskSecret(secret: String): String = "•".repeat(secret.length.coerceIn(MIN_MASK_LENGTH, MAX_MASK_LENGTH))

// The mask length only loosely follows the real one, so it does not reveal it.
private const val MIN_MASK_LENGTH = 8
private const val MAX_MASK_LENGTH = 16
