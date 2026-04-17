package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class UriSameOriginTest {

    @Test
    fun `identical URIs are same origin`() {
        assertThat(Uri.of("https://example.com").isSameOrigin(Uri.of("https://example.com")), equalTo(true))
    }

    @Test
    fun `same origin with different paths`() {
        assertThat(Uri.of("https://example.com/foo").isSameOrigin(Uri.of("https://example.com/bar")), equalTo(true))
    }

    @Test
    fun `same origin with default port normalized - https`() {
        assertThat(Uri.of("https://example.com").isSameOrigin(Uri.of("https://example.com:443")), equalTo(true))
    }

    @Test
    fun `same origin with default port normalized - http`() {
        assertThat(Uri.of("http://example.com").isSameOrigin(Uri.of("http://example.com:80")), equalTo(true))
    }

    @Test
    fun `different scheme is not same origin`() {
        assertThat(Uri.of("http://example.com").isSameOrigin(Uri.of("https://example.com")), equalTo(false))
    }

    @Test
    fun `different host is not same origin`() {
        assertThat(Uri.of("https://example.com").isSameOrigin(Uri.of("https://evil.com")), equalTo(false))
    }

    @Test
    fun `different port is not same origin`() {
        assertThat(Uri.of("https://example.com:8443").isSameOrigin(Uri.of("https://example.com:9443")), equalTo(false))
    }

    @Test
    fun `non-default port differs from default is not same origin`() {
        assertThat(Uri.of("https://example.com").isSameOrigin(Uri.of("https://example.com:8443")), equalTo(false))
    }

    @Test
    fun `scheme comparison is case insensitive`() {
        assertThat(Uri.of("HTTPS://example.com").isSameOrigin(Uri.of("https://example.com")), equalTo(true))
    }

    @Test
    fun `host comparison is case insensitive`() {
        assertThat(Uri.of("https://Example.COM").isSameOrigin(Uri.of("https://example.com")), equalTo(true))
    }
}
