package org.http4k


import org.http4k.Contract.Companion.Handler
import org.http4k.contract.ContractRenderer
import org.http4k.contract.ContractRoutingHttpHandler
import org.http4k.contract.NoRenderer
import org.http4k.contract.NoSecurity
import org.http4k.contract.Security
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.lens.PathLens
import org.http4k.contract.ContractRoutingHttpHandler.Companion.Handler as ContractHandler

infix fun String.by(router: Contract): Contract = router.withBasePath(this)
infix fun String.by(router: ContractRoutingHttpHandler): ContractRoutingHttpHandler = router.withBasePath(this)

interface ContractBuilder {
    operator fun invoke(vararg sbbs: SBB): Contract
}

fun cont(renderer: ContractRenderer = NoRenderer, descriptionPath: String = "", security: Security = NoSecurity) =
    object : ContractBuilder {
        override fun invoke(vararg sbbs: SBB): Contract = Contract(Handler(
            renderer, security, descriptionPath, "", sbbs.map { it.toServerRoute() }, Filter { { req -> it(req) } }
        ))
    }

operator fun <A> String.div(next: PathLens<A>): PathDef1<A> = PathDef0 { it } / next

infix fun Pair<Method, String>.bindTo(fn: HttpHandler): SBB = SBB0(first, PathDef0 { it / second }, fn)

@JvmName("bind0")
infix fun Pair<Method, PathDef0>.bindTo(fn: HttpHandler): SBB = SBB0(first, second, fn)

@JvmName("bind1")
infix fun <A> Pair<Method, PathDef1<A>>.bindTo(fn: (A) -> HttpHandler): SBB = SBB1(first, second, fn)

@JvmName("bind2")
infix fun <A, B> Pair<Method, PathDef2<A, B>>.bindTo(fn: (A, B) -> HttpHandler): SBB = SBB2(first, second, fn)

@JvmName("bind3")
infix fun <A, B, C> Pair<Method, PathDef3<A, B, C>>.bindTo(fn: (A, B, C) -> HttpHandler): SBB = SBB3(first, second, fn)
