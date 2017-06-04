package org.http4k.routing

import org.http4k.contract.BasePath
import org.http4k.core.Request
import org.http4k.lens.BodyLens
import org.http4k.lens.Lens
import org.http4k.lens.Path
import org.http4k.lens.PathLens

abstract class PathDef internal constructor(val pathFn: (BasePath) -> BasePath,
                                            val requestParams: List<Lens<Request, *>>,
                                            val body: BodyLens<*>?,
                                            vararg val pathLenses: PathLens<*>) {

    abstract infix operator fun plus(new: Lens<Request, *>): PathDef
    abstract infix operator fun plus(new: BodyLens<*>): PathDef

    abstract infix operator fun <T> div(next: PathLens<T>): PathDef

    open infix operator fun div(next: String) = div(Path.fixed(next))

    internal fun describe(contractRoot: BasePath): String = "${pathFn(contractRoot)}${if (pathLenses.isNotEmpty()) "/${pathLenses.joinToString("/")}" else ""}"
}

class PathDef0 internal constructor(pathFn: (BasePath) -> BasePath, requestParams: List<Lens<Request, *>>, body: BodyLens<*>?) : PathDef(pathFn, requestParams, body) {
    override infix fun plus(new: Lens<Request, *>) = PathDef0(pathFn, requestParams.plus(listOf(new)), body)
    override infix fun plus(new: BodyLens<*>) = PathDef0(pathFn, requestParams, new)

    override infix operator fun div(next: String) = PathDef0({ it / next }, emptyList(), body)

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = PathDef1(pathFn, requestParams, body, next)
}

class PathDef1<out A> internal constructor(pathFn: (BasePath) -> BasePath, requestParams: List<Lens<Request, *>>, body: BodyLens<*>?, val a: PathLens<A>) : PathDef(pathFn, requestParams, body, a) {
    override infix fun plus(new: Lens<Request, *>) = PathDef1(pathFn, requestParams.plus(listOf(new)), body, a)
    override infix fun plus(new: BodyLens<*>) = PathDef1(pathFn, requestParams, new, a)

    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = PathDef2(pathFn, requestParams, body, a, next)
}

class PathDef2<out A, out B> internal constructor(pathFn: (BasePath) -> BasePath, requestParams: List<Lens<Request, *>>, body: BodyLens<*>?, val a: PathLens<A>, val b: PathLens<B>) : PathDef(pathFn, requestParams, body, a, b) {
    override infix fun plus(new: Lens<Request, *>) = PathDef2(pathFn, requestParams.plus(listOf(new)), body, a, b)
    override infix fun plus(new: BodyLens<*>) = PathDef2(pathFn, requestParams, new, a, b)

    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = PathDef3(pathFn, requestParams, body, a, b, next)
}

class PathDef3<out A, out B, out C> internal constructor(pathFn: (BasePath) -> BasePath, requestParams: List<Lens<Request, *>>, body: BodyLens<*>?,
                                                         val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>) : PathDef(pathFn, requestParams, body, a, b, c) {
    override infix fun plus(new: Lens<Request, *>) = PathDef3(pathFn, requestParams.plus(listOf(new)), body, a, b, c)
    override infix fun plus(new: BodyLens<*>) = PathDef3(pathFn, requestParams, new, a, b, c)

    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = PathDef4(pathFn, requestParams, body, a, b, c, next)
}

class PathDef4<out A, out B, out C, out D> internal constructor(pathFn: (BasePath) -> BasePath, requestParams: List<Lens<Request, *>>, body: BodyLens<*>?,
                                                                val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>, val d: PathLens<D>) : PathDef(pathFn, requestParams, body, a, b, c, d) {
    override infix fun plus(new: Lens<Request, *>) = PathDef4(pathFn, requestParams.plus(listOf(new)), body, a, b, c, d)
    override infix fun plus(new: BodyLens<*>) = PathDef4(pathFn, requestParams, new, a, b, c, d)

    override infix operator fun div(next: String) = throw UnsupportedOperationException("no longer paths!")

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = throw UnsupportedOperationException("no longer paths!")
}

