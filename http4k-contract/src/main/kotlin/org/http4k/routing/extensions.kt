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
import org.http4k.lens.PathLens
import org.http4k.routing.ContractRoutingHttpHandler.Companion.Handler

interface ContractBuilder {
    operator fun invoke(vararg serverRoutes: ServerRoute): ContractRoutingHttpHandler
}

fun contract(renderer: ContractRenderer = NoRenderer, descriptionPath: String = "", security: Security = NoSecurity) =
    object : ContractBuilder {
        override fun invoke(vararg serverRoutes: ServerRoute): ContractRoutingHttpHandler = ContractRoutingHttpHandler(Handler(
            renderer, security, descriptionPath, "", serverRoutes.map { it }, Filter { { req -> it(req) } }
        ))
    }

operator fun <A> String.div(next: PathLens<A>): RouteSpec1<A> = RouteSpec0({ it / this }, emptyList(), null) / next

operator fun <A, B> PathLens<A>.div(next: PathLens<B>): RouteSpec2<A, B> = RouteSpec1({ it }, emptyList(), null, this) / next

infix fun String.by(router: ContractRoutingHttpHandler): ContractRoutingHttpHandler = router.withBasePath(this)

infix fun Pair<Method, String>.bind(handler: HttpHandler) =
    ServerRoute(first, RouteSpec0({ if(BasePath(second) == Root) it else it /second  }, emptyList(), null), { handler })

@JvmName("bindPathDef0")
infix fun Pair<Method, RouteSpec0>.bind(handler: HttpHandler) = ServerRoute(first, second, { handler })

@JvmName("bind1")
infix fun <A> Pair<Method, PathLens<A>>.bind(fn: (A) -> HttpHandler) = first to RouteSpec1({ it }, emptyList(), null, second) bind fn

@JvmName("bind1Def")
infix fun <A> Pair<Method, RouteSpec1<A>>.bind(fn: (A) -> HttpHandler) = ServerRoute(first, second, { fn(it[second.a]) })

@JvmName("bind2")
infix fun <A, B> Pair<Method, RouteSpec2<A, B>>.bind(fn: (A, B) -> HttpHandler) = ServerRoute(first, second, { fn(it[second.a], it[second.b]) })

@JvmName("bind3")
infix fun <A, B, C> Pair<Method, RouteSpec3<A, B, C>>.bind(fn: (A, B, C) -> HttpHandler) = ServerRoute(first, second, { fn(it[second.a], it[second.b], it[second.c]) })

@JvmName("bind4")
infix fun <A, B, C, D> Pair<Method, RouteSpec4<A, B, C, D>>.bind(fn: (A, B, C, D) -> HttpHandler) = ServerRoute(first, second, { fn(it[second.a], it[second.b], it[second.c], it[second.d]) })
