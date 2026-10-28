package com.ascdev.keyra.domain.generator

data class GeneratorOptions(
    val length: Int = DEFAULT_LENGTH,
    val uppercase: Boolean = true,
    val lowercase: Boolean = true,
    val digits: Boolean = true,
    val symbols: Boolean = true,
) {
    companion object {
        const val MIN_LENGTH = 8
        const val MAX_LENGTH = 64
        const val DEFAULT_LENGTH = 20
    }
}
