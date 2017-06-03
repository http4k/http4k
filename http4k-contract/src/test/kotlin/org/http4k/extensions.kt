package org.http4k


import org.http4k.contract.BasePath
import org.http4k.contract.ContractRenderer
import org.http4k.contract.ContractRoutingHttpHandler
import org.http4k.contract.NoRenderer
import org.http4k.contract.NoSecurity
import org.http4k.contract.Security
import org.http4k.contract.basePath
import org.http4k.contract.isIn
import org.http4k.contract.without
import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.lens.Header
import org.http4k.lens.LensFailure
import org.http4k.lens.Path
import org.http4k.lens.PathLens
import org.http4k.routing.Router
import org.http4k.routing.RoutingHttpHandler
import org.http4k.contract.ContractRoutingHttpHandler.Companion.Handler as ContractHandler

infix fun String.by(router: Another): Another = router.withBasePath(this)
infix fun String.by(router: ContractRoutingHttpHandler): ContractRoutingHttpHandler = router.withBasePath(this)

interface ContractBuilder {
    operator fun invoke(vararg sbbs: SBB): Another
}

fun cont(renderer: ContractRenderer = NoRenderer, descriptionPath: String = "", security: Security = NoSecurity) =
    object : ContractBuilder {
        override fun invoke(vararg sbbs: SBB): Another {
            val routes = sbbs.map { it.toServerRoute() }
            val filter = Filter { { req -> it(req) } }
            return Another(Another.Companion.Handler(
                renderer, security, descriptionPath, "", routes, filter
            ))
        }
    }

class ServerRoute2 internal constructor(private val sbb: SBB, private val toHandler: (ExtractedParts) -> HttpHandler) {
    internal val nonBodyParams = sbb.desc.core.requestParams.plus(sbb.pb.pathLenses).flatMap { it }

    internal val jsonRequest: Request? = sbb.desc.core.request?.let { if (Header.Common.CONTENT_TYPE(it) == ContentType.APPLICATION_JSON) it else null }

    internal val tags = sbb.desc.core.tags.toSet().sortedBy { it.name }

    internal fun router(contractRoot: BasePath): Router = sbb.toRouter(contractRoot, toHandler)

    internal fun describeFor(contractRoot: BasePath): String = sbb.pb.describe(contractRoot)
}

abstract class PB internal constructor(val pathFn: (BasePath) -> BasePath, vararg val pathLenses: PathLens<*>) {
    abstract infix operator fun <T> div(next: PathLens<T>): PB

    open infix operator fun div(next: String) = div(Path.fixed(next))

    internal fun describe(contractRoot: BasePath): String = "${pathFn(contractRoot)}${if (pathLenses.isNotEmpty()) "/${pathLenses.joinToString("/")}" else ""}"
}

class PB0 internal constructor(pathFn: (BasePath) -> BasePath) : PB(pathFn) {

    override infix operator fun div(next: String) = PB0 { it }

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = PB1(pathFn, next)
}

class PB1<out A> internal constructor(pathFn: (BasePath) -> BasePath, val a: PathLens<A>) : PB(pathFn) {
    override infix operator fun div(next: String) = throw UnsupportedOperationException("no longer paths!")

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = throw UnsupportedOperationException("no longer paths!")
}

operator fun <A> String.div(next: PathLens<A>): PB1<A> = PB0 { it } / next

abstract class SBB(val method: Method, val pb: PB, val desc: Desc) {
    abstract infix fun describedBy(new: Desc): SBB
    abstract fun toServerRoute(): ServerRoute2

    internal fun toRouter(contractRoot: BasePath, toHandler: (ExtractedParts) -> HttpHandler): Router = object : Router {
        override fun match(request: Request): HttpHandler? = matches(contractRoot, request, desc.core.validationFilter, pb.pathLenses.toList(), toHandler)
    }

    internal fun matches(contractRoot: BasePath, request: Request,
                         validationFilter: Filter,
                         lenses: List<PathLens<*>>,
                         toHandler: (ExtractedParts) -> HttpHandler): HttpHandler? =
        if (request.method == method && request.basePath().startsWith(pb.pathFn(contractRoot))) {
            try {
                request.without(pb.pathFn(contractRoot)).extract(lenses)?.let { validationFilter.then(toHandler(it)) }
            } catch (e: LensFailure) {
                null
            }
        } else null
}

class SBB0(method: Method, pb: PB, desc: Desc, private val fn: HttpHandler) : SBB(method, pb, desc) {
    override infix fun describedBy(new: Desc): SBB = SBB0(method, pb, desc, fn)
    override fun toServerRoute(): ServerRoute2 = ServerRoute2(this, { fn })
}

class SBB1<A>(method: Method, pb: PB, desc: Desc, private val fn: (A) -> HttpHandler, private val psA: PathLens<A>) : SBB(method, pb, desc) {
    override infix fun describedBy(new: Desc): SBB = SBB1(method, pb, desc, fn, psA)
    override fun toServerRoute(): ServerRoute2 = ServerRoute2(this, { parts -> fn(parts[psA]) })
}

infix fun Pair<Method, String>.bindTo(fn: HttpHandler): SBB = SBB0(first, PB0 { it / second }, Desc(), fn)

@JvmName("bind0")
infix fun Pair<Method, PB0>.bindTo(fn: HttpHandler): SBB = SBB0(first, second, Desc(), fn)

@JvmName("bind1")
infix fun <A> Pair<Method, PB1<A>>.bindTo(fn: (A) -> HttpHandler): SBB = SBB1(first, second, Desc(), fn, second.a)

internal class ExtractedParts(private val mapping: Map<PathLens<*>, *>) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(lens: PathLens<T>): T = mapping[lens] as T
}

private operator fun <T> BasePath.invoke(index: Int, fn: (String) -> T): T? = toList().let { if (it.size > index) fn(it[index]) else null }

private fun BasePath.extract(lenses: List<PathLens<*>>): ExtractedParts? =
    if (this.toList().size == lenses.size) ExtractedParts(lenses.mapIndexed { index, lens -> lens to this(index, lens::invoke) }.toMap()) else null

class Another internal constructor(val httpHandler: Another.Companion.Handler) : RoutingHttpHandler {
    override fun match(request: Request): HttpHandler? = httpHandler.match(request)

    override fun invoke(request: Request): Response = httpHandler(request)

    override fun withBasePath(new: String): Another = Another(httpHandler.withBasePath(new))
    override fun withFilter(new: Filter): RoutingHttpHandler = Another(httpHandler.withFilter(new))

    companion object {
        internal data class Handler(private val renderer: ContractRenderer,
                                    private val security: Security,
                                    private val descriptionPath: String,
                                    private val rootAsString: String = "",
                                    private val routes: List<ServerRoute2> = emptyList(),
                                    private val filter: Filter = ServerFilters.CatchLensFailure
        ) : HttpHandler {
            private val contractRoot = BasePath(rootAsString)
            internal fun withFilter(new: Filter) = copy(filter = filter.then(new))
            internal fun withBasePath(new: String) = copy(rootAsString = new + rootAsString)

            private val handler: HttpHandler = { match(it)?.invoke(it) ?: Response(Status.NOT_FOUND.description("Route not found")) }

            override fun invoke(request: Request): Response = handler(request)

            private val routers = routes
                .map { it.router(contractRoot) to security.filter.then(identify(it)).then(filter) }

            private val noMatch: HttpHandler? = null

            fun match(request: Request): HttpHandler? =
                if (request.isIn(contractRoot)) {
                    routers.fold(noMatch, { memo, (router, routeFilter) ->
                        memo ?: router.match(request)?.let { routeFilter.then(it) }
                    })
                } else null

            private fun identify(route: ServerRoute2): Filter =
                route.describeFor(contractRoot).let { routeIdentity ->
                    Filter { next -> { next(it.with(Header.X_URI_TEMPLATE of if (routeIdentity.isEmpty()) "/" else routeIdentity)) } }
                }
        }
    }

}