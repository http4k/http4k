package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.UriTemplate
import org.junit.jupiter.api.Test

class NewRoutingTests {

    @Test
    fun `routes a template`() {
        val app: HttpHandler = newRoutes("/foo" to { req: Request -> Response(Status.OK) })

        assertThat(app(Request(Method.GET, "/bar")).status, equalTo(Status.NOT_FOUND))
        assertThat(app(Request(Method.GET, "/foo")).status, equalTo(Status.OK))
        assertThat(app(Request(Method.PUT, "/foo")).status, equalTo(Status.OK))
    }

    private fun newRoutes(routes: Pair<String, HttpHandler>): HttpHandler = RoutedHttpHandler(
        TemplatedHttpHandler(UriTemplate.from(routes.first), routes.second)
    )
}

class RoutedHttpHandler(private val templatedHandler: TemplatedHttpHandler) : HttpHandler {
    override fun invoke(request: Request) = templatedHandler.match(request).toHandler()(request)

}

class TemplatedHttpHandler(private val uriTemplate: UriTemplate, private val handler: HttpHandler) {
    fun match(request: Request): RoutingMatchResult {
        if (uriTemplate.matches(request.uri.path)) {
            return RoutingMatchResult.Matched(handler)
        }
        return RoutingMatchResult.NotFound
    }
}

sealed class RoutingMatchResult {
    data class Matched(val handler: HttpHandler) : RoutingMatchResult()
    object MethodNotMatched : RoutingMatchResult()
    object NotFound : RoutingMatchResult()
}

fun RoutingMatchResult.toHandler() = when(this){
     is RoutingMatchResult.Matched -> handler
     is RoutingMatchResult.MethodNotMatched -> routeMethodNotAllowedHandler
     is RoutingMatchResult.NotFound -> routeNotFoundHandler
 }
