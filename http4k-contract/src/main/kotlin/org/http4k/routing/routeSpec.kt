package org.http4k.routing

import org.http4k.contract.PathSegments
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.lens.BodyLens
import org.http4k.lens.Failure
import org.http4k.lens.Lens
import org.http4k.lens.LensFailure
import org.http4k.lens.Path
import org.http4k.lens.PathLens

abstract class RouteSpec internal constructor(val pathFn: (PathSegments) -> PathSegments,
                                              val requestParams: List<Lens<Request, *>>,
                                              val body: BodyLens<*>?,
                                              vararg val pathLenses: PathLens<*>) : Filter {

    abstract infix operator fun <T> div(next: PathLens<T>): RouteSpec

    open infix operator fun div(next: String) = div(Path.fixed(next))

    override fun invoke(nextHandler: HttpHandler): HttpHandler =
        { req ->
            val body = body?.let { listOf(it::invoke) } ?: emptyList<(Request) -> Any?>()
            val errors = body.plus(requestParams).fold(emptyList<Failure>()) { memo, next ->
                try {
                    next(req)
                    memo
                } catch (e: LensFailure) {
                    memo.plus(e.failures)
                }
            }
            if (errors.isEmpty()) nextHandler(req) else throw LensFailure(errors)
        }

    internal fun describe(contractRoot: PathSegments): String = "${pathFn(contractRoot)}${if (pathLenses.isNotEmpty()) "/${pathLenses.joinToString("/")}" else ""}"
}

class RouteSpec0 internal constructor(pathFn: (PathSegments) -> PathSegments, requestParams: List<Lens<Request, *>>, body: BodyLens<*>?) : RouteSpec(pathFn, requestParams, body) {
    override infix operator fun div(next: String) = RouteSpec0({ it / next }, emptyList(), body)

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = RouteSpec1(pathFn, requestParams, body, next)
}

class RouteSpec1<out A> internal constructor(pathFn: (PathSegments) -> PathSegments, requestParams: List<Lens<Request, *>>, body: BodyLens<*>?, val a: PathLens<A>) : RouteSpec(pathFn, requestParams, body, a) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = RouteSpec2(pathFn, requestParams, body, a, next)
}

class RouteSpec2<out A, out B> internal constructor(pathFn: (PathSegments) -> PathSegments, requestParams: List<Lens<Request, *>>, body: BodyLens<*>?, val a: PathLens<A>, val b: PathLens<B>) : RouteSpec(pathFn, requestParams, body, a, b) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = RouteSpec3(pathFn, requestParams, body, a, b, next)
}

class RouteSpec3<out A, out B, out C> internal constructor(pathFn: (PathSegments) -> PathSegments, requestParams: List<Lens<Request, *>>, body: BodyLens<*>?,
                                                           val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>) : RouteSpec(pathFn, requestParams, body, a, b, c) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = RouteSpec4(pathFn, requestParams, body, a, b, c, next)
}

class RouteSpec4<out A, out B, out C, out D> internal constructor(pathFn: (PathSegments) -> PathSegments, requestParams: List<Lens<Request, *>>, body: BodyLens<*>?,
                                                                  val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>, val d: PathLens<D>) : RouteSpec(pathFn, requestParams, body, a, b, c, d) {
    override infix operator fun div(next: String) = throw UnsupportedOperationException("no longer paths!")

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = throw UnsupportedOperationException("no longer paths!")
}

