package com.ascdev.keyra

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascdev.keyra.core.security.applySecureWindow
import com.ascdev.keyra.core.session.AutoLockManager
import com.ascdev.keyra.ui.root.KeyraRootRoute
import com.ascdev.keyra.ui.root.RootViewModel
import com.ascdev.keyra.ui.theme.KeyraTheme
import com.ascdev.keyra.ui.theme.SyncSystemBars
import com.ascdev.keyra.ui.theme.isKeyraDark
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// FragmentActivity because BiometricPrompt hosts its dialog in a fragment.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var autoLockManager: AutoLockManager

    private val rootViewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        applySecureWindow()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by rootViewModel.themeMode.collectAsStateWithLifecycle()
            SyncSystemBars(isKeyraDark(themeMode))
            KeyraTheme(themeMode) {
                KeyraRootRoute(rootViewModel)
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        autoLockManager.onUserInteraction()
    }
}
