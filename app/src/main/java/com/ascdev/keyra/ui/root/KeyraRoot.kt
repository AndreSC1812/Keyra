package com.ascdev.keyra.ui.root

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascdev.keyra.ui.navigation.KeyraNavHost
import com.ascdev.keyra.ui.setup.SetupScreen
import com.ascdev.keyra.ui.unlock.UnlockScreen

/**
 * Top-level switch between setup, the lock screen and the vault. The vault navigation graph
 * leaves the composition when the vault locks, so no decrypted screen state outlives it.
 */
@Composable
fun KeyraRoot(state: VaultState) {
    // Surface provides the content color, so Text outside a Scaffold is readable in both themes.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "vaultState",
        ) { target ->
            when (target) {
                VaultState.LOADING -> Unit
                VaultState.NEEDS_SETUP -> SetupScreen()
                VaultState.LOCKED -> UnlockScreen()
                VaultState.UNLOCKED -> KeyraNavHost()
            }
        }
    }
}

@Composable
fun KeyraRootRoute(viewModel: RootViewModel) {
    val state by viewModel.vaultState.collectAsStateWithLifecycle()
    KeyraRoot(state)
}
