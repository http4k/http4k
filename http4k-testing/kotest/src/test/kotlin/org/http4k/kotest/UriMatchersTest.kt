package org.http4k.kotest

import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class UriMatchersTest {

    @Test
    fun path() = assertMatchAndNonMatch(
        Uri.of("/bob"),
        { shouldHavePath("/bob") },
        { shouldHavePath("bill") }
    )

    @Test
    fun `path regex`() = assertMatchAndNonMatch(
        Uri.of("/bob"),
        { shouldHavePath(Regex(".*bob")) },
        { shouldHavePath(Regex(".*bill")) }
    )

    @Test
    fun authority() = assertMatchAndNonMatch(
        Uri.of("http://bob:80"),
        { shouldHaveAuthority("bob:80") },
        { shouldHaveAuthority("bill:80") }
    )

    @Test
    fun host() = assertMatchAndNonMatch(
        Uri.of("http://bob:80"),
        { shouldHaveHost("bob") },
        { shouldHaveHost("bill") }
    )

    @Test
    fun port() = assertMatchAndNonMatch(
        Uri.of("http://bob:80"),
        { shouldHavePort(80) },
        { shouldHavePort(81) }
    )

    @Test
    fun query() =
        assertMatchAndNonMatch(
            Uri.of("http://bob:80?query=bob"),
            { shouldHaveQuery("query=bob") },
            { shouldHaveQuery("query=bill") }
        )
}
