package org.reekwest.http.contract.spike

import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method
import java.lang.UnsupportedOperationException

interface PathBinder {
    val route: Route
    val pathFn: (PathBuilder) -> PathBuilder
    infix operator fun <T> div(next: PathLens<T>): PathBinder

    infix operator fun div(next: String): PathBinder = div(Path.fixed(next))

}

class PathBinder0(override val route: Route, override val pathFn: (PathBuilder) -> PathBuilder) : PathBinder {

    override infix operator fun div(next: String) = PathBinder0(route, { pathFn(it) / next })

    override infix operator fun <T> div(next: PathLens<T>) = PathBinder1(route, pathFn, next)

    infix fun at(method: Method): RouteBinder<() -> HttpHandler> =
        RouteBinder(this, method, { fn, _ -> fn() })
}

class PathBinder1<out A>(override val route: Route, override val pathFn: (PathBuilder) -> PathBuilder, private val ppa: PathLens<A>) : PathBinder {

    override infix operator fun <T> div(next: PathLens<T>) = PathBinder2(route, pathFn, ppa, next)

    infix fun at(method: Method): RouteBinder<(A) -> HttpHandler> =
        RouteBinder(this, method, { fn, parts -> fn(parts[ppa]) }, ppa)
}

class PathBinder2<out A, out B>(override val route: Route, override val pathFn: (PathBuilder) -> PathBuilder, private val ppa: PathLens<A>, private val ppb: PathLens<B>) : PathBinder {
    override fun <T> div(next: PathLens<T>) = throw UnsupportedOperationException("No support for longer paths!")

    infix fun at(method: Method): RouteBinder<(A, B) -> HttpHandler> =
        RouteBinder(this, method, { fn, parts -> fn(parts[ppa], parts[ppb]) }, ppa, ppb)
}
