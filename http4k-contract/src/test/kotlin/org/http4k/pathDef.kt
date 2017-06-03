package org.http4k

import org.http4k.contract.BasePath
import org.http4k.lens.Path
import org.http4k.lens.PathLens

abstract class PathDef internal constructor(val pathFn: (BasePath) -> BasePath, vararg val pathLenses: PathLens<*>) {
    abstract infix operator fun <T> div(next: PathLens<T>): PathDef

    open infix operator fun div(next: String) = div(Path.fixed(next))

    internal fun describe(contractRoot: BasePath): String = "${pathFn(contractRoot)}${if (pathLenses.isNotEmpty()) "/${pathLenses.joinToString("/")}" else ""}"
}

class PathDef0 internal constructor(pathFn: (BasePath) -> BasePath) : PathDef(pathFn) {

    override infix operator fun div(next: String) = PathDef0 { it }

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = PathDef1(pathFn, next)
}

class PathDef1<out A> internal constructor(pathFn: (BasePath) -> BasePath, val a: PathLens<A>) : PathDef(pathFn) {
    override infix operator fun div(next: String) = throw UnsupportedOperationException("no longer paths!")

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = throw UnsupportedOperationException("no longer paths!")
}

