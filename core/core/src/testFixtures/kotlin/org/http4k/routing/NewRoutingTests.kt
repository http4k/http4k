package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.routing.MethodConstraint.Any
import org.http4k.routing.MethodConstraint.Specific
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
        val app: HttpHandler = newRoutes("/foo" to aValidHandler)

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
        val app: HttpHandler = newRoutes("/foo/{name}" to aValidHandler)
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
    }

}

// public API
private fun newRoutes(routedHttpHandler: RoutedHttpHandler): RoutedHttpHandler = routedHttpHandler

private fun newRoutes(vararg routes: Pair<String, HttpHandler>): RoutedHttpHandler = RoutedHttpHandler(
    routes.map { (template, handler) ->
        TemplatedHttpHandler(UriTemplate.from(template), handler)
    }
)

private fun newRoutes(vararg routes: Triple<String, Method, HttpHandler>): RoutedHttpHandler = RoutedHttpHandler(
    routes.map { (template, method, handler) ->
        TemplatedHttpHandler(UriTemplate.from(template), handler, Specific(method))
    }
)

private infix fun String.newBind(newRoutes: RoutedHttpHandler): RoutedHttpHandler = newRoutes.withBasePath(this)

infix fun String.newBind(pair: Method): Pair<String, Method> = Pair(this, pair)

infix fun Pair<String, Method>.to(handler: HttpHandler): Triple<String, Method, HttpHandler> =
    Triple(this.first, this.second, handler)

// internals
data class RoutedHttpHandler(val templates: List<TemplatedHttpHandler>) : HttpHandler {
    override fun invoke(request: Request) = templates
        .map { it.match(request) }
        .sortedBy(RoutingMatchResult::priority)
        .first()
        .toHandler()(request)
}

private fun RoutedHttpHandler.withBasePath(prefix: String): RoutedHttpHandler {
    return copy(templates = templates.map { it.withBasePath(prefix) })
}

data class TemplatedHttpHandler(
    val uriTemplate: UriTemplate,
    val handler: HttpHandler,
    val method: MethodConstraint = Any
) {
    fun match(request: Request): RoutingMatchResult =
        if (uriTemplate.matches(request.uri.path)) {
            if (!method.matches(request))
                RoutingMatchResult.MethodNotMatched
            else
                RoutingMatchResult.Matched(AddUriTemplate(uriTemplate).then(handler))
        } else
            RoutingMatchResult.NotFound
}


private fun TemplatedHttpHandler.withBasePath(prefix: String): TemplatedHttpHandler {
    return copy(uriTemplate = UriTemplate.from("$prefix/${uriTemplate}"))
}

sealed class MethodConstraint {
    data object Any : MethodConstraint()
    data class Specific(val method: Method) : MethodConstraint()
}

fun MethodConstraint.matches(request: Request): Boolean = when (this) {
    is Any -> true
    is Specific -> request.method == method
}

sealed class RoutingMatchResult(val priority: Int) {
    data class Matched(val handler: HttpHandler) : RoutingMatchResult(0)
    data object MethodNotMatched : RoutingMatchResult(1)
    data object NotFound : RoutingMatchResult(2)
}

fun RoutingMatchResult.toHandler() = when (this) {
    is RoutingMatchResult.Matched -> handler
    is RoutingMatchResult.MethodNotMatched -> routeMethodNotAllowedHandler
    is RoutingMatchResult.NotFound -> routeNotFoundHandler
}

fun AddUriTemplate(uriTemplate: UriTemplate) = Filter { next ->
    {
        next(RoutedRequest(it, uriTemplate))
    }
}
