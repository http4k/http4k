package org.http4k.backend

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.bind
import org.http4k.routing.hostDemux
import org.http4k.routing.routes
import org.junit.jupiter.api.Test

class HostDemuxRoutingHttpHandlerTest {
    private val handler = hostDemux(hostFor("host1"), hostFor("host2"))

    @Test
    fun `matching handler`() {
        assertThat(handler(requestWithHost("host1", "/foo")), hasBody("host1host1"))
        assertThat(handler(requestWithHost("host2", "/foo")), hasBody("host2host2"))
        assertThat(handler(Request(GET, "")), hasStatus(NOT_FOUND))
    }

    @Test
    fun `with base path`() {
        val handler2 = handler.withBasePath("/bar")
        assertThat(handler2(requestWithHost("host1", "/bar/foo")), hasBody("host1host1"))
        assertThat(handler2(requestWithHost("host2", "/bar/foo")), hasBody("host2host2"))
        assertThat(handler2(requestWithHost("host1", "/foo")), hasStatus(NOT_FOUND))
        assertThat(handler2(Request(GET, "")), hasStatus(NOT_FOUND))
    }

    @Test
    fun `with filter`() {
        val handler2 = handler.withFilter(Filter { next ->
            {
                next(it.replaceHeader("host", "foobar"))
            }
        })
        assertThat(handler2(requestWithHost("host1", "/foo")), hasBody("host1foobar"))
        assertThat(handler2(requestWithHost("host2", "/foo")), hasBody("host2foobar"))
        assertThat(handler2(Request(GET, "")), hasStatus(NOT_FOUND))
    }
}

private fun requestWithHost(host: String, path: String) = Request(GET, path).header("host", host)

private fun hostFor(host: String) = host to routes("/foo" bind GET to { Response(OK).body(host + it.header("host")) })
