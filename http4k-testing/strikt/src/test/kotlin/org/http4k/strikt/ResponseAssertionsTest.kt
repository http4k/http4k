package org.http4k.strikt

import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ResponseAssertionsTest {

    @Test
    fun assertions() {
        val resp = Response(OK)
            .header("h1", "h2")
            .header("h3", "h4")
            .body("{}")

        expectThat(resp) {
            status.isEqualTo(resp.status)
            header("h1").isEqualTo(resp.header("h1"))
            headerValues("h1").isEqualTo(resp.headerValues("h1"))
            body.isEqualTo(resp.body)
            bodyString.isEqualTo(resp.bodyString())
            jsonBody(Jackson).isEqualTo(Jackson.parse(resp.bodyString()))
        }
    }
}
