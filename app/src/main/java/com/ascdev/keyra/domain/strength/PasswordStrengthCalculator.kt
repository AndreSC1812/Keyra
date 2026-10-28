package com.ascdev.keyra.domain.strength

import javax.inject.Inject
import kotlin.math.log2

/**
 * Estimates the strength of a password from its character pool entropy, with penalties
 * for the patterns attackers try first: common passwords, repeated characters and sequences.
 */
class PasswordStrengthCalculator @Inject constructor() {

    fun evaluate(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength(StrengthLevel.VERY_WEAK, 0.0)
        if (password.lowercase() in COMMON_PASSWORDS) return PasswordStrength(StrengthLevel.VERY_WEAK, 0.0)

        val entropy = (password.length * log2(poolSize(password).toDouble()) - penalty(password))
            .coerceAtLeast(0.0)

        val level = when {
            entropy < WEAK_BITS -> StrengthLevel.VERY_WEAK
            entropy < FAIR_BITS -> StrengthLevel.WEAK
            entropy < STRONG_BITS -> StrengthLevel.FAIR
            entropy < VERY_STRONG_BITS -> StrengthLevel.STRONG
            else -> StrengthLevel.VERY_STRONG
        }
        return PasswordStrength(level, entropy)
    }

    private fun poolSize(password: String): Int {
        var pool = 0
        if (password.any { it.isLowerCase() }) pool += LETTERS
        if (password.any { it.isUpperCase() }) pool += LETTERS
        if (password.any { it.isDigit() }) pool += DIGITS
        if (password.any { !it.isLetterOrDigit() }) pool += SYMBOLS
        return pool.coerceAtLeast(1)
    }

    private fun penalty(password: String): Double {
        var penalty = 0.0
        password.zipWithNext().forEach { (current, next) ->
            when {
                current == next -> penalty += REPEAT_PENALTY
                next - current == 1 || current - next == 1 -> penalty += SEQUENCE_PENALTY
            }
        }
        return penalty
    }

    private companion object {
        const val WEAK_BITS = 28
        const val FAIR_BITS = 40
        const val STRONG_BITS = 60
        const val VERY_STRONG_BITS = 80

        const val LETTERS = 26
        const val DIGITS = 10
        const val SYMBOLS = 33

        const val REPEAT_PENALTY = 3.0
        const val SEQUENCE_PENALTY = 2.5

        val COMMON_PASSWORDS = setOf(
            "123456", "12345678", "123456789", "1234567890", "password", "password1", "qwerty",
            "qwerty123", "111111", "000000", "abc123", "iloveyou", "admin", "welcome", "letmein",
            "monkey", "dragon", "football", "baseball", "sunshine", "princess", "passw0rd",
            "contraseña", "123123", "654321", "superman", "trustno1",
        )
    }
}
