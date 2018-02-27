package org.http4k.contract

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.lens.Failure
import org.http4k.lens.LensFailure
import org.http4k.lens.Path
import org.http4k.lens.PathLens

abstract class ContractRouteSpec internal constructor(val pathFn: (PathSegments) -> PathSegments,
                                                      val routeMeta: RouteMeta,
                                                      vararg val pathLenses: PathLens<*>) : Filter {

    abstract fun with(new: RouteMeta): ContractRouteSpec
    abstract infix operator fun <T> div(next: PathLens<T>): ContractRouteSpec

    open infix operator fun div(next: String) = div(Path.fixed(next))

    override fun invoke(nextHandler: HttpHandler): HttpHandler =
        { req ->
            val body = routeMeta.body?.let { listOf(it::invoke) } ?: emptyList<(Request) -> Any?>()
            val errors = body.plus(routeMeta.requestParams).fold(emptyList<Failure>()) { memo, next ->
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

class ContractRouteSpec0 internal constructor(pathFn: (PathSegments) -> PathSegments, routeMeta: RouteMeta) : ContractRouteSpec(pathFn, routeMeta) {
    override fun with(new: RouteMeta): ContractRouteSpec0 = ContractRouteSpec0(pathFn, new)

    override infix operator fun div(next: String) = ContractRouteSpec0({ it / next }, routeMeta)

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec1(pathFn, routeMeta, next)
}

class ContractRouteSpec1<out A> internal constructor(pathFn: (PathSegments) -> PathSegments, routeMeta: RouteMeta, val a: PathLens<A>) : ContractRouteSpec(pathFn, routeMeta, a) {
    override fun with(new: RouteMeta): ContractRouteSpec1<A> = ContractRouteSpec1(pathFn, new, a)

    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec2(pathFn, routeMeta, a, next)
}

class ContractRouteSpec2<out A, out B> internal constructor(pathFn: (PathSegments) -> PathSegments, routeMeta: RouteMeta, val a: PathLens<A>, val b: PathLens<B>) : ContractRouteSpec(pathFn, routeMeta, a, b) {
    override fun with(new: RouteMeta): ContractRouteSpec2<A, B> = ContractRouteSpec2(pathFn, new, a, b)

    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec3(pathFn, routeMeta, a, b, next)
}

class ContractRouteSpec3<out A, out B, out C> internal constructor(pathFn: (PathSegments) -> PathSegments, routeMeta: RouteMeta,
                                                                   val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>) : ContractRouteSpec(pathFn, routeMeta, a, b, c) {
    override fun with(new: RouteMeta): ContractRouteSpec3<A, B, C> = ContractRouteSpec3(pathFn, new, a, b, c)

    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec4(pathFn, routeMeta, a, b, c, next)
}

class ContractRouteSpec4<out A, out B, out C, out D> internal constructor(pathFn: (PathSegments) -> PathSegments, routeMeta: RouteMeta,
                                                                          val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>, val d: PathLens<D>) : ContractRouteSpec(pathFn, routeMeta, a, b, c, d) {

    override fun with(new: RouteMeta): ContractRouteSpec4<A, B, C, D> = ContractRouteSpec4(pathFn, new, a, b, c, d)

    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec5(pathFn, routeMeta, a, b, c, d, next)
}

class ContractRouteSpec5<out A, out B, out C, out D, out E> internal constructor(pathFn: (PathSegments) -> PathSegments, routeMeta: RouteMeta,
                                                                                 val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>, val d: PathLens<D>, val e: PathLens<E>) : ContractRouteSpec(pathFn, routeMeta, a, b, c, d, e) {
    override fun with(new: RouteMeta): ContractRouteSpec5<A, B, C, D, E> = ContractRouteSpec5(pathFn, new, a, b, c, d, e)

    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec6(pathFn, routeMeta, a, b, c, d, e, next)
}

class ContractRouteSpec6<out A, out B, out C, out D, out E, out F> internal constructor(pathFn: (PathSegments) -> PathSegments, routeMeta: RouteMeta,
                                     val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>, val d: PathLens<D>, val e: PathLens<E>, val f: PathLens<F>) : ContractRouteSpec(pathFn, routeMeta, a, b, c, d, e, f) {
    override fun with(new: RouteMeta): ContractRouteSpec6<A, B, C, D, E, F> = ContractRouteSpec6(pathFn, new, a, b, c, d, e, f)

    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec7(pathFn, routeMeta, a, b, c, d, e, f, next)
}

class ContractRouteSpec7<out A, out B, out C, out D, out E, out F, out G> internal constructor(pathFn: (PathSegments) -> PathSegments, routeMeta: RouteMeta,
                                   val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>, val d: PathLens<D>, val e: PathLens<E>,
                                   val f: PathLens<F>, val g: PathLens<G>) : ContractRouteSpec(pathFn, routeMeta, a, b, c, d, e, f, g) {
    override fun with(new: RouteMeta): ContractRouteSpec7<A, B, C, D, E, F, G> = ContractRouteSpec7(pathFn, new, a, b, c, d, e, f, g)

    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec8(pathFn, routeMeta, a, b, c, d, e, f, g, next)
}

class ContractRouteSpec8<out A, out B, out C, out D, out E, out F, out G, out H> internal constructor(pathFn: (PathSegments) -> PathSegments, routeMeta: RouteMeta,
                                   val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>, val d: PathLens<D>, val e: PathLens<E>,
                                   val f: PathLens<F>, val g: PathLens<G>, val h: PathLens<H>) : ContractRouteSpec(pathFn, routeMeta, a, b, c, d, e, f, g, h) {
    override fun with(new: RouteMeta): ContractRouteSpec8<A, B, C, D, E, F, G, H> = ContractRouteSpec8(pathFn, new, a, b, c, d, e, f, g, h)

    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec9(pathFn, routeMeta, a, b, c, d, e, f, g, h, next)
}

class ContractRouteSpec9<out A, out B, out C, out D, out E, out F, out G, out H, out I> internal constructor(pathFn: (PathSegments) -> PathSegments, routeMeta: RouteMeta,
                                  val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>, val d: PathLens<D>, val e: PathLens<E>,
                                  val f: PathLens<F>, val g: PathLens<G>, val h: PathLens<H>, val i: PathLens<I>) : ContractRouteSpec(pathFn, routeMeta, a, b, c, d, e, f, g, h, i) {
    override fun with(new: RouteMeta): ContractRouteSpec9<A, B, C, D, E, F, G, H, I> = ContractRouteSpec9(pathFn, new, a, b, c, d, e, f, g, h, i)

    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec10(pathFn, routeMeta, a, b, c, d, e, f, g, h, i, next)
}

class ContractRouteSpec10<out A, out B, out C, out D, out E, out F, out G, out H, out I, out J> internal constructor(pathFn: (PathSegments) -> PathSegments, routeMeta: RouteMeta,
                                 val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>, val d: PathLens<D>, val e: PathLens<E>,
                                 val f: PathLens<F>, val g: PathLens<G>, val h: PathLens<H>, val i: PathLens<I>,
                                 val j: PathLens<J>) : ContractRouteSpec(pathFn, routeMeta, a, b, c, d, e, f, g, h, i, j) {
    override fun with(new: RouteMeta): ContractRouteSpec10<A, B, C, D, E, F, G, H, I, J> = ContractRouteSpec10(pathFn, new, a, b, c, d, e, f, g, h, i, j)

    override infix operator fun div(next: String) = throw UnsupportedOperationException("no longer paths!")

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = throw UnsupportedOperationException("no longer paths!")
}


