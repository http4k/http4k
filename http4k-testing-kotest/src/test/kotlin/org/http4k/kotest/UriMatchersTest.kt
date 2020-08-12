package org.http4k.kotest

import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class UriMatchersTest {

    @Test
    fun `path`() = assertMatchAndNonMatch(Uri.of("/bob"), havePath("/bob"), havePath("bill"))

    @Test
    fun `path regex`() = assertMatchAndNonMatch(Uri.of("/bob"), havePath(Regex(".*bob")), havePath(Regex(".*bill")))

    @Test
    fun `authority`() = assertMatchAndNonMatch(Uri.of("http://bob:80"), haveAuthority("bob:80"), haveAuthority("bill:80"))

    @Test
    fun `host`() = assertMatchAndNonMatch(Uri.of("http://bob:80"), haveHost("bob"), haveHost("bill"))

    @Test
    fun `port`() = assertMatchAndNonMatch(Uri.of("http://bob:80"), havePort(80), havePort(81))

    @Test
    fun `query`() = assertMatchAndNonMatch(Uri.of("http://bob:80?query=bob"), haveQuery("query=bob"), haveQuery("query=bill"))
}
