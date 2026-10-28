package com.ascdev.keyra.domain

import com.ascdev.keyra.domain.pin.PinError
import com.ascdev.keyra.domain.pin.PinValidator
import com.ascdev.keyra.domain.pin.copyChars
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PinValidatorTest {

    @Test
    fun `accepts valid pins`() {
        listOf("482916", "13579246", "907312845561").forEach { assertNull(PinValidator.validate(it)) }
    }

    @Test
    fun `rejects pins with the wrong length`() {
        assertEquals(PinError.TOO_SHORT, PinValidator.validate("4829"))
        assertEquals(PinError.TOO_LONG, PinValidator.validate("4829164829164"))
    }

    @Test
    fun `rejects non numeric pins`() {
        assertEquals(PinError.NOT_NUMERIC, PinValidator.validate("48a916"))
    }

    @Test
    fun `rejects trivial pins`() {
        listOf("000000", "123456", "987654", "111111111").forEach {
            assertEquals(PinError.TOO_SIMPLE, PinValidator.validate(it))
        }
    }

    @Test
    fun `copies digits without converting to string`() {
        assertArrayEquals(charArrayOf('1', '2'), StringBuilder("12").copyChars())
    }
}
