package org.http4k.routing

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class NewRoutingTests {

    private val aValidHandler = { request: Request ->
        val body: String =
            when (request) {
                is RoutedRequest -> request.xUriTemplate.toString()
                else -> request.uri.path
            }
        Response(OK).body(body)
    }

    @Test
    fun `routes a template`() {
        val app: HttpHandler = routes("/foo" bind aValidHandler)

        assertThat(app(Request(GET, "/bar")).status, equalTo(NOT_FOUND))
        assertThat(app(Request(GET, "/foo")).status, equalTo(OK))
        assertThat(app(Request(PUT, "/foo")).status, equalTo(OK))
    }

    @Test
    fun `routes a template with method`() {
        val app: HttpHandler = routes("/foo" bind GET to aValidHandler)

        assertThat(app(Request(GET, "/bar")).status, equalTo(NOT_FOUND))
        assertThat(app(Request(GET, "/foo")).status, equalTo(OK))
        assertThat(app(Request(PUT, "/foo")).status, equalTo(METHOD_NOT_ALLOWED))
    }

    @Test
    fun `includes routing info in request`() {
        val app: HttpHandler = routes("/foo/{name}" bind aValidHandler)
        assertThat(app(Request(GET, "/foo/bar")).bodyString(), equalTo("foo/{name}"))
    }

    @Test
    fun `multiple routes`() {
        val app: HttpHandler = routes(
            "/foo" bind GET to aValidHandler,
            "/bar" bind GET to aValidHandler,
        )

        assertThat(app(Request(GET, "/foo")).status, equalTo(OK))
        assertThat(app(Request(GET, "/bar")).status, equalTo(OK))
        assertThat(app(Request(GET, "/baz")).status, equalTo(NOT_FOUND))
    }

    @Test
    fun `nested routes`() {
        val app: HttpHandler = routes(
            "/foo" bind routes(
                "/bar" bind GET to aValidHandler
            )
        )

        assertThat(app(Request(GET, "/foo")).status, equalTo(NOT_FOUND))
        assertThat(app(Request(GET, "/bar")).status, equalTo(NOT_FOUND))
        assertThat(app(Request(GET, "/foo/bar")).status, equalTo(OK))
    }

    @Test
    fun `mix and match`() {
        val routes = routes(
            "/a" bind GET to { Response(OK).body("matched a") },
            "/b/c" bind routes(
                "/d" bind GET to { Response(OK).body("matched b/c/d") },
                "/e" bind routes(
                    "/f" bind GET to { Response(OK).body("matched b/c/e/f") },
                    "/g" bind routes(
                        GET to { _: Request -> Response(OK).body("matched b/c/e/g/GET") },
                        POST to { _: Request -> Response(OK).body("matched b/c/e/g/POST") }
                    )
                ),
                "/" bind GET to { Response(OK).body("matched b/c") }
            )
        )

        assertThat(routes(Request(GET, "/a")).bodyString(), equalTo("matched a"))
        assertThat(routes(Request(GET, "/b/c/d")).bodyString(), equalTo("matched b/c/d"))
        assertThat(routes(Request(GET, "/b/c")).bodyString(), equalTo("matched b/c"))
        assertThat(routes(Request(GET, "/b/c/e/f")).bodyString(), equalTo("matched b/c/e/f"))
        assertThat(routes(Request(GET, "/b/c/e/g")).bodyString(), equalTo("matched b/c/e/g/GET"))
        assertThat(routes(Request(POST, "/b/c/e/g")).bodyString(), equalTo("matched b/c/e/g/POST"))
        assertThat(routes(Request(GET, "/b/c/e/h")).status, equalTo(NOT_FOUND))
        assertThat(routes(Request(GET, "/b")).status, equalTo(NOT_FOUND))
        assertThat(routes(Request(GET, "/b/e")).status, equalTo(NOT_FOUND))

    }

    @Test
    fun `other request predicates`() {
    }

    @Test
    fun `with filter - applies to matching handler`() {
        val handler = "/foo" bind GET to routes("/bar" bind { Response(OK) })
        val filtered = handler.withFilter(filterAppending("bar"))
        val criteria = hasStatus(OK) and hasHeader("res-header", "bar")
        val request = Request(GET, "/foo/bar").header("host", "host")

        assertThat(filtered(request), criteria)
    }

    @Test
    fun `with filter - applies when not found`() {
        val handler = "/foo" bind GET to routes("/bar" bind { Response(OK) })
        val filtered = handler.withFilter(filterAppending("foo"))
        val request = Request(GET, "/not-found").header("host", "host")

        assertThat(
            filtered(request),
            hasStatus(NOT_FOUND) and hasHeader("res-header", "foo") and hasBody("")
        )
    }

    @Test
    fun `stacked filter application - applies when not found`() {
        val handler = "/foo" bind GET to routes("/bar" bind { Response(OK) })
        val filtered = filterAppending("foo").then(routes(handler))
        val request = Request(GET, "/not-found").header("host", "host")

        assertThat(
            filtered(request),
            hasStatus(NOT_FOUND) and hasHeader("res-header", "foo") and hasBody("")
        )
    }

    @Test
    fun `with filter - applies in correct order`() {
        val handler = "/foo" bind GET to routes("/bar" bind { Response(OK) })
        val filtered = handler.withFilter(filterAppending("foo")).withFilter(filterAppending("bar"))
        val request = Request(GET, "/not-found").header("host", "host")

        assertThat(filtered(request), hasStatus(NOT_FOUND) and hasHeader("res-header", "foobar"))
    }

    private fun filterAppending(value: String) = Filter { next ->
        {
            val response = next(it)
            response.replaceHeader("res-header", response.header("res-header").orEmpty() + value)
        }
    }

    @Test
    fun `binding to static resources`() {

    }

    @Test
    fun `reverse proxy`() {
        val otherHandler = reverseProxyRouting(
            "host1" to routes("/foo" bind GET to { Response(OK).body("host1" + it.header("host")) }),
            "host2" to routes("/foo" bind GET to { Response(OK).body("host2" + it.header("host")) })
        )

        assertThat(otherHandler(requestWithHost("host1", "/foo")), hasBody("host1host1"))
        assertThat(otherHandler(requestWithHost("host1", "http://host2/foo")), hasBody("host1host1"))
        assertThat(otherHandler(requestWithHost("host2", "/foo")), hasBody("host2host2"))
        assertThat(otherHandler(Request(GET, "http://host2/foo")), hasBody("host2null"))
        assertThat(otherHandler(Request(GET, "")), hasStatus(NOT_FOUND))
    }

    private fun requestWithHost(host: String, path: String) = Request(GET, path).header("host", host)

    @Test
    fun `single page apps`() {

    }

    @Test
    fun `works in contracts`() {

    }

    @Test
    fun `binding to sse handlers`() {

    }

    @Test
    fun `nice descriptions`() {
        val routes = routes(
            "/a" bind GET to { Response(OK).body("matched a") },
            "/b/c" bind routes(
                "/d" bind GET to { Response(OK).body("matched b/c/d") },
                "/e" bind routes(
                    "/f" bind GET to { Response(OK).body("matched b/c/e/f") },
                    "/g" bind routes(
                        GET to { _: Request -> Response(OK).body("matched b/c/e/g/GET") },
                        POST to { _: Request -> Response(OK).body("matched b/c/e/g/POST") }
                    )
                ),
                "/" bind GET to { Response(OK).body("matched b/c") }
            )
        )
    }
}
