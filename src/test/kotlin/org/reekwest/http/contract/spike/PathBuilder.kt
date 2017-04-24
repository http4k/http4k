package org.reekwest.http.contract.spike

import org.reekwest.http.contract.spike.p2.APath
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method
import org.reekwest.http.core.then

interface PathBuilder {
    val route: Route
    val pathFn: (APath) -> APath
}

class PathBuilder0(override val route: Route, override val pathFn: (APath) -> APath) : PathBuilder {
    infix fun at(method: Method): RouteBinder<() -> HttpHandler> =
        RouteBinder(this, method, { fn, _, filter -> fn().let { filter.then(it) } })

    infix operator fun div(next: String): PathBuilder0 = PathBuilder0(route, { pathFn(it) / next })

    infix operator fun <T> div(next: PathLens<T>): PathBuilder1<T> = PathBuilder1(route, pathFn, next)
}

class PathBuilder1<out A>(override val route: Route, override val pathFn: (APath) -> APath, private val ppa: PathLens<A>) : PathBuilder {

    infix operator fun div(next: String): PathBuilder2<A, String> = div(Path.fixed(next))

    infix operator fun <T> div(next: PathLens<T>): PathBuilder2<A, T> = PathBuilder2(route, pathFn, ppa, next)

    infix fun at(method: Method): RouteBinder<(A) -> HttpHandler?> =
        RouteBinder(this, method, { fn, path, filter -> ppa(path.toString())?.let { fn(it) }?.let { filter.then(it) } })
}

class PathBuilder2<out A, out B>(override val route: Route, override val pathFn: (APath) -> APath, private val ppa: PathLens<A>, private val ppb: PathLens<B>) : PathBuilder {

    infix fun at(method: Method): RouteBinder<(A, B) -> HttpHandler?> =
        RouteBinder(this, method, { fn, path, filter ->
            ppa(path.toString())?.
                let { ai ->
                    ppb(path.toString())?.
                        let { ai to it }
                }?.
                let { fn(it.first, it.second) }?.let { filter.then(it) }
        })
}
