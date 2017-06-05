package org.http4k.routing


import org.http4k.contract.BasePath
import org.http4k.contract.ContractRenderer
import org.http4k.contract.NoRenderer
import org.http4k.contract.NoSecurity
import org.http4k.contract.Root
import org.http4k.contract.Security
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.lens.BodyLens
import org.http4k.lens.HeaderLens
import org.http4k.lens.PathLens
import org.http4k.lens.QueryLens
import org.http4k.routing.ContractRoutingHttpHandler.Companion.Handler

fun contract(vararg serverRoutes: ServerRoute) = contract(NoRenderer, "", NoSecurity, *serverRoutes)
fun contract(renderer: ContractRenderer, vararg serverRoutes: ServerRoute) = contract(renderer, "", NoSecurity, *serverRoutes)
fun contract(renderer: ContractRenderer, descriptionPath: String, vararg serverRoutes: ServerRoute) = contract(renderer, descriptionPath, NoSecurity, *serverRoutes)
fun contract(renderer: ContractRenderer = NoRenderer, descriptionPath: String = "", security: Security = NoSecurity, vararg serverRoutes: ServerRoute) =
    ContractRoutingHttpHandler(Handler(renderer, security, descriptionPath, "", serverRoutes.map { it }, Filter { { req -> it(req) } }))

operator fun <A> String.div(next: PathLens<A>): RouteSpec1<A> = RouteSpec0(toBaseFn(this), emptyList(), null) / next

operator fun <A, B> PathLens<A>.div(next: PathLens<B>): RouteSpec2<A, B> = RouteSpec1({ it }, emptyList(), null, this) / next

infix fun String.by(router: ContractRoutingHttpHandler): ContractRoutingHttpHandler = router.withBasePath(this)

@JvmName("newRequestString")
fun Pair<String, Method>.newRequest(baseUri: Uri = Uri.of("")) = Request(second, "").uri(baseUri.path(first))

@JvmName("newRequestRouteSpec")
fun Pair<RouteSpec, Method>.newRequest(baseUri: Uri = Uri.of("")) = Request(second, "").uri(baseUri.path(first.describe(Root)))

@JvmName("handler0String")
infix fun Pair<String, Method>.handler(handler: HttpHandler) = ServerRoute(second, RouteSpec0(toBaseFn(first), emptyList(), null), { handler })

@JvmName("handler1Path")
infix fun <A> Pair<PathLens<A>, Method>.handler(fn: (A) -> HttpHandler) = RouteSpec1({ it }, emptyList(), null, first) to second handler fn

@JvmName("handler0")
infix fun Pair<RouteSpec0, Method>.handler(handler: HttpHandler) = ServerRoute(second, first, { handler })

@JvmName("handler1")
infix fun <A> Pair<RouteSpec1<A>, Method>.handler(fn: (A) -> HttpHandler) = ServerRoute(second, first, { fn(it[first.a]) })

@JvmName("handler2")
infix fun <A, B> Pair<RouteSpec2<A, B>, Method>.handler(fn: (A, B) -> HttpHandler) = ServerRoute(second, first, { fn(it[first.a], it[first.b]) })

@JvmName("handler3")
infix fun <A, B, C> Pair<RouteSpec3<A, B, C>, Method>.handler(fn: (A, B, C) -> HttpHandler) = ServerRoute(second, first, { fn(it[first.a], it[first.b], it[first.c]) })

@JvmName("handler4")
infix fun <A, B, C, D> Pair<RouteSpec4<A, B, C, D>, Method>.handler(fn: (A, B, C, D) -> HttpHandler) = ServerRoute(second, first, { fn(it[first.a], it[first.b], it[first.c], it[first.d]) })

infix fun String.query(new: QueryLens<*>) = RouteSpec0(toBaseFn(this), listOf(new), null)
infix fun String.header(new: HeaderLens<*>) = RouteSpec0(toBaseFn(this), listOf(new), null)
infix fun String.body(new: BodyLens<*>) = RouteSpec0(toBaseFn(this), emptyList(), new)

infix fun RouteSpec0.query(new: QueryLens<*>) = RouteSpec0(pathFn, requestParams.plus(listOf(new)), body)
infix fun RouteSpec0.header(new: HeaderLens<*>) = RouteSpec0(pathFn, requestParams.plus(listOf(new)), body)
infix fun RouteSpec0.body(new: BodyLens<*>) = RouteSpec0(pathFn, requestParams, new)

infix fun <A> RouteSpec1<A>.query(new: QueryLens<*>) = RouteSpec1(pathFn, requestParams.plus(listOf(new)), body, a)
infix fun <A> RouteSpec1<A>.header(new: HeaderLens<*>) = RouteSpec1(pathFn, requestParams.plus(listOf(new)), body, a)
infix fun <A> RouteSpec1<A>.body(new: BodyLens<*>) = RouteSpec1(pathFn, requestParams, new, a)

infix fun <A, B> RouteSpec2<A, B>.query(new: QueryLens<*>) = RouteSpec2(pathFn, requestParams.plus(listOf(new)), body, a, b)
infix fun <A, B> RouteSpec2<A, B>.header(new: HeaderLens<*>) = RouteSpec2(pathFn, requestParams.plus(listOf(new)), body, a, b)
infix fun <A, B> RouteSpec2<A, B>.body(new: BodyLens<*>) = RouteSpec2(pathFn, requestParams, new, a, b)

infix fun <A, B, C> RouteSpec3<A, B, C>.query(new: QueryLens<*>) = RouteSpec3(pathFn, requestParams.plus(listOf(new)), body, a, b, c)
infix fun <A, B, C> RouteSpec3<A, B, C>.header(new: HeaderLens<*>) = RouteSpec3(pathFn, requestParams.plus(listOf(new)), body, a, b, c)
infix fun <A, B, C> RouteSpec3<A, B, C>.body(new: BodyLens<*>) = RouteSpec3(pathFn, requestParams, new, a, b, c)

infix fun <A, B, C, D> RouteSpec4<A, B, C, D>.query(new: QueryLens<*>) = RouteSpec4(pathFn, requestParams.plus(listOf(new)), body, a, b, c, d)
infix fun <A, B, C, D> RouteSpec4<A, B, C, D>.header(new: HeaderLens<*>) = RouteSpec4(pathFn, requestParams.plus(listOf(new)), body, a, b, c, d)
infix fun <A, B, C, D> RouteSpec4<A, B, C, D>.body(new: BodyLens<*>) = RouteSpec4(pathFn, requestParams, new, a, b, c, d)

infix fun ServerRoute.meta(new: RouteMeta) = ServerRoute(method, routeSpec, toHandler, new)

private fun toBaseFn(path: String): (BasePath) -> BasePath = when (BasePath(path)) {
    is Root -> { basePath: BasePath -> basePath }
    else -> { basePath: BasePath -> basePath / path.trimStart('/') }
}
