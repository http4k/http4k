package org.http4k

import org.http4k.contract.BasePath
import org.http4k.contract.ContractRenderer
import org.http4k.contract.ContractRoutingHttpHandler
import org.http4k.contract.NoRenderer
import org.http4k.contract.NoSecurity
import org.http4k.contract.Route
import org.http4k.contract.Security
import org.http4k.contract.ServerRoute
import org.http4k.contract.basePath
import org.http4k.contract.without
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.lens.LensFailure
import org.http4k.lens.Path
import org.http4k.lens.PathLens
import org.http4k.lens.int
import org.http4k.routing.Router
import org.http4k.routing.routes
import org.http4k.contract.ContractRoutingHttpHandler.Companion.Handler as ContractHandler

infix fun String.by(router: ContractRoutingHttpHandler): ContractRoutingHttpHandler = router.withBasePath(this)

interface ContractBuilder {
    operator fun invoke(vararg sbbs: SBB): ContractRoutingHttpHandler
}

fun cont(renderer: ContractRenderer = NoRenderer, descriptionPath: String = "", security: Security = NoSecurity) =
    object : ContractBuilder {
        override fun invoke(vararg sbbs: SBB): ContractRoutingHttpHandler = TODO()
    }

class ServerRoute2 internal constructor(private val pathBinder: PB, private val toHandler: (ExtractedParts) -> HttpHandler) {
    internal val core = pathBinder.core.route.core
    internal val method = pathBinder.core.method
    internal fun router(contractRoot: BasePath): Router = pathBinder.toRouter(contractRoot, toHandler)
}

abstract class PB internal constructor(internal val core: Core, internal vararg val pathLenses: PathLens<*>) {
    abstract infix operator fun <T> div(next: PathLens<T>): PB

    open infix operator fun div(next: String) = div(Path.fixed(next))

    internal fun toRouter(contractRoot: BasePath, toHandler: (ExtractedParts) -> HttpHandler): Router = object : Router {
        override fun match(request: Request): HttpHandler? = core.matches(contractRoot, request, pathLenses.toList(), toHandler)
    }

    internal fun describe(contractRoot: BasePath): String {
        return "${core.pathFn(contractRoot)}${if (pathLenses.isNotEmpty()) "/${pathLenses.joinToString("/")}" else ""}"
    }

    companion object {
        internal data class Core(val route: Route, val method: Method, val pathFn: (BasePath) -> BasePath) {
            infix operator fun div(next: String) = copy(pathFn = { pathFn(it) / next })

            fun matches(contractRoot: BasePath, request: Request, lenses: List<PathLens<*>>, toHandler: (ExtractedParts) -> HttpHandler): HttpHandler? =
                if (request.method == method && request.basePath().startsWith(pathFn(contractRoot))) {
                    try {
                        request.without(pathFn(contractRoot)).extract(lenses)?.let { route.core.validationFilter.then(toHandler(it)) }
                    } catch (e: LensFailure) {
                        null
                    }
                } else null
        }
    }
}

class PB0 internal constructor(core: PB.Companion.Core) : PB(core) {

    override infix operator fun div(next: String) = PB0(core)

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = PB1<NEXT>(core)

    infix fun bind(handler: HttpHandler): ServerRoute = TODO()
}

class PB1<out A> internal constructor(core: PB.Companion.Core) : PB(core) {
    override infix operator fun div(next: String) = throw UnsupportedOperationException("no longer paths!")

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = throw UnsupportedOperationException("no longer paths!")

    infix fun bind(fn: (A) -> HttpHandler): ServerRoute = TODO()
}

operator fun String.div(pathLens: PathLens<*>): PB0 = TODO()

fun bob(i: Int): HttpHandler = TODO()

interface SBB {
    infix fun describedBy(spec: Desc): SBB

    fun toServerRoute(): ServerRoute2
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
    override fun toServerRoute(): ServerRoute2 = TODO()
}

//infix fun <A> Pair<Method, PB1<A>>.speccedBy(spec: ContractSpec, b: String): ServerRoute = TODO()

internal class ExtractedParts(private val mapping: Map<PathLens<*>, *>) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(lens: PathLens<T>): T = mapping[lens] as T
}

private operator fun <T> BasePath.invoke(index: Int, fn: (String) -> T): T? = toList().let { if (it.size > index) fn(it[index]) else null }

private fun BasePath.extract(lenses: List<PathLens<*>>): ExtractedParts? =
    if (this.toList().size == lenses.size) ExtractedParts(lenses.mapIndexed { index, lens -> lens to this(index, lens::invoke) }.toMap()) else null
