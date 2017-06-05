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
import org.http4k.lens.Lens
import org.http4k.lens.PathLens
import org.http4k.routing.ContractRoutingHttpHandler.Companion.Handler

fun contract(vararg serverRoutes: ServerRoute) = contract(NoRenderer, "", NoSecurity, *serverRoutes)
fun contract(renderer: ContractRenderer, vararg serverRoutes: ServerRoute) = contract(renderer, "", NoSecurity, *serverRoutes)
fun contract(renderer: ContractRenderer, descriptionPath: String, vararg serverRoutes: ServerRoute) = contract(renderer, descriptionPath, NoSecurity, *serverRoutes)
fun contract(renderer: ContractRenderer = NoRenderer, descriptionPath: String = "", security: Security = NoSecurity, vararg serverRoutes: ServerRoute) =
    ContractRoutingHttpHandler(Handler(renderer, security, descriptionPath, "", serverRoutes.map { it }, Filter { { req -> it(req) } }))

operator infix fun String.rem(new: Lens<Request, *>) = RouteSpec0(toBaseFn(this), listOf(new), null)

operator infix fun String.rem(new: BodyLens<*>) = RouteSpec0(toBaseFn(this), emptyList(), new)

operator fun <A> String.div(next: PathLens<A>): RouteSpec1<A> = RouteSpec0(toBaseFn(this), emptyList(), null) / next

operator fun <A, B> PathLens<A>.div(next: PathLens<B>): RouteSpec2<A, B> = RouteSpec1({ it }, emptyList(), null, this) / next

infix fun String.by(router: ContractRoutingHttpHandler): ContractRoutingHttpHandler = router.withBasePath(this)

fun Pair<RouteSpec, Method>.newRequest(baseUri: Uri) = Request(second, "").uri(baseUri.path(first.describe(Root)))

@JvmName("bind0String")
infix fun Pair<String, Method>.bind(handler: HttpHandler) = ServerRoute(second, RouteSpec0(toBaseFn(first), emptyList(), null), { handler })

@JvmName("bind1Path")
infix fun <A> Pair<PathLens<A>, Method>.bind(fn: (A) -> HttpHandler) = RouteSpec1({ it }, emptyList(), null, first) to second bind fn

@JvmName("bind0")
infix fun Pair<RouteSpec0, Method>.bind(handler: HttpHandler) = ServerRoute(second, first, { handler })

@JvmName("bind1")
infix fun <A> Pair<RouteSpec1<A>, Method>.bind(fn: (A) -> HttpHandler) = ServerRoute(second, first, { fn(it[first.a]) })

@JvmName("bind2")
infix fun <A, B> Pair<RouteSpec2<A, B>, Method>.bind(fn: (A, B) -> HttpHandler) = ServerRoute(second, first, { fn(it[first.a], it[first.b]) })

@JvmName("bind3")
infix fun <A, B, C> Pair<RouteSpec3<A, B, C>, Method>.bind(fn: (A, B, C) -> HttpHandler) = ServerRoute(second, first, { fn(it[first.a], it[first.b], it[first.c]) })

@JvmName("bind4")
infix fun <A, B, C, D> Pair<RouteSpec4<A, B, C, D>, Method>.bind(fn: (A, B, C, D) -> HttpHandler) = ServerRoute(second, first, { fn(it[first.a], it[first.b], it[first.c], it[first.d]) })

private fun toBaseFn(path: String): (BasePath) -> BasePath = when (BasePath(path)) {
    is Root -> { basePath: BasePath -> basePath }
    else -> { basePath: BasePath -> basePath / path.trimStart('/') }
}
