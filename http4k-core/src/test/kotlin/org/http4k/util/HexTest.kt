package org.http4k.util

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class HexTest {

    @Test
    fun `converts byte array to hex`() {
        assertThat(Hex.hex(ByteArray(10) { (it * 2).toByte() }), equalTo("00020406080a0c0e1012"))
    }
}
