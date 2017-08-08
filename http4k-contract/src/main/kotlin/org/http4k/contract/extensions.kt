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

interface Binder<in T> {
    infix fun to(fn: T): ContractRoute
}

infix fun <A> PathLens<A>.bindContract(method: Method) = ContractRouteSpec1({ it }, emptyList(), null, this).bindContract(method)


infix fun String.bindContract(method: Method): Binder<HttpHandler> = ContractRouteSpec0(toBaseFn(this), emptyList(), null).bindContract(method)

infix fun ContractRouteSpec0.bindContract(method: Method) = let { spec ->
    object : Binder<HttpHandler> {
        override fun to(fn: HttpHandler): ContractRoute = ContractRoute(method, spec, { fn })
    }
}

infix fun <A> ContractRouteSpec1<A>.bindContract(method: Method) = let { spec ->
    object : Binder<(A) -> HttpHandler> {
        override fun to(fn: (A) -> HttpHandler): ContractRoute = ContractRoute(method, spec, { fn(it[spec.a]) })
    }
}

infix fun <A, B> ContractRouteSpec2<A, B>.bindContract(method: Method) = let { spec ->
    object : Binder<(A, B) -> HttpHandler> {
        override fun to(fn: (A, B) -> HttpHandler): ContractRoute = ContractRoute(method, spec, { fn(it[spec.a], it[spec.b]) })
    }
}

infix fun <A, B, C> ContractRouteSpec3<A, B, C>.bindContract(method: Method) = let { spec ->
    object : Binder<(A, B, C) -> HttpHandler> {
        override fun to(fn: (A, B, C) -> HttpHandler): ContractRoute = ContractRoute(method, spec, { fn(it[spec.a], it[spec.b], it[spec.c]) })
    }
}

infix fun <A, B, C, D> ContractRouteSpec4<A, B, C, D>.bindContract(method: Method) = let { spec ->
    object : Binder<(A, B, C, D) -> HttpHandler> {
        override fun to(fn: (A, B, C, D) -> HttpHandler): ContractRoute = ContractRoute(method, spec, { fn(it[spec.a], it[spec.b], it[spec.c], it[spec.d]) })
    }
}

@JvmName("newRequestString")
fun Pair<String, Method>.newRequest(baseUri: Uri = Uri.of("")) = Request(second, "").uri(baseUri.path(first))

@JvmName("newRequestRouteSpec")
fun Pair<ContractRouteSpec, Method>.newRequest(baseUri: Uri = Uri.of("")) = Request(second, "").uri(baseUri.path(first.describe(Root)))

@JvmName("handler0String")
infix fun Pair<String, Method>.bind(handler: HttpHandler) = ContractRouteSpec0(toBaseFn(first), emptyList(), null).bindContract(second) to handler

@JvmName("handler1Path")
infix fun <A> Pair<PathLens<A>, Method>.bind(fn: (A) -> HttpHandler) = ContractRouteSpec1({ it }, emptyList(), null, first).bindContract(second) to fn


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
