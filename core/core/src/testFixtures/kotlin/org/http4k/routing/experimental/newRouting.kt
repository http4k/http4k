package org.http4k.routing.experimental

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.UriTemplate

 fun newRoutes(vararg list: Pair<Method, HttpHandler>): RoutedHttpHandler =
    newRoutes(*list.map { "" newBind  it.first to it.second }.toTypedArray())

 fun newRoutes(vararg list: RoutedHttpHandler): RoutedHttpHandler = newRoutes(list.toList())

 fun newRoutes(routers: List<RoutedHttpHandler>):RoutedHttpHandler = RoutedHttpHandler(routers.flatMap { it.routes })

 infix fun String.newBind(method: Method) = NewPathMethod(this, method)
 infix fun String.newBind(httpHandler: RoutedHttpHandler): RoutedHttpHandler = httpHandler.withBasePath(this)
 infix fun String.newBind(action: HttpHandler): RoutedHttpHandler =
    RoutedHttpHandler(listOf(TemplatedRoute(UriTemplate.from(this), action)))

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
                    handler.routes.map { it.withPredicate(hostHeaderOrUriHost(host)) }
                else -> listOf(TemplatedRoute(UriTemplate.from(""), handler, hostHeaderOrUriHost(host)))
            }
        }
    )

private fun hostHeaderOrUriHost(host: String): Predicate =
    Predicate("host header or uri host = $host") { req: Request ->
        (req.headerValues("host").firstOrNull() ?: req.uri.authority).contains(host)
    }

fun Method.asPredicate(): Predicate = Predicate("method == $this", notMatchedStatus = METHOD_NOT_ALLOWED) { it.method == this }
