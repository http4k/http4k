package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Query
import org.http4k.lens.int
import org.junit.jupiter.api.Test

class ApiKeyAuthenticationTest {
    @Test
    fun `lens key`() {
        val lens = Query.int().required("foobar")
        val handler = ServerFilters.ApiKeyAuth(lens) { it == 5 }.then { Response(OK) }
        assertThat(handler(Request(GET, "/").query("foobar", "5")), hasStatus(OK))
        assertThat(handler(Request(GET, "/").query("foobar", "asd")), hasStatus(UNAUTHORIZED))
        assertThat(handler(Request(GET, "/")), hasStatus(UNAUTHORIZED))
    }

    @Test
    fun `is success`() {
        val handler = ServerFilters.ApiKeyAuth { true }.then { Response(OK) }
        assertThat(handler(Request(GET, "/")), hasStatus(OK))
    }

    @Test
    fun `is failure`() {
        val handler = ServerFilters.ApiKeyAuth { false }.then { Response(OK) }
        assertThat(handler(Request(GET, "/")), hasStatus(UNAUTHORIZED))
    }

    @Test
    fun `client sets value on request`() {
        val apiKey = "hello"
        val lens = Query.required("foo")

        assertThat((ClientFilters.ApiKeyAuth(lens of apiKey)
            .then(ServerFilters.ApiKeyAuth(lens) { it == apiKey })
            .then { Response(OK) })(Request(GET, "/")), hasStatus(OK))

        assertThat((ClientFilters.ApiKeyAuth(lens of "not hello")
            .then(ServerFilters.ApiKeyAuth(lens) { it == apiKey })
            .then { Response(OK) })(Request(GET, "/")), hasStatus(UNAUTHORIZED))
    }
}
