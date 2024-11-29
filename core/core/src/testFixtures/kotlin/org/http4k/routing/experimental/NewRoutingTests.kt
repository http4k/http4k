package org.http4k.routing.experimental

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.RoutedRequest
import org.http4k.routing.RoutedResponse
import org.http4k.routing.routeMethodNotAllowedHandler
import org.http4k.routing.routeNotFoundHandler
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
        val app: HttpHandler = newRoutes("/foo" newBind aValidHandler)

        assertThat(app(Request(GET, "/bar")).status, equalTo(NOT_FOUND))
        assertThat(app(Request(GET, "/foo")).status, equalTo(OK))
        assertThat(app(Request(PUT, "/foo")).status, equalTo(OK))
    }

    @Test
    fun `routes a template with method`() {
        val app: HttpHandler = newRoutes("/foo" newBind GET to aValidHandler)

        assertThat(app(Request(GET, "/bar")).status, equalTo(NOT_FOUND))
        assertThat(app(Request(GET, "/foo")).status, equalTo(OK))
        assertThat(app(Request(PUT, "/foo")).status, equalTo(METHOD_NOT_ALLOWED))
    }

    @Test
    fun `includes routing info in request`() {
        val app: HttpHandler = newRoutes("/foo/{name}" newBind aValidHandler)
        assertThat(app(Request(GET, "/foo/bar")).bodyString(), equalTo("foo/{name}"))
    }

    @Test
    fun `multiple routes`() {
        val app: HttpHandler = newRoutes(
            "/foo" newBind GET to aValidHandler,
            "/bar" newBind GET to aValidHandler,
        )

        assertThat(app(Request(GET, "/foo")).status, equalTo(OK))
        assertThat(app(Request(GET, "/bar")).status, equalTo(OK))
        assertThat(app(Request(GET, "/baz")).status, equalTo(NOT_FOUND))
    }

    @Test
    fun `nested routes`() {
        val app: HttpHandler = newRoutes(
            "/foo" newBind newRoutes(
                "/bar" newBind GET to aValidHandler
            )
        )

        assertThat(app(Request(GET, "/foo")).status, equalTo(NOT_FOUND))
        assertThat(app(Request(GET, "/bar")).status, equalTo(NOT_FOUND))
        assertThat(app(Request(GET, "/foo/bar")).status, equalTo(OK))
    }

    @Test
    fun `mix and match`() {
        val routes = newRoutes(
            "/a" newBind GET to { Response(OK).body("matched a") },
            "/b/c" newBind newRoutes(
                "/d" newBind GET to { Response(OK).body("matched b/c/d") },
                "/e" newBind newRoutes(
                    "/f" newBind GET to { Response(OK).body("matched b/c/e/f") },
                    "/g" newBind newRoutes(
                        GET to { _: Request -> Response(OK).body("matched b/c/e/g/GET") },
                        POST to { _: Request -> Response(OK).body("matched b/c/e/g/POST") }
                    )
                ),
                "/" newBind GET to { Response(OK).body("matched b/c") }
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
        val handler = "/foo" newBind GET to newRoutes("/bar" newBind { Response(OK) })
        val filtered = handler.withFilter(filterAppending("bar"))
        val criteria = hasStatus(OK) and hasHeader("res-header", "bar")
        val request = Request(GET, "/foo/bar").header("host", "host")

        assertThat(filtered(request), criteria)
    }

    @Test
    fun `with filter - applies when not found`() {
        val handler = "/foo" newBind GET to newRoutes("/bar" newBind { Response(OK) })
        val filtered = handler.withFilter(filterAppending("foo"))
        val request = Request(GET, "/not-found").header("host", "host")

        assertThat(
            filtered(request),
            hasStatus(NOT_FOUND) and hasHeader("res-header", "foo") and hasBody("")
        )
    }
//
//    @Test
//    open fun `stacked filter application - applies when not found`() {
//        val filtered = filterAppending("foo").then(routes(handler))
//        val request = Request(GET, "/not-found").header("host", "host")
//
//        assertThat(filtered.matchAndInvoke(request), absent())
//        assertThat(
//            filtered(request),
//            hasStatus(NOT_FOUND) and hasHeader("res-header", "foo") and hasBody(expectedNotFoundBody)
//        )
//    }
//
//    @Test
//    open fun `with filter - applies in correct order`() {
//        val filtered = handler.withFilter(filterAppending("foo")).withFilter(filterAppending("bar"))
//        val request = Request(GET, "/not-found").header("host", "host")
//
//        assertThat(filtered.matchAndInvoke(request), absent())
//        assertThat(filtered(request), hasStatus(NOT_FOUND) and hasHeader("res-header", "foobar"))
//    }

    fun filterAppending(value: String) = Filter { next ->
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
        val otherHandler = newReverseProxyRouting(
            "host1" to newRoutes("/foo" newBind GET to { Response(OK).body("host1" + it.header("host")) }),
            "host2" to newRoutes("/foo" newBind GET to { Response(OK).body("host2" + it.header("host")) })
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
        val routes = newRoutes(
            "/a" newBind GET to { Response(OK).body("matched a") },
            "/b/c" newBind newRoutes(
                "/d" newBind GET to { Response(OK).body("matched b/c/d") },
                "/e" newBind newRoutes(
                    "/f" newBind GET to { Response(OK).body("matched b/c/e/f") },
                    "/g" newBind newRoutes(
                        GET to { _: Request -> Response(OK).body("matched b/c/e/g/GET") },
                        POST to { _: Request -> Response(OK).body("matched b/c/e/g/POST") }
                    )
                ),
                "/" newBind GET to { Response(OK).body("matched b/c") }
            )
        )


    }
}


// public API
private fun newRoutes(vararg routed: RoutedHttpHandler): RoutedHttpHandler =
    RoutedHttpHandler(routed.flatMap { it.routes })

private infix fun String.newBind(newRoutes: RoutedHttpHandler): RoutedHttpHandler = newRoutes.withBasePath(this)

private infix fun String.newBind(handler: HttpHandler): RoutedHttpHandler =
    RoutedHttpHandler(listOf(TemplatedRoute(UriTemplate.from(this), handler)))

infix fun String.newBind(method: Method) = NewPathMethod(this, method)

infix fun Pair<String, Method>.to(handler: HttpHandler): RoutedHttpHandler = NewPathMethod(first, second) to handler

infix fun Method.to(httpHandler: HttpHandler): RoutedHttpHandler =
    RoutedHttpHandler(listOf(TemplatedRoute(UriTemplate.from(""), httpHandler, asPredicate())))

/**
 * Simple Reverse Proxy which will split and direct traffic to the appropriate
 * HttpHandler based on the content of the Host header
 */
fun newReverseProxy(vararg hostToHandler: Pair<String, HttpHandler>): HttpHandler =
    newReverseProxyRouting(*hostToHandler)

/**
 * Simple Reverse Proxy. Exposes routing.
 */
fun newReverseProxyRouting(vararg hostToHandler: Pair<String, HttpHandler>): RoutedHttpHandler =
    RoutedHttpHandler(
        hostToHandler.flatMap { (host, handler) ->
            when (handler) {
                is RoutedHttpHandler ->
                    handler.routes.map { it.copy(predicate = it.predicate.and(hostHeaderOrUriHost(host))) }

                else -> listOf(TemplatedRoute(UriTemplate.from(""), handler, hostHeaderOrUriHost(host)))
            }
        }
    )


// internals
data class RoutedHttpHandler(
    val routes: List<TemplatedRoute>,
    private val routeNotFound: HttpHandler = routeNotFoundHandler,
    private val routeMethodNotAllowed: HttpHandler = routeMethodNotAllowedHandler
) : HttpHandler {
    init {
        require(routeNotFound !is RoutedHttpHandler)
        require(routeMethodNotAllowed !is RoutedHttpHandler)
    }

    override fun invoke(request: Request) = routes
        .map { it.match(request) }
        .sortedBy(RoutingMatchResult::priority)
        .first()
        .toHandler()(request)

    fun withBasePath(prefix: String): RoutedHttpHandler = copy(routes = routes.map { it.withBasePath(prefix) })

    fun withFilter(filter: Filter): RoutedHttpHandler = copy(
        routes = routes.map { it.withFilter(filter) },
        routeNotFound = filter.then(routeNotFound),
        routeMethodNotAllowed = filter.then(routeMethodNotAllowed)
    )

    fun withPredicate(predicate: Predicate): RoutedHttpHandler =
        copy(routes = routes.map { it.withPredicate(predicate) })

    override fun toString(): String = routes.sortedBy(TemplatedRoute::toString).joinToString("\n")

    private fun RoutingMatchResult.toHandler() =
        when (this) {
            is RoutingMatchResult.Matched -> handler
            is RoutingMatchResult.MethodNotMatched -> routeMethodNotAllowed
            is RoutingMatchResult.NotFound -> routeNotFound
        }
}

data class TemplatedRoute(
    private val uriTemplate: UriTemplate,
    private val handler: HttpHandler,
    val predicate: Predicate = Any
) {
    init {
        require(handler !is RoutedHttpHandler)
    }

    fun match(request: Request): RoutingMatchResult =
        if (uriTemplate.matches(request.uri.path)) {
            if (!predicate(request))
                RoutingMatchResult.MethodNotMatched
            else
                RoutingMatchResult.Matched(AddUriTemplate(uriTemplate).then(handler))
        } else
            RoutingMatchResult.NotFound

    fun withBasePath(prefix: String): TemplatedRoute = copy(uriTemplate = UriTemplate.from("$prefix/${uriTemplate}"))

    fun withFilter(filter: Filter): TemplatedRoute = copy(handler = filter.then(handler))

    fun withPredicate(other: Predicate): TemplatedRoute = copy(predicate = predicate.and(other))

    override fun toString(): String = "template=$uriTemplate AND ${predicate.description}"

    private fun AddUriTemplate(uriTemplate: UriTemplate) = Filter { next ->
        {
            RoutedResponse(next(RoutedRequest(it, uriTemplate)), uriTemplate)
        }
    }


}

interface Predicate {
    val description: String
    operator fun invoke(request: Request): Boolean

    companion object {
        operator fun invoke(description: String = "", predicate: (Request) -> Boolean) = object : Predicate {
            override val description: String = description
            override fun invoke(request: Request): Boolean = predicate(request)
            override fun toString(): String = description
        }
    }
}

val Any: Predicate = Predicate("any") { true }
fun Method.asPredicate(): Predicate = Predicate("method == $this") { it.method == this }
fun Predicate.and(other: Predicate): Predicate = Predicate("($this AND $other)") { this(it) && other(it) }
fun Predicate.or(other: Predicate): Predicate = Predicate("($this OR $other)") { this(it) || other(it) }
fun Predicate.not(): Predicate = Predicate("NOT $this") { !this(it) }

private fun hostHeaderOrUriHost(host: String): Predicate =
    Predicate("host header or uri host = $host") { req: Request ->
        (req.headerValues("host").firstOrNull() ?: req.uri.authority).let { it.contains(host) }
    }

sealed class RoutingMatchResult(val priority: Int) {
    data class Matched(val handler: HttpHandler) : RoutingMatchResult(0)
    data object MethodNotMatched : RoutingMatchResult(1)
    data object NotFound : RoutingMatchResult(2)
}

data class NewPathMethod(val path: String, val method: Method) {
    infix fun to(handler: HttpHandler) =
        when (handler) {
            is RoutedHttpHandler ->
                handler.withPredicate(method.asPredicate()).withBasePath(path)
            else -> RoutedHttpHandler(listOf(TemplatedRoute(UriTemplate.from(path), handler, method.asPredicate())))
        }
}
