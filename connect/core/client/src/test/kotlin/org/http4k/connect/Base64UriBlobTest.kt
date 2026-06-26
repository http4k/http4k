package org.http4k.connect

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.model.Base64UriBlob
import org.junit.jupiter.api.Test
import kotlin.random.Random

class Base64UriBlobTest {

    @Test
    fun `encode decode string`() {
        val encoded = Base64UriBlob.encode("hello")
        assertThat(encoded.decoded(), equalTo("hello"))
    }

    @Test
    fun `round trips arbitrary bytes`() {
        val message = Random(0).nextBytes(256)
        val encoded = Base64UriBlob.encode(message)
        assertThat(encoded.decodedBytes().contentEquals(message), equalTo(true))
    }

    @Test
    fun `is url-safe and unpadded`() {
        // bytes that force '+' and '/' under standard base64
        val encoded = Base64UriBlob.encode(byteArrayOf(-1, -17, -2)).value
        assertThat(encoded.any { it == '+' || it == '/' || it == '=' }, equalTo(false))
    }
}
