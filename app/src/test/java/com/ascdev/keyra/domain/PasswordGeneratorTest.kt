package com.ascdev.keyra.domain

import com.ascdev.keyra.domain.generator.GeneratorOptions
import com.ascdev.keyra.domain.generator.PasswordGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class PasswordGeneratorTest {

    private val generator = PasswordGenerator(SecureRandom())

    @Test
    fun `generates the requested length`() {
        listOf(8, 20, 64).forEach { length ->
            assertEquals(length, generator.generate(GeneratorOptions(length = length)).length)
        }
    }

    @Test
    fun `clamps the length to the allowed range`() {
        assertEquals(GeneratorOptions.MIN_LENGTH, generator.generate(GeneratorOptions(length = 2)).length)
        assertEquals(GeneratorOptions.MAX_LENGTH, generator.generate(GeneratorOptions(length = 500)).length)
    }

    @Test
    fun `contains at least one character of every selected set`() {
        repeat(200) {
            val password = generator.generate(GeneratorOptions(length = 8))
            assertTrue(password.any { it in PasswordGenerator.UPPERCASE })
            assertTrue(password.any { it in PasswordGenerator.LOWERCASE })
            assertTrue(password.any { it in PasswordGenerator.DIGITS })
            assertTrue(password.any { it in PasswordGenerator.SYMBOLS })
        }
    }

    @Test
    fun `only uses the selected sets`() {
        val password = generator.generate(
            GeneratorOptions(length = 64, uppercase = false, lowercase = false, digits = true, symbols = false),
        )
        assertTrue(password.all { it.isDigit() })
    }

    @Test
    fun `falls back to lowercase when nothing is selected`() {
        val password = generator.generate(
            GeneratorOptions(uppercase = false, lowercase = false, digits = false, symbols = false),
        )
        assertTrue(password.all { it in PasswordGenerator.LOWERCASE })
    }

    @Test
    fun `does not repeat passwords`() {
        val passwords = List(500) { generator.generate(GeneratorOptions()) }.toSet()
        assertEquals(500, passwords.size)
    }
}
