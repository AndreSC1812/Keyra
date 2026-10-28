package com.ascdev.keyra.domain.generator

import java.security.SecureRandom
import javax.inject.Inject

class PasswordGenerator(private val random: SecureRandom) {

    @Inject
    constructor() : this(SecureRandom())

    fun generate(options: GeneratorOptions): String {
        val length = options.length.coerceIn(GeneratorOptions.MIN_LENGTH, GeneratorOptions.MAX_LENGTH)
        val sets = buildList {
            if (options.uppercase) add(UPPERCASE)
            if (options.lowercase) add(LOWERCASE)
            if (options.digits) add(DIGITS)
            if (options.symbols) add(SYMBOLS)
        }.ifEmpty { listOf(LOWERCASE) }

        val pool = sets.joinToString(separator = "")
        // One character from every selected set guarantees the password honours the options.
        val chars = CharArray(length)
        sets.forEachIndexed { index, set -> chars[index] = set.randomChar() }
        for (index in sets.size until length) {
            chars[index] = pool.randomChar()
        }
        chars.shuffle()
        return String(chars).also { chars.fill(' ') }
    }

    private fun String.randomChar(): Char = this[random.nextInt(length)]

    private fun CharArray.shuffle() {
        for (index in lastIndex downTo 1) {
            val swap = random.nextInt(index + 1)
            val tmp = this[index]
            this[index] = this[swap]
            this[swap] = tmp
        }
    }

    companion object {
        const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
        const val DIGITS = "0123456789"
        const val SYMBOLS = "!@#\$%^&*()-_=+[]{};:,.<>?/~"
    }
}
