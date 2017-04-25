package org.reekwest.http.contract.spike

import org.reekwest.http.contract.PathSegment
import org.reekwest.http.contract.PathSegmentLens
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method
import java.lang.UnsupportedOperationException

interface PathBinder {
    val route: Route
    val pathFn: (Path) -> Path
    infix operator fun <T> div(next: PathSegmentLens<T>): PathBinder

    infix operator fun div(next: String): PathBinder = div(PathSegment.fixed(next))

}

class PathBinder0(override val route: Route, override val pathFn: (Path) -> Path) : PathBinder {

    override infix operator fun div(next: String) = PathBinder0(route, { pathFn(it) / next })

    override infix operator fun <T> div(next: PathSegmentLens<T>) = PathBinder1(route, pathFn, next)

    infix fun at(method: Method): RouteBinder<() -> HttpHandler> =
        RouteBinder(this, method, { fn, _ -> fn() })
}

class PathBinder1<out A>(override val route: Route, override val pathFn: (Path) -> Path,
                         private val psA: PathSegmentLens<A>) : PathBinder {

    override infix operator fun <T> div(next: PathSegmentLens<T>) = PathBinder2(route, pathFn, psA, next)

    infix fun at(method: Method): RouteBinder<(A) -> HttpHandler> =
        RouteBinder(this, method, { fn, parts -> fn(parts[psA]) }, psA)
}

class PathBinder2<out A, out B>(override val route: Route, override val pathFn: (Path) -> Path,
                                private val psA: PathSegmentLens<A>,
                                private val psB: PathSegmentLens<B>) : PathBinder {
    override fun <T> div(next: PathSegmentLens<T>) = PathBinder3(route, pathFn, psA, psB, next)

    infix fun at(method: Method): RouteBinder<(A, B) -> HttpHandler> =
        RouteBinder(this, method, { fn, parts -> fn(parts[psA], parts[psB]) }, psA, psB)
}

class PathBinder3<out A, out B, out C>(override val route: Route, override val pathFn: (Path) -> Path,
                                       private val psA: PathSegmentLens<A>,
                                       private val psB: PathSegmentLens<B>,
                                       private val psC: PathSegmentLens<C>) : PathBinder {
    override fun <T> div(next: PathSegmentLens<T>) = PathBinder4(route, pathFn, psA, psB, psC, next)

    infix fun at(method: Method): RouteBinder<(A, B, C) -> HttpHandler> =
        RouteBinder(this, method, { fn, parts -> fn(parts[psA], parts[psB], parts[psC]) }, psA, psB, psC)
}

class PathBinder4<out A, out B, out C, out D>(override val route: Route, override val pathFn: (Path) -> Path,
                                       private val psA: PathSegmentLens<A>,
                                       private val psB: PathSegmentLens<B>,
                                       private val psC: PathSegmentLens<C>,
                                       private val psD: PathSegmentLens<D>) : PathBinder {
    override fun <T> div(next: PathSegmentLens<T>) = throw UnsupportedOperationException("No support for longer paths!")

    infix fun at(method: Method): RouteBinder<(A, B, C, D) -> HttpHandler> =
        RouteBinder(this, method, { fn, parts -> fn(parts[psA], parts[psB], parts[psC], parts[psD]) }, psA, psB, psC, psD)
}
