package org.http4k.security.digest

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class ParameterizedHeaderTest {

    @Test
    fun `parses prefix and parameters`() {
        val header = ParameterizedHeader.parse("""Digest realm="r", nonce="n"""")
        assertThat(header.prefix, equalTo("Digest"))
        assertThat(header.parameters, equalTo(mapOf("realm" to "r", "nonce" to "n")))
    }

    @Test
    fun `tolerates value with no parameters at all`() {
        val header = ParameterizedHeader.parse("Digest")
        assertThat(header.prefix, equalTo("Digest"))
        assertThat(header.parameters, equalTo(emptyMap()))
    }

    @Test
    fun `tolerates value with only whitespace after prefix`() {
        val header = ParameterizedHeader.parse("Digest   ")
        assertThat(header.prefix, equalTo("Digest"))
        assertThat(header.parameters, equalTo(emptyMap()))
    }

    @Test
    fun `tolerates empty value`() {
        val header = ParameterizedHeader.parse("")
        assertThat(header.prefix, equalTo(""))
        assertThat(header.parameters, equalTo(emptyMap()))
    }
}
