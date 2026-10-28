package com.ascdev.keyra.core.security

/** Exponential back-off after too many wrong PINs: 30 s, 1 min, 2 min... up to 15 min. */
object PinLockoutPolicy {
    const val FREE_ATTEMPTS = 5
    private const val BASE_LOCKOUT_MS = 30_000L
    private const val MAX_LOCKOUT_MS = 15 * 60_000L

    fun lockoutDurationMs(failedAttempts: Int): Long {
        if (failedAttempts < FREE_ATTEMPTS) return 0L
        val exponent = (failedAttempts - FREE_ATTEMPTS).coerceAtMost(MAX_EXPONENT)
        return (BASE_LOCKOUT_MS shl exponent).coerceAtMost(MAX_LOCKOUT_MS)
    }

    private const val MAX_EXPONENT = 10
}
