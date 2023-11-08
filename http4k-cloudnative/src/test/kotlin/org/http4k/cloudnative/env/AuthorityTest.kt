package org.http4k.cloudnative.env

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class AuthorityTest {

    @Test
    fun `can parse from string`() {
        assertThat(Authority("localhost:80"), equalTo(Authority(Host.localhost, Port(80))))
        assertThat(Authority("localhost"), equalTo(Authority(Host.localhost, null)))
        assertThat({ Authority("") }, throws<IllegalArgumentException>())
    }

    @Test
    fun `check toString`() {
        assertThat(Authority(Host.localhost, Port(80)).toString(), equalTo("localhost:80"))
        assertThat(Authority(Host.localhost).toString(), equalTo("localhost"))
    }

    @Test
    fun `as http uri`() {
        assertThat(Authority("localhost:123").asHttpUri(), equalTo(Uri.of("http://localhost:123")))
        assertThat(Authority("localhost").asHttpUri(), equalTo(Uri.of("http://localhost")))
    }

    @Test
    fun `as https uri`() {
        assertThat(Authority("localhost:123").asHttpsUri(), equalTo(Uri.of("https://localhost:123")))
        assertThat(Authority("localhost").asHttpsUri(), equalTo(Uri.of("https://localhost")))
    }
}
