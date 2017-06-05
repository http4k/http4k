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

operator infix fun String.rem(new: Lens<Request, *>) = RouteSpec0({ if (BasePath(this) == Root) it else it / this }, listOf(new), null)

operator infix fun String.rem(new: BodyLens<*>) = RouteSpec0({ if (BasePath(this) == Root) it else it / this }, emptyList(), new)

operator fun <A> String.div(next: PathLens<A>): RouteSpec1<A> = RouteSpec0({ it / this }, emptyList(), null) / next

operator fun <A, B> PathLens<A>.div(next: PathLens<B>): RouteSpec2<A, B> = RouteSpec1({ it }, emptyList(), null, this) / next

infix fun String.by(router: ContractRoutingHttpHandler): ContractRoutingHttpHandler = router.withBasePath(this)

fun Pair<Method, RouteSpec>.newRequest(baseUri: Uri) = Request(first, "").uri(baseUri.path(second.describe(Root)))

@JvmName("bind0String")
infix fun Pair<Method, String>.bind(handler: HttpHandler) =
    ServerRoute(first, RouteSpec0({ if (BasePath(second) == Root) it else it / second }, emptyList(), null), { handler })

@JvmName("bind1Path")
infix fun <A> Pair<Method, PathLens<A>>.bind(fn: (A) -> HttpHandler) = first to RouteSpec1({ it }, emptyList(), null, second) bind fn

@JvmName("bind0")
infix fun Pair<Method, RouteSpec0>.bind(handler: HttpHandler) = ServerRoute(first, second, { handler })

@JvmName("bind1")
infix fun <A> Pair<Method, RouteSpec1<A>>.bind(fn: (A) -> HttpHandler) = ServerRoute(first, second, { fn(it[second.a]) })

@JvmName("bind2")
infix fun <A, B> Pair<Method, RouteSpec2<A, B>>.bind(fn: (A, B) -> HttpHandler) = ServerRoute(first, second, { fn(it[second.a], it[second.b]) })

@JvmName("bind3")
infix fun <A, B, C> Pair<Method, RouteSpec3<A, B, C>>.bind(fn: (A, B, C) -> HttpHandler) = ServerRoute(first, second, { fn(it[second.a], it[second.b], it[second.c]) })

@JvmName("bind4")
infix fun <A, B, C, D> Pair<Method, RouteSpec4<A, B, C, D>>.bind(fn: (A, B, C, D) -> HttpHandler) = ServerRoute(first, second, { fn(it[second.a], it[second.b], it[second.c], it[second.d]) })
