package org.http4k.strikt

import org.http4k.core.Uri
import org.http4k.strikt.authority
import org.http4k.strikt.host
import org.http4k.strikt.port
import org.http4k.strikt.query
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class UriMatchersTest {

    @Test
    fun matchers() {
        val uri = Uri.of("http://bob:80?query=bob")

        expectThat(uri) {
            query.isEqualTo(uri.query)
            host.isEqualTo(uri.host)
            authority.isEqualTo(uri.authority)
            port.isEqualTo(uri.port)
        }
    }
}
