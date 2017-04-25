package org.reekwest.http.contract.spike

import org.reekwest.http.contract.ContractBreach
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method
import org.reekwest.http.core.then

interface PathBinder {
    val route: Route
    val pathFn: (PathBuilder) -> PathBuilder
}

class PathBinder0(override val route: Route, override val pathFn: (PathBuilder) -> PathBuilder) : PathBinder {
    infix fun at(method: Method): RouteBinder<() -> HttpHandler> =
        RouteBinder(this, method, { fn, _, filter -> fn().let { filter.then(it) } })

    infix operator fun div(next: String): PathBinder0 = PathBinder0(route, { pathFn(it) / next })

    infix operator fun <T> div(next: PathLens<T>): PathBinder1<T> = PathBinder1(route, pathFn, next)
}

class PathBinder1<out A>(override val route: Route, override val pathFn: (PathBuilder) -> PathBuilder, private val ppa: PathLens<A>) : PathBinder {

    infix operator fun div(next: String): PathBinder2<A, String> = div(Path.fixed(next))

    infix operator fun <T> div(next: PathLens<T>): PathBinder2<A, T> = PathBinder2(route, pathFn, ppa, next)

    infix fun at(method: Method): RouteBinder<(A) -> HttpHandler?> =
        RouteBinder(this, method, { fn, path, filter ->
            safe { path(0, ppa)?.let { fn(it) }?.let { filter.then(it) } }
        })
}

class PathBinder2<out A, out B>(override val route: Route, override val pathFn: (PathBuilder) -> PathBuilder, private val ppa: PathLens<A>, private val ppb: PathLens<B>) : PathBinder {

    infix fun at(method: Method): RouteBinder<(A, B) -> HttpHandler?> =
        RouteBinder(this, method,
            { fn, path, filter ->
                safe { path(0, ppa) }?.let { path(1, ppb)?.let { b -> fn(it, b) }?.let { filter.then(it) } }
            })
}

private fun <T> safe(fn: () -> T?): T? = try {
    fn()
} catch (e: ContractBreach) {
    null
}