package org.http4k.routing

import org.http4k.contract.BasePath
import org.http4k.lens.Path
import org.http4k.lens.PathLens

abstract class PathDef internal constructor(val pathFn: (BasePath) -> BasePath, vararg val pathLenses: PathLens<*>) {
    abstract infix operator fun <T> div(next: PathLens<T>): PathDef

    open infix operator fun div(next: String) = div(Path.fixed(next))

    internal fun describe(contractRoot: BasePath): String = "${pathFn(contractRoot)}${if (pathLenses.isNotEmpty()) "/${pathLenses.joinToString("/")}" else ""}"
}

class PathDef0 internal constructor(pathFn: (BasePath) -> BasePath) : PathDef(pathFn) {
    override infix operator fun div(next: String) = org.http4k.routing.PathDef0 { it / next }

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = PathDef1(pathFn, next)
}

class PathDef1<out A> internal constructor(pathFn: (BasePath) -> BasePath, val a: PathLens<A>) : PathDef(pathFn, a) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = PathDef2(pathFn, a, next)
}

class PathDef2<out A, out B> internal constructor(pathFn: (BasePath) -> BasePath, val a: PathLens<A>, val b: PathLens<B>) : PathDef(pathFn, a, b) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = PathDef3(pathFn, a, b, next)
}

class PathDef3<out A, out B, out C> internal constructor(pathFn: (BasePath) -> BasePath, val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>) : PathDef(pathFn, a, b, c) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = PathDef4(pathFn, a, b, c, next)
}

class PathDef4<out A, out B, out C, out D> internal constructor(pathFn: (BasePath) -> BasePath, val a: PathLens<A>, val b: PathLens<B>, val c: PathLens<C>, val d: PathLens<D>) : PathDef(pathFn, a, b, c, c) {
    override infix operator fun div(next: String) = throw UnsupportedOperationException("no longer paths!")

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = throw UnsupportedOperationException("no longer paths!")
}

