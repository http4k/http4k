package org.http4k.contract


import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.lens.PathLens

fun contract(vararg serverRoutes: ContractRoute) = contract(NoRenderer, "", NoSecurity, *serverRoutes)
fun contract(renderer: ContractRenderer, vararg serverRoutes: ContractRoute) = contract(renderer, "", NoSecurity, *serverRoutes)
fun contract(renderer: ContractRenderer, descriptionPath: String, vararg serverRoutes: ContractRoute) = contract(renderer, descriptionPath, NoSecurity, *serverRoutes)
fun contract(renderer: ContractRenderer = NoRenderer, descriptionPath: String = "", security: Security = NoSecurity, vararg serverRoutes: ContractRoute) =
    ContractRoutingHttpHandler(renderer, security, descriptionPath, "", serverRoutes.map { it }, Filter { { req -> it(req) } })

operator fun <A> String.div(next: PathLens<A>): ContractRouteSpec1<A> = ContractRouteSpec0(toBaseFn(this), RouteMeta()) / next

operator fun <A, B> PathLens<A>.div(next: PathLens<B>): ContractRouteSpec2<A, B> = ContractRouteSpec1({ it }, RouteMeta(), this) / next

infix fun String.bind(router: ContractRoutingHttpHandler): ContractRoutingHttpHandler = router.withBasePath(this)

interface RouteBinder<in T> {
    fun newRequest(baseUri: Uri = Uri.of("")): Request
    infix fun to(fn: T): ContractRoute
}

infix fun <A> PathLens<A>.bindContract(method: Method) = ContractRouteSpec1({ it }, RouteMeta(), this).bindContract(method)


infix fun String.bindContract(method: Method): RouteBinder<HttpHandler> = ContractRouteSpec0(toBaseFn(this), RouteMeta()).bindContract(method)

infix fun ContractRouteSpec0.bindContract(method: Method) = let { spec ->
    object : RouteBinder<HttpHandler> {
        override fun newRequest(baseUri: Uri) = Request(method, "").uri(baseUri.path(spec.describe(Root)))

        override fun to(fn: HttpHandler): ContractRoute = ContractRoute(method, spec, { fn })
    }
}

infix fun <A> ContractRouteSpec1<A>.bindContract(method: Method) = let { spec ->
    object : RouteBinder<(A) -> HttpHandler> {
        override fun newRequest(baseUri: Uri) = Request(method, "").uri(baseUri.path(spec.describe(Root)))

        override fun to(fn: (A) -> HttpHandler): ContractRoute = ContractRoute(method, spec, { fn(it[spec.a]) })
    }
}

infix fun <A, B> ContractRouteSpec2<A, B>.bindContract(method: Method) = let { spec ->
    object : RouteBinder<(A, B) -> HttpHandler> {
        override fun newRequest(baseUri: Uri) = Request(method, "").uri(baseUri.path(spec.describe(Root)))
        override fun to(fn: (A, B) -> HttpHandler): ContractRoute = ContractRoute(method, spec, { fn(it[spec.a], it[spec.b]) })
    }
}

infix fun <A, B, C> ContractRouteSpec3<A, B, C>.bindContract(method: Method) = let { spec ->
    object : RouteBinder<(A, B, C) -> HttpHandler> {
        override fun newRequest(baseUri: Uri) = Request(method, "").uri(baseUri.path(spec.describe(Root)))
        override fun to(fn: (A, B, C) -> HttpHandler): ContractRoute = ContractRoute(method, spec, { fn(it[spec.a], it[spec.b], it[spec.c]) })
    }
}

infix fun <A, B, C, D> ContractRouteSpec4<A, B, C, D>.bindContract(method: Method) = let { spec ->
    object : RouteBinder<(A, B, C, D) -> HttpHandler> {
        override fun newRequest(baseUri: Uri): Request = Request(method, "").uri(baseUri.path(spec.describe(Root)))
        override fun to(fn: (A, B, C, D) -> HttpHandler): ContractRoute = ContractRoute(method, spec, { fn(it[spec.a], it[spec.b], it[spec.c], it[spec.d]) })
    }
}

//// TODO deprecate or remove??
//@JvmName("handler0String")
//infix fun Pair<String, Method>.bind(handler: HttpHandler) = ContractRouteSpec0(toBaseFn(first), RouteMeta()).bindContract(second) to handler
//
//// TODO deprecate or remove??
//@JvmName("handler1Path")
//infix fun <A> Pair<PathLens<A>, Method>.bind(fn: (A) -> HttpHandler) = ContractRouteSpec1({ it }, RouteMeta(), first).bindContract(second) to fn

infix fun String.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec0(toBaseFn(this), metaDsl(new))
infix fun ContractRouteSpec0.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec0(pathFn, metaDsl(new))
infix fun <A> ContractRouteSpec1<A>.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec1(pathFn, metaDsl(new), a)
infix fun <A, B> ContractRouteSpec2<A, B>.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec2(pathFn, metaDsl(new), a, b)
infix fun <A, B, C> ContractRouteSpec3<A, B, C>.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec3(pathFn, metaDsl(new), a, b, c)
infix fun <A, B, C, D> ContractRouteSpec4<A, B, C, D>.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec4(pathFn, metaDsl(new), a, b, c, d)

internal fun toBaseFn(path: String): (PathSegments) -> PathSegments = PathSegments(path).let { { old: PathSegments -> old / it } }
