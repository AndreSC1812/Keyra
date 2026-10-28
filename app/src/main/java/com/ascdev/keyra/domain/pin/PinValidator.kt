package com.ascdev.keyra.domain.pin

enum class PinError { TOO_SHORT, TOO_LONG, NOT_NUMERIC, TOO_SIMPLE }

object PinValidator {
    const val MIN_LENGTH = 6
    const val MAX_LENGTH = 12

    fun validate(pin: CharSequence): PinError? = when {
        pin.any { !it.isDigit() } -> PinError.NOT_NUMERIC
        pin.length < MIN_LENGTH -> PinError.TOO_SHORT
        pin.length > MAX_LENGTH -> PinError.TOO_LONG
        isTooSimple(pin) -> PinError.TOO_SIMPLE
        else -> null
    }

    private fun isTooSimple(pin: CharSequence): Boolean {
        if (pin.toSet().size == 1) return true
        val steps = pin.zipWithNext { a, b -> b - a }.toSet()
        // 123456, 654321 and friends
        return steps == setOf(1) || steps == setOf(-1)
    }
}

/** Copies the typed digits without creating an immutable String that could linger in memory. */
fun StringBuilder.copyChars(): CharArray = CharArray(length, ::get)
