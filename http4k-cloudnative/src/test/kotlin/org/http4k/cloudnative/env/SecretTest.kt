package org.http4k.cloudnative.env

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class SecretTest {

    private val aSecret = Secret("mySecret")

    @Test
    fun equality() {
        assertThat(Secret("mySecret".toByteArray()), equalTo(aSecret))
        assertThat(aSecret.hashCode(), equalTo(aSecret.hashCode()))
    }

    @Test
    fun `string value`() {
        assertThat(aSecret.stringValue(), equalTo("mySecret"))
    }

    @Test
    fun `toString value doesn't reveal value`() {
        assertThat(aSecret.toString(), equalTo("Secret(hashcode = 1666631293)"))
    }

    @Test
    fun `can clear the value`() {
        assertThat(Secret("mySecret").clear().stringValue(), equalTo("\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"))
    }
}

