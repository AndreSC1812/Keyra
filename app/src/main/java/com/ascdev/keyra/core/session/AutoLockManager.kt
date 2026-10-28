package com.ascdev.keyra.core.session

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.ascdev.keyra.core.di.ApplicationScope
import com.ascdev.keyra.core.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Locks the vault when the app goes to the background or after a period of inactivity.
 * Flows that briefly leave the app on purpose (the file picker for backups) can call
 * [pauseBackgroundLock] so the vault is not locked under their feet.
 */
@Singleton
class AutoLockManager @Inject constructor(
    private val session: VaultSession,
    private val settings: SettingsRepository,
    @param:ApplicationScope private val scope: CoroutineScope,
) : DefaultLifecycleObserver {

    private val lastInteraction = MutableStateFlow(SystemClock.elapsedRealtime())

    @Volatile
    private var backgroundLockPaused = false

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        scope.launch {
            val timeoutMs = settings.settings.map { it.autoLockMinutes * MINUTE_MS }.distinctUntilChanged()
            // Every interaction restarts the countdown thanks to collectLatest.
            combine(session.isUnlocked, timeoutMs, lastInteraction) { unlocked, timeout, _ -> unlocked to timeout }
                .collectLatest { (unlocked, timeout) ->
                    if (unlocked) {
                        delay(timeout)
                        lockNow()
                    }
                }
        }
    }

    fun onUserInteraction() {
        lastInteraction.value = SystemClock.elapsedRealtime()
    }

    fun pauseBackgroundLock() {
        backgroundLockPaused = true
    }

    fun resumeBackgroundLock() {
        backgroundLockPaused = false
        onUserInteraction()
    }

    fun lockNow() = session.lock()

    override fun onStop(owner: LifecycleOwner) {
        if (backgroundLockPaused) return
        scope.launch {
            if (settings.current().lockOnBackground) lockNow()
        }
    }

    private companion object {
        const val MINUTE_MS = 60_000L
    }
}
