package com.ascdev.keyra.core.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import com.ascdev.keyra.core.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies secrets flagged as sensitive (hidden from the clipboard preview on Android 13+) and
 * wipes them automatically after [CLEAR_DELAY_MS].
 */
@Singleton
class SecureClipboard @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val clipboard = context.getSystemService(ClipboardManager::class.java)
    private var clearJob: Job? = null

    /** Android 13+ already shows its own confirmation when something is copied. */
    val showsSystemConfirmation: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun copy(label: String, text: String, sensitive: Boolean = true) {
        val clip = ClipData.newPlainText(label, text)
        if (sensitive) {
            clip.description.extras = PersistableBundle().apply { putBoolean(EXTRA_IS_SENSITIVE, true) }
        }
        clipboard.setPrimaryClip(clip)

        clearJob?.cancel()
        if (sensitive) {
            clearJob = scope.launch(Dispatchers.Main) {
                delay(CLEAR_DELAY_MS)
                clear()
            }
        }
    }

    fun clear() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }

    companion object {
        const val CLEAR_DELAY_MS = 30_000L

        private val EXTRA_IS_SENSITIVE =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ClipDescription.EXTRA_IS_SENSITIVE
            } else {
                "android.content.extra.IS_SENSITIVE"
            }
    }
}
