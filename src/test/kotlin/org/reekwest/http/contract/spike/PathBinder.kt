package org.reekwest.http.contract.spike

import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method

interface PathBinder {
    val route: Route
    val pathFn: (PathBuilder) -> PathBuilder
}

class PathBinder0(override val route: Route, override val pathFn: (PathBuilder) -> PathBuilder) : PathBinder {
    infix fun at(method: Method): RouteBinder<() -> HttpHandler> =
        RouteBinder(this, method, { fn, _ -> fn() })

    infix operator fun div(next: String) = PathBinder0(route, { pathFn(it) / next })

    infix operator fun <T> div(next: PathLens<T>) = PathBinder1(route, pathFn, next)
}

class PathBinder1<out A>(override val route: Route, override val pathFn: (PathBuilder) -> PathBuilder, private val ppa: PathLens<A>) : PathBinder {

    infix operator fun div(next: String) = div(Path.fixed(next))

    infix operator fun <T> div(next: PathLens<T>) = PathBinder2(route, pathFn, ppa, next)

    infix fun at(method: Method): RouteBinder<(A) -> HttpHandler> =
        RouteBinder(this, method, { fn, parts -> fn(parts[ppa]) }, ppa)
}

class PathBinder2<out A, out B>(override val route: Route, override val pathFn: (PathBuilder) -> PathBuilder, private val ppa: PathLens<A>, private val ppb: PathLens<B>) : PathBinder {
    infix fun at(method: Method): RouteBinder<(A, B) -> HttpHandler> =
        RouteBinder(this, method, { fn, parts -> fn(parts[ppa], parts[ppb]) }, ppa, ppb)
}
