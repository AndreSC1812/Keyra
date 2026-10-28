package com.ascdev.keyra.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascdev.keyra.core.security.VaultKeyManager
import com.ascdev.keyra.core.session.VaultSession
import com.ascdev.keyra.core.settings.SettingsRepository
import com.ascdev.keyra.core.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class VaultState { LOADING, NEEDS_SETUP, LOCKED, UNLOCKED }

@HiltViewModel
class RootViewModel @Inject constructor(
    vaultKeyManager: VaultKeyManager,
    session: VaultSession,
    settings: SettingsRepository,
) : ViewModel() {

    val vaultState: StateFlow<VaultState> =
        combine(vaultKeyManager.isSetupComplete, session.isUnlocked) { setupComplete, unlocked ->
            when {
                !setupComplete -> VaultState.NEEDS_SETUP
                unlocked -> VaultState.UNLOCKED
                else -> VaultState.LOCKED
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, VaultState.LOADING)

    val themeMode: StateFlow<ThemeMode> = settings.settings
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DARK)
}
