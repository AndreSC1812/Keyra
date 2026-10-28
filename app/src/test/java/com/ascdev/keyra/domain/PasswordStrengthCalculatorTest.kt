package com.ascdev.keyra.domain

import com.ascdev.keyra.domain.generator.GeneratorOptions
import com.ascdev.keyra.domain.generator.PasswordGenerator
import com.ascdev.keyra.domain.strength.PasswordStrengthCalculator
import com.ascdev.keyra.domain.strength.StrengthLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordStrengthCalculatorTest {

    private val calculator = PasswordStrengthCalculator()

    @Test
    fun `empty and common passwords are very weak`() {
        listOf("", "123456", "password", "Qwerty", "letmein").forEach {
            assertEquals(StrengthLevel.VERY_WEAK, calculator.evaluate(it).level)
        }
    }

    @Test
    fun `short lowercase passwords are weak`() {
        assertTrue(calculator.evaluate("sunsets").level.isWeak)
    }

    @Test
    fun `repeated characters and sequences are penalised`() {
        val repeated = calculator.evaluate("aaaaaaaaaaaa")
        val random = calculator.evaluate("qhzmvtrkwpxe")
        assertTrue(repeated.entropyBits < random.entropyBits)
        assertTrue(calculator.evaluate("abcdefghijkl").entropyBits < random.entropyBits)
    }

    @Test
    fun `long mixed passwords are very strong`() {
        assertEquals(StrengthLevel.VERY_STRONG, calculator.evaluate("k8#Rt!vQ2@pLm9\$wZx4").level)
    }

    @Test
    fun `generated passwords with default options are never weak`() {
        val generator = PasswordGenerator()
        repeat(100) {
            val level = calculator.evaluate(generator.generate(GeneratorOptions())).level
            assertTrue(level >= StrengthLevel.STRONG)
        }
    }

    @Test
    fun `progress grows with the level`() {
        val weak = calculator.evaluate("abc").progress
        val strong = calculator.evaluate("k8#Rt!vQ2@pLm9\$wZx4").progress
        assertTrue(weak < strong)
        assertEquals(1f, strong)
    }
}
