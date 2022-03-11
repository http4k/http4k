package org.http4k.strikt

import org.http4k.core.Uri
import org.http4k.strikt.authority
import org.http4k.strikt.host
import org.http4k.strikt.port
import org.http4k.strikt.query
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class UriAssertionsTest {

    @Test
    fun assertions() {
        val uri = Uri.of("http://bob:80/bill?query=bob")

        expectThat(uri) {
            query.isEqualTo(uri.query)
            host.isEqualTo(uri.host)
            authority.isEqualTo(uri.authority)
            port.isEqualTo(uri.port)
            path.isEqualTo(uri.path)
        }
    }
}
