package com.ascdev.keyra.core.security

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import com.ascdev.keyra.BuildConfig

/**
 * Blocks screenshots, screen recording and the preview in the recent apps screen.
 * Debug builds can opt out with -Pkeyra.allowScreenshots=true to capture README images.
 */
fun Activity.applySecureWindow() {
    if (BuildConfig.ALLOW_SCREENSHOTS) return
    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(false)
}
