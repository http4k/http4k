package org.http4k.hamkrest

import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class UriMatchersTest {

    @Test
    fun `path`() = assertMatchAndNonMatch(Uri.of("/bob"), hasUriPath("/bob"), hasUriPath("bill"))

    @Test
    fun `path regex`() = assertMatchAndNonMatch(Uri.of("/bob"), hasUriPath(Regex(".*bob")), hasUriPath(Regex(".*bill")))

    @Test
    fun `authority`() = assertMatchAndNonMatch(Uri.of("http://bob:80"), hasAuthority("bob:80"), hasAuthority("bill:80"))

    @Test
    fun `host`() = assertMatchAndNonMatch(Uri.of("http://bob:80"), hasHost("bob"), hasHost("bill"))

    @Test
    fun `port`() = assertMatchAndNonMatch(Uri.of("http://bob:80"), hasPort(80), hasPort(81))

    @Test
    fun `query`() = assertMatchAndNonMatch(Uri.of("http://bob:80?query=bob"), hasUriQuery("query=bob"), hasUriQuery("query=bill"))
}