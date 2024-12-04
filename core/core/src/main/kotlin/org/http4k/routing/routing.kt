package org.http4k.routing

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.UriTemplate
import org.http4k.core.queries

fun routes(vararg list: Pair<Method, HttpHandler>) =
    routes(*list.map { "" bind it.first to it.second }.toTypedArray())

fun routes(vararg list: RoutingHttpHandler) = routes(list.toList())

fun routes(routers: List<RoutingHttpHandler>) = RoutingHttpHandler(routers.flatMap { it.routes })

infix fun String.bind(method: Method) = HttpPathMethod(this, method)
infix fun String.bind(httpHandler: RoutingHttpHandler) = httpHandler.withBasePath(this)
infix fun String.bind(action: HttpHandler) =
    RoutingHttpHandler(listOf(TemplatedHttpRoute(UriTemplate.from(this), action)))

infix fun Router.bind(handler: HttpHandler): RoutingHttpHandler =
    RoutingHttpHandler(listOf(SimpleRouteMatcher(this, handler)))

infix fun Router.bind(handler: RoutingHttpHandler): RoutingHttpHandler = handler.withRouter(this)

fun Method.bind(handler: HttpHandler): RoutingHttpHandler = asRouter().bind(handler)

fun RoutingHttpHandler.and(router: Router) = withRouter(router)


fun Method.asRouter() = Router("method == $this", notMatchedStatus = METHOD_NOT_ALLOWED) { it.method == this }

fun Method.and(router: Router) = asRouter().and(router)

/**
 * Apply routing predicate to a query
 */
fun query(name: String, fn: (String) -> Boolean) =
    { req: Request -> req.queries(name).filterNotNull().any(fn) }.asRouter("Query $name matching $fn")

/**
 * Apply routing predicate to a query
 */
fun query(name: String, value: String) = query(name) { it == value }

/**
 * Ensure all queries are present and not null
 */
fun queries(vararg names: String) =
    { req: Request -> names.all { req.query(it) != null } }.asRouter("Queries ${names.toList()}")

/**
 * Ensure query is present - even with no value
 */
fun query(name: String) =
    { req: Request -> req.queries(name).isNotEmpty() }.asRouter()

/**
 * Ensure all queries are present and match what is in the Uri
 */
fun queriesFrom(uri: Uri) = { req: Request -> uri.queries().all { (name, value) -> req.queries(name).contains(value) } }.asRouter()

/**
 * Apply routing predicate to a header
 */
fun header(name: String, fn: (String) -> Boolean) =
    { req: Request -> req.headerValues(name).filterNotNull().any(fn) }.asRouter("Header $name matching $fn")

/**
 * Apply routing predicate to a header
 */
fun header(name: String, value: String) = header(name) { it == value }

/**
 * Ensure all headers are present
 */
fun headers(vararg names: String) =
    { req: Request -> names.all { req.header(it) != null } }.asRouter("Headers ${names.toList()}")

/**
 * Ensure body matches predicate
 */
fun body(fn: (Body) -> Boolean) = { it: Request -> fn(it.body) }.asRouter("Body matching $fn")

/**
 * Ensure body string matches predicate
 */
@JvmName("bodyMatches")
fun body(fn: (String) -> Boolean) = { req: Request -> fn(req.bodyString()) }.asRouter("Body matching $fn")
