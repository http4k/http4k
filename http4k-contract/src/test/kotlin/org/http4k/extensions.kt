package org.http4k

import org.http4k.contract.ContractRenderer
import org.http4k.contract.ContractRoutingHttpHandler
import org.http4k.contract.NoRenderer
import org.http4k.contract.NoSecurity
import org.http4k.contract.Security
import org.http4k.contract.ServerRoute
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.lens.Path
import org.http4k.lens.PathLens
import org.http4k.lens.int
import org.http4k.routing.routes
import org.http4k.contract.ContractRoutingHttpHandler.Companion.Handler as ContractHandler

infix fun String.by(router: ContractRoutingHttpHandler): ContractRoutingHttpHandler = router.withBasePath(this)

interface ContractBuilder {
    operator fun invoke(vararg sbbs: SBB): ContractRoutingHttpHandler
}

fun cont(renderer: ContractRenderer = NoRenderer, descriptionPath: String = "", security: Security = NoSecurity) =
    object : ContractBuilder {
        override fun invoke(vararg sbbs: SBB): ContractRoutingHttpHandler {
            return ContractRoutingHttpHandler(ContractHandler(renderer, security, descriptionPath)).withRoutes(sbbs.map { it.toServerRoute() })
        }
    }

abstract class PB internal constructor() {
    abstract infix operator fun <NEXT> div(next: PathLens<NEXT>): PB

    open infix operator fun div(next: String) = div(Path.fixed(next))
}

class PB0 internal constructor() : PB() {

    override infix operator fun div(next: String) = PB0()

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = PB1<NEXT>()

    infix fun bind(handler: HttpHandler): ServerRoute = TODO()
}

class PB1<out A> internal constructor() : PB() {
    override infix operator fun div(next: String) = throw UnsupportedOperationException("no longer paths!")

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = throw UnsupportedOperationException("no longer paths!")

    infix fun bind(fn: (A) -> HttpHandler): ServerRoute = TODO()
}

operator fun String.div(pathLens: PathLens<*>): PB0 = TODO()

fun bob(i: Int): HttpHandler = TODO()

interface SBB {
    infix fun describedBy(spec: Desc): SBB

    fun toServerRoute(): ServerRoute
}

fun main(args: Array<String>) {

    val a = routes(
        "/contract" by cont(NoRenderer)(
            GET to "value" / Path.of("hello") / Path.int().of("world") bindTo ::bob describedBy Desc(),
            GET to "value2" / Path.of("hello") / Path.int().of("world") bindTo ::bob
        )
    )

    a(Request(GET, "/contract/value2/bob/bob2"))
}

infix fun <A> Pair<Method, PB1<A>>.bindTo(fn: (A) -> HttpHandler): SBB = object : SBB {
    override infix fun describedBy(spec: Desc): SBB = TODO()
    override fun toServerRoute(): ServerRoute = TODO()
}

//infix fun <A> Pair<Method, PB1<A>>.speccedBy(spec: ContractSpec, b: String): ServerRoute = TODO()
