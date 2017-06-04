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

operator fun <A> String.div(next: PathLens<A>): PathDef1<A> = PathDef0({ it / this }, emptyList(), null) / next

operator fun <A, B> PathLens<A>.div(next: PathLens<B>): PathDef2<A, B> = PathDef1({ it }, emptyList(), null, this) / next

infix fun String.by(router: ContractRoutingHttpHandler): ContractRoutingHttpHandler = router.withBasePath(this)

infix fun Pair<Method, String>.bind(handler: HttpHandler) =
    ServerRoute(first, PathDef0({ if(BasePath(second) == Root) it else it /second  }, emptyList(), null), { handler })

@JvmName("bindPathDef0")
infix fun Pair<Method, PathDef0>.bind(handler: HttpHandler) = ServerRoute(first, second, { handler })

@JvmName("bind1")
infix fun <A> Pair<Method, PathLens<A>>.bind(fn: (A) -> HttpHandler) = first to PathDef1({ it }, emptyList(), null, second) bind fn

@JvmName("bind1Def")
infix fun <A> Pair<Method, PathDef1<A>>.bind(fn: (A) -> HttpHandler) = ServerRoute(first, second, { fn(it[second.a]) })

@JvmName("bind2")
infix fun <A, B> Pair<Method, PathDef2<A, B>>.bind(fn: (A, B) -> HttpHandler) = ServerRoute(first, second, { fn(it[second.a], it[second.b]) })

@JvmName("bind3")
infix fun <A, B, C> Pair<Method, PathDef3<A, B, C>>.bind(fn: (A, B, C) -> HttpHandler) = ServerRoute(first, second, { fn(it[second.a], it[second.b], it[second.c]) })

@JvmName("bind4")
infix fun <A, B, C, D> Pair<Method, PathDef4<A, B, C, D>>.bind(fn: (A, B, C, D) -> HttpHandler) = ServerRoute(first, second, { fn(it[second.a], it[second.b], it[second.c], it[second.d]) })
