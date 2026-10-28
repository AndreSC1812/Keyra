package com.ascdev.keyra.domain.strength

enum class StrengthLevel {
    VERY_WEAK,
    WEAK,
    FAIR,
    STRONG,
    VERY_STRONG,
    ;

    val isWeak: Boolean get() = this <= WEAK
}

data class PasswordStrength(
    val level: StrengthLevel,
    val entropyBits: Double,
) {
    /** Value between 0 and 1, handy for progress indicators. */
    val progress: Float get() = (level.ordinal + 1) / StrengthLevel.entries.size.toFloat()
}
