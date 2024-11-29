package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.*
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.UriTemplate
import org.http4k.routing.MethodConstraint.Any
import org.http4k.routing.MethodConstraint.Specific
import org.junit.jupiter.api.Test

class NewRoutingTests {

    private val aValidHandler = { _: Request -> Response(OK) }

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
}

// public API
private fun newRoutes(routes: Pair<String, HttpHandler>): HttpHandler = RoutedHttpHandler(
    TemplatedHttpHandler(UriTemplate.from(routes.first), routes.second)
)

private fun newRoutes(routes: Triple<String, Method, HttpHandler>): HttpHandler = RoutedHttpHandler(
    TemplatedHttpHandler(UriTemplate.from(routes.first), routes.third, Specific(routes.second))
)

infix fun String.newBind(pair: Method): Pair<String, Method> = Pair(this, pair)

infix fun Pair<String, Method>.to(handler: HttpHandler): Triple<String, Method, HttpHandler> =
    Triple(this.first, this.second, handler)


// internals
class RoutedHttpHandler(private val templatedHandler: TemplatedHttpHandler) : HttpHandler {
    override fun invoke(request: Request) = templatedHandler.match(request).toHandler()(request)
}

class TemplatedHttpHandler(
    private val uriTemplate: UriTemplate,
    private val handler: HttpHandler,
    private val method: MethodConstraint = Any
) {
    fun match(request: Request): RoutingMatchResult =
        if (uriTemplate.matches(request.uri.path)) {
            if (!method.matches(request))
                RoutingMatchResult.MethodNotMatched
            else
                RoutingMatchResult.Matched(handler)
        } else
            RoutingMatchResult.NotFound
}

sealed class MethodConstraint {
    data object Any : MethodConstraint()
    data class Specific(val method: Method) : MethodConstraint()
}

fun MethodConstraint.matches(request: Request): Boolean = when (this) {
    is Any -> true
    is Specific -> request.method == method
}

sealed class RoutingMatchResult {
    data class Matched(val handler: HttpHandler) : RoutingMatchResult()
    data object MethodNotMatched : RoutingMatchResult()
    data object NotFound : RoutingMatchResult()
}

fun RoutingMatchResult.toHandler() = when (this) {
    is RoutingMatchResult.Matched -> handler
    is RoutingMatchResult.MethodNotMatched -> routeMethodNotAllowedHandler
    is RoutingMatchResult.NotFound -> routeNotFoundHandler
}
