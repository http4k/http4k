package org.http4k.routing

import org.http4k.contract.BasePath
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.lens.BodyLens
import org.http4k.lens.Failure
import org.http4k.lens.Lens
import org.http4k.lens.LensFailure
import org.http4k.lens.Path
import org.http4k.lens.PathLens

data class RequestParameters(val list: List<Lens<Request, *>> = emptyList(), val body: BodyLens<*>? = null)

abstract class RouteSpec internal constructor(val pathFn: (BasePath) -> BasePath,
                                              val requestParams: RequestParameters,
                                              vararg val pathLenses: PathLens<*>) : Filter {

    abstract infix operator fun <T> div(next: PathLens<T>): RouteSpec

    open infix operator fun div(next: String) = div(Path.fixed(next))

    override fun invoke(nextHandler: HttpHandler): HttpHandler =
        { req ->
            val body = requestParams.body?.let { listOf(it::invoke) } ?: emptyList<(Request) -> Any?>()
            val errors = body.plus(requestParams.list).fold(emptyList<Failure>()) { memo, next ->
                try {
                    next(req)
                    memo
                } catch (e: LensFailure) {
                    memo.plus(e.failures)
                }
            }
            if (errors.isEmpty()) nextHandler(req) else throw LensFailure(errors)
        }

    internal fun describe(contractRoot: BasePath): String = "${pathFn(contractRoot)}${if (pathLenses.isNotEmpty()) "/${pathLenses.joinToString("/")}" else ""}"
}

class RouteSpec0 internal constructor(pathFn: (BasePath) -> BasePath, requestParams: RequestParameters) : RouteSpec(pathFn, requestParams) {
    override infix operator fun div(next: String) = RouteSpec0({ it / next }, requestParams)

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = RouteSpec1(pathFn, requestParams, next)
}

class RouteSpec1<out A> internal constructor(pathFn: (BasePath) -> BasePath, requestParams: RequestParameters, val a: PathLens<A>) : RouteSpec(pathFn, requestParams, a) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = RouteSpec2(pathFn, requestParams, a, next)
}

class RouteSpec2<out A, out B> internal constructor(pathFn: (BasePath) -> BasePath, requestParams: RequestParameters, val a: PathLens<A>, val b: PathLens<B>) : RouteSpec(pathFn, requestParams, a, b) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = RouteSpec3(pathFn, requestParams, a, b, next)
}

class RouteSpec3<out A, out B, out C> internal constructor(pathFn: (BasePath) -> BasePath, requestParams: RequestParameters,
                                                           val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>) : RouteSpec(pathFn, requestParams, a, b, c) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = RouteSpec4(pathFn, requestParams, a, b, c, next)
}

class RouteSpec4<out A, out B, out C, out D> internal constructor(pathFn: (BasePath) -> BasePath, requestParams: RequestParameters,
                                                                  val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>, val d: PathLens<D>) : RouteSpec(pathFn, requestParams, a, b, c, d) {
    override infix operator fun div(next: String) = throw UnsupportedOperationException("no longer paths!")

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = throw UnsupportedOperationException("no longer paths!")
}

