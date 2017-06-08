package org.http4k.contract

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.lens.BodyLens
import org.http4k.lens.Failure
import org.http4k.lens.Lens
import org.http4k.lens.LensFailure
import org.http4k.lens.Path
import org.http4k.lens.PathLens

abstract class ContractRouteSpec internal constructor(val pathFn: (PathSegments) -> PathSegments,
                                                      val requestParams: List<Lens<Request, *>>,
                                                      val body: BodyLens<*>?,
                                                      vararg val pathLenses: PathLens<*>) : Filter {

    abstract infix operator fun <T> div(next: PathLens<T>): ContractRouteSpec

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

class ContractRouteSpec0 internal constructor(pathFn: (PathSegments) -> PathSegments, requestParams: List<Lens<Request, *>>, body: BodyLens<*>?) : ContractRouteSpec(pathFn, requestParams, body) {
    override infix operator fun div(next: String) = ContractRouteSpec0({ it / next }, emptyList(), body)

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec1(pathFn, requestParams, body, next)
}

class ContractRouteSpec1<out A> internal constructor(pathFn: (PathSegments) -> PathSegments, requestParams: List<Lens<Request, *>>, body: BodyLens<*>?, val a: PathLens<A>) : ContractRouteSpec(pathFn, requestParams, body, a) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec2(pathFn, requestParams, body, a, next)
}

class ContractRouteSpec2<out A, out B> internal constructor(pathFn: (PathSegments) -> PathSegments, requestParams: List<Lens<Request, *>>, body: BodyLens<*>?, val a: PathLens<A>, val b: PathLens<B>) : ContractRouteSpec(pathFn, requestParams, body, a, b) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec3(pathFn, requestParams, body, a, b, next)
}

class ContractRouteSpec3<out A, out B, out C> internal constructor(pathFn: (PathSegments) -> PathSegments, requestParams: List<Lens<Request, *>>, body: BodyLens<*>?,
                                                                   val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>) : ContractRouteSpec(pathFn, requestParams, body, a, b, c) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec4(pathFn, requestParams, body, a, b, c, next)
}

class ContractRouteSpec4<out A, out B, out C, out D> internal constructor(pathFn: (PathSegments) -> PathSegments, requestParams: List<Lens<Request, *>>, body: BodyLens<*>?,
                                                                          val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>, val d: PathLens<D>) : ContractRouteSpec(pathFn, requestParams, body, a, b, c, d) {
    override infix operator fun div(next: String) = throw UnsupportedOperationException("no longer paths!")

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = throw UnsupportedOperationException("no longer paths!")
}

