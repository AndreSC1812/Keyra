package com.ascdev.keyra.core

import com.ascdev.keyra.core.security.PinLockoutPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class PinLockoutPolicyTest {

    @Test
    fun `first attempts are free`() {
        (0 until PinLockoutPolicy.FREE_ATTEMPTS).forEach {
            assertEquals(0L, PinLockoutPolicy.lockoutDurationMs(it))
        }
    }

    @Test
    fun `lockout doubles and is capped`() {
        assertEquals(30_000L, PinLockoutPolicy.lockoutDurationMs(5))
        assertEquals(60_000L, PinLockoutPolicy.lockoutDurationMs(6))
        assertEquals(120_000L, PinLockoutPolicy.lockoutDurationMs(7))
        assertEquals(15 * 60_000L, PinLockoutPolicy.lockoutDurationMs(50))
    }
}
