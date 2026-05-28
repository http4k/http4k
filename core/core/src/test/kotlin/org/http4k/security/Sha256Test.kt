package org.http4k.security

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.util.Hex
import org.junit.jupiter.api.Test

class Sha256Test {

    @Test
    fun `hashes a string`() {
        assertThat(Sha256.hash("abc"), equalTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"))
    }

    @Test
    fun `hashes a byte array`() {
        assertThat(Sha256.hash("abc".toByteArray()), equalTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"))
    }

    @Test
    fun `hmac matches RFC 4231 test vector`() {
        val key = ByteArray(20) { 0x0b }
        assertThat(
            Hex.hex(Sha256.hmac(key, "Hi There")),
            equalTo("b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7")
        )
    }
}
