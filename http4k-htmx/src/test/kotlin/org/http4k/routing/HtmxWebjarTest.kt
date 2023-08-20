package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class HtmxWebjarTest {
    @Test
    fun `can serve htmx`() {
        assertThat(htmxWebjar()(Request(GET, "htmx.js")), hasStatus(OK))
    }
}
