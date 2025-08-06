package org.http4k.powerassert

import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class UriMatchersTest {

    @Test
    fun path() {
        assert(Uri.of("/bob").hasPath("/bob"))
        assert(!Uri.of("/bob").hasPath("bill"))
    }

    @Test
    fun `path regex`() {
        assert(Uri.of("/bob").hasPath(Regex(".*bob")))
        assert(!Uri.of("/bob").hasPath(Regex(".*bill")))
    }

    @Test
    fun authority() {
        assert(Uri.of("http://bob:80").hasAuthority("bob:80"))
        assert(!Uri.of("http://bob:80").hasAuthority("bill:80"))
    }

    @Test
    fun host() {
        assert(Uri.of("http://bob:80").hasHost("bob"))
        assert(!Uri.of("http://bob:80").hasHost("bill"))
    }

    @Test
    fun port() {
        assert(Uri.of("http://bob:80").hasPort(80))
        assert(!Uri.of("http://bob:80").hasPort(81))
    }

    @Test
    fun query() {
        assert(Uri.of("http://bob:80?query=bob").hasQuery("query=bob"))
        assert(!Uri.of("http://bob:80?query=bob").hasQuery("query=bill"))
    }
}