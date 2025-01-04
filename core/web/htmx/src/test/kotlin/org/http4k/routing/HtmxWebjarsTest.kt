package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.ClientFilters.FollowRedirects
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class HtmxWebjarsTest {

    private val app = FollowRedirects().then(htmxWebjars())

    @Test
    fun `can serve htmx`() {
        assertThat(app(Request(GET, "htmx.js")), hasStatus(OK))
        assertThat(app(Request(GET, "htmx.min.js")), hasStatus(OK))
    }

    @Test
    fun `can serve hyperscript (with redirects)`() {
        assertThat(app(Request(GET, "hyperscript.js")), hasStatus(OK))
        assertThat(app(Request(GET, "hyperscript.min.js")), hasStatus(OK))

        assertThat(app(Request(GET, "_hyperscript.js")), hasStatus(OK))
        assertThat(app(Request(GET, "_hyperscript.min.js")), hasStatus(OK))
    }
}
