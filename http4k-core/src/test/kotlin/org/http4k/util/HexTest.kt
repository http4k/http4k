package org.http4k.util

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.MatchResult.Match
import com.natpryce.hamkrest.MatchResult.Mismatch
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.describe
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class HexTest {

    @Test
    fun `converts byte array to hex`() {
        assertThat(Hex.hex(ByteArray(10) { (it * 2).toByte() }), equalTo("00020406080a0c0e1012"))
    }

    @Test
    fun `converts hex to byte array`() {
        val expected = ByteBuffer.wrap("http4k".toByteArray(StandardCharsets.UTF_8))
        assertThat(ByteBuffer.wrap(Hex.unhex("68747470346B")), equalTo(expected))
    }

    @Test
    fun `checks for even input length`() {
        val exception = assertThrows<IllegalStateException> { Hex.unhex("4") }
        assertThat(exception.message, equalTo("Must have an even length"))
    }

    @Test
    internal fun `converts to hex and back again`() {
        val data = "some random text".toByteArray(StandardCharsets.UTF_8)
        assertThat(Hex.unhex(Hex.hex(data)), contentEquals(data))
    }

    @Test
    internal fun `converts from hex string and back again`() {
        val data = "00020590feffab"
        assertThat(Hex.hex(Hex.unhex(data)), equalTo(data))
    }

}

private fun contentEquals(expected: ByteArray?): Matcher<ByteArray?> =
    object : Matcher<ByteArray?> {
        override fun invoke(actual: ByteArray?): MatchResult =
            if (actual contentEquals expected) Match else Mismatch("was: ${describe(actual.contentToString())}")

        override val description: String get() = "has equal content to ${describe(expected.contentToString())}"
        override val negatedDescription: String get() = "has not equal content to ${describe(expected.contentToString())}"
    }
