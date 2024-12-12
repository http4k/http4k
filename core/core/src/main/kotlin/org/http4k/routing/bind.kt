package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.UriTemplate

infix fun String.bind(method: Method) = HttpPathMethod(this, method)

infix fun String.bind(httpHandler: RoutingHttpHandler) = httpHandler.withBasePath(this)
infix fun String.bind(action: HttpHandler) =
    RoutingHttpHandler(listOf(TemplatedHttpRoute(UriTemplate.from(this), action)))

infix fun Router.bind(handler: HttpHandler): RoutingHttpHandler =
    RoutingHttpHandler(listOf(SimpleRouteMatcher(this, handler)))

infix fun Router.bind(handler: RoutingHttpHandler): RoutingHttpHandler = handler.withRouter(this)

infix fun Method.bind(handler: HttpHandler): RoutingHttpHandler = asRouter().bind(handler)

