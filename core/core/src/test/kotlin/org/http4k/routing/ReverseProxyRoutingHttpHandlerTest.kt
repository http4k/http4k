package org.http4k.routing

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class ReverseProxyHandlerTest {
    private val handler = reverseProxy("host" to { Response(OK) })

    @Test
    fun `applies filter before routing`() {
        val app = Filter { next ->
            {
                next(it.header("host", "host1"))
            }
        }.then(handler)
        assertThat(app(Request(GET, "")), hasStatus(OK))
    }
}

class ReverseProxyRoutingHttpHandlerTest : RoutingHttpHandlerContract() {
    override val handler = reverseProxyRouting(
        "host" to routes(validPath bind GET to { Response(OK) }),
        "anotherHost" to routes(validPath bind GET to { Response(OK) }
        )
    )

    private val otherHandler = reverseProxyRouting(hostFor("host1"), hostFor("host2"))

    @Test
    override fun `does not match a particular route`() {
        assertThat(handler.matchAndInvoke(Request(GET, "/not-found").header("host", "unknown")), absent())
        assertThat(handler.matchAndInvoke(Request(GET, "/not-found").header("host", "host"))!!, hasStatus(NOT_FOUND))
        assertThat(
            handler(Request(GET, "/not-found").header("host", "host")),
            hasStatus(NOT_FOUND) and hasBody(expectedNotFoundBody)
        )
    }

    @Test
    fun `matching handler`() {
        assertThat(otherHandler(requestWithHost("host1", "/foo")), hasBody("host1host1"))
        assertThat(otherHandler(requestWithHost("host1", "http://host2/foo")), hasBody("host1host1"))
        assertThat(otherHandler(requestWithHost("host2", "/foo")), hasBody("host2host2"))
        assertThat(otherHandler(Request(GET, "http://host2/foo")), hasBody("host2null"))
        assertThat(otherHandler(Request(GET, "")), hasStatus(NOT_FOUND))
    }

    @Test
    fun `with base path`() {
        val handler2 = otherHandler.withBasePath("/bar")
        assertThat(handler2(requestWithHost("host1", "/bar/foo")), hasBody("host1host1"))
        assertThat(handler2(requestWithHost("host2", "/bar/foo")), hasBody("host2host2"))
        assertThat(handler2(requestWithHost("host1", "/foo")), hasStatus(NOT_FOUND))
        assertThat(handler2(Request(GET, "")), hasStatus(NOT_FOUND))
    }

    @Test
    fun `with filter`() {
        val handler2 = otherHandler.withFilter { next ->
            {
                next(it.replaceHeader("host", "foobar"))
            }
        }
        assertThat(handler2(requestWithHost("host1", "/foo")), hasBody("host1foobar"))
        assertThat(handler2(requestWithHost("host2", "/foo")), hasBody("host2foobar"))
        assertThat(handler2(Request(GET, "")), hasStatus(NOT_FOUND))
    }

    @Test
    fun `applies filter after routing`() {
        val app = Filter { next ->
            {
                next(it.replaceHeader("host", "host2"))
            }
        }.then(otherHandler)
        assertThat(app(requestWithHost("host1", "/foo")), hasBody("host1host2"))
    }

}

private fun requestWithHost(host: String, path: String) = Request(GET, path).header("host", host)

private fun hostFor(host: String) = host to routes("/foo" bind GET to { Response(OK).body(host + it.header("host")) })
