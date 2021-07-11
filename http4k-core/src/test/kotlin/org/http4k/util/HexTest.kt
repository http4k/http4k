package org.http4k.util

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
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
}
