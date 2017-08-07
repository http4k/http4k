package org.http4k.contract


import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.lens.BodyLens
import org.http4k.lens.HeaderLens
import org.http4k.lens.PathLens
import org.http4k.lens.QueryLens

fun contract(vararg serverRoutes: ContractRoute) = contract(NoRenderer, "", NoSecurity, *serverRoutes)
fun contract(renderer: ContractRenderer, vararg serverRoutes: ContractRoute) = contract(renderer, "", NoSecurity, *serverRoutes)
fun contract(renderer: ContractRenderer, descriptionPath: String, vararg serverRoutes: ContractRoute) = contract(renderer, descriptionPath, NoSecurity, *serverRoutes)
fun contract(renderer: ContractRenderer = NoRenderer, descriptionPath: String = "", security: Security = NoSecurity, vararg serverRoutes: ContractRoute) =
    ContractRoutingHttpHandler(renderer, security, descriptionPath, "", serverRoutes.map { it }, Filter { { req -> it(req) } })

operator fun <A> String.div(next: PathLens<A>): ContractRouteSpec1<A> = ContractRouteSpec0(toBaseFn(this), emptyList(), null) / next

operator fun <A, B> PathLens<A>.div(next: PathLens<B>): ContractRouteSpec2<A, B> = ContractRouteSpec1({ it }, emptyList(), null, this) / next

infix fun String.bind(router: ContractRoutingHttpHandler): ContractRoutingHttpHandler = router.withBasePath(this)

@JvmName("newRequestString")
fun Pair<String, Method>.newRequest(baseUri: Uri = Uri.of("")) = Request(second, "").uri(baseUri.path(first))

@JvmName("newRequestRouteSpec")
fun Pair<ContractRouteSpec, Method>.newRequest(baseUri: Uri = Uri.of("")) = Request(second, "").uri(baseUri.path(first.describe(Root)))

@JvmName("handler0String")
infix fun Pair<String, Method>.bind(handler: HttpHandler) = ContractRoute(second, ContractRouteSpec0(toBaseFn(first), emptyList(), null), { handler })

@JvmName("handler1Path")
infix fun <A> Pair<PathLens<A>, Method>.bind(fn: (A) -> HttpHandler) = ContractRouteSpec1({ it }, emptyList(), null, first) to second bind fn

@JvmName("handler0")
infix fun Pair<ContractRouteSpec0, Method>.bind(handler: HttpHandler) = ContractRoute(second, first, { handler })

@JvmName("handler1")
infix fun <A> Pair<ContractRouteSpec1<A>, Method>.bind(fn: (A) -> HttpHandler) = ContractRoute(second, first, { fn(it[first.a]) })

@JvmName("handler2")
infix fun <A, B> Pair<ContractRouteSpec2<A, B>, Method>.bind(fn: (A, B) -> HttpHandler) = ContractRoute(second, first, { fn(it[first.a], it[first.b]) })

@JvmName("handler3")
infix fun <A, B, C> Pair<ContractRouteSpec3<A, B, C>, Method>.bind(fn: (A, B, C) -> HttpHandler) = ContractRoute(second, first, { fn(it[first.a], it[first.b], it[first.c]) })

@JvmName("handler4")
infix fun <A, B, C, D> Pair<ContractRouteSpec4<A, B, C, D>, Method>.bind(fn: (A, B, C, D) -> HttpHandler) = ContractRoute(second, first, { fn(it[first.a], it[first.b], it[first.c], it[first.d]) })

infix fun String.query(new: QueryLens<*>) = ContractRouteSpec0(toBaseFn(this), listOf(new), null)
infix fun String.header(new: HeaderLens<*>) = ContractRouteSpec0(toBaseFn(this), listOf(new), null)
infix fun String.body(new: BodyLens<*>) = ContractRouteSpec0(toBaseFn(this), emptyList(), new)

infix fun ContractRouteSpec0.query(new: QueryLens<*>) = ContractRouteSpec0(pathFn, requestParams.plus(listOf(new)), body)
infix fun ContractRouteSpec0.header(new: HeaderLens<*>) = ContractRouteSpec0(pathFn, requestParams.plus(listOf(new)), body)
infix fun ContractRouteSpec0.body(new: BodyLens<*>) = ContractRouteSpec0(pathFn, requestParams, new)

infix fun <A> ContractRouteSpec1<A>.query(new: QueryLens<*>) = ContractRouteSpec1(pathFn, requestParams.plus(listOf(new)), body, a)
infix fun <A> ContractRouteSpec1<A>.header(new: HeaderLens<*>) = ContractRouteSpec1(pathFn, requestParams.plus(listOf(new)), body, a)
infix fun <A> ContractRouteSpec1<A>.body(new: BodyLens<*>) = ContractRouteSpec1(pathFn, requestParams, new, a)

infix fun <A, B> ContractRouteSpec2<A, B>.query(new: QueryLens<*>) = ContractRouteSpec2(pathFn, requestParams.plus(listOf(new)), body, a, b)
infix fun <A, B> ContractRouteSpec2<A, B>.header(new: HeaderLens<*>) = ContractRouteSpec2(pathFn, requestParams.plus(listOf(new)), body, a, b)
infix fun <A, B> ContractRouteSpec2<A, B>.body(new: BodyLens<*>) = ContractRouteSpec2(pathFn, requestParams, new, a, b)

infix fun <A, B, C> ContractRouteSpec3<A, B, C>.query(new: QueryLens<*>) = ContractRouteSpec3(pathFn, requestParams.plus(listOf(new)), body, a, b, c)
infix fun <A, B, C> ContractRouteSpec3<A, B, C>.header(new: HeaderLens<*>) = ContractRouteSpec3(pathFn, requestParams.plus(listOf(new)), body, a, b, c)
infix fun <A, B, C> ContractRouteSpec3<A, B, C>.body(new: BodyLens<*>) = ContractRouteSpec3(pathFn, requestParams, new, a, b, c)

infix fun <A, B, C, D> ContractRouteSpec4<A, B, C, D>.query(new: QueryLens<*>) = ContractRouteSpec4(pathFn, requestParams.plus(listOf(new)), body, a, b, c, d)
infix fun <A, B, C, D> ContractRouteSpec4<A, B, C, D>.header(new: HeaderLens<*>) = ContractRouteSpec4(pathFn, requestParams.plus(listOf(new)), body, a, b, c, d)
infix fun <A, B, C, D> ContractRouteSpec4<A, B, C, D>.body(new: BodyLens<*>) = ContractRouteSpec4(pathFn, requestParams, new, a, b, c, d)

infix fun ContractRoute.meta(new: RouteMeta) = ContractRoute(method, spec, toHandler, new)

private fun toBaseFn(path: String): (PathSegments) -> PathSegments = PathSegments(path).let { { old: PathSegments -> old / it } }
