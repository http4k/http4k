package org.reekwest.http.contract.spike

import org.reekwest.http.contract.Path
import org.reekwest.http.contract.PathLens
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method
import java.lang.UnsupportedOperationException

interface PathBinder {
    val route: Route
    val pathFn: (BasePath) -> BasePath
    infix operator fun <T> div(next: PathLens<T>): PathBinder

    infix operator fun div(next: String): PathBinder = div(Path.fixed(next))

}

class PathBinder0(override val route: Route, override val pathFn: (BasePath) -> BasePath) : PathBinder {

    override infix operator fun div(next: String) = PathBinder0(route, { pathFn(it) / next })

    override infix operator fun <T> div(next: PathLens<T>) = PathBinder1(route, pathFn, next)

    infix fun at(method: Method): RouteBinder<() -> HttpHandler> =
        RouteBinder(this, method, { fn, _ -> fn() })
}

class PathBinder1<out A>(override val route: Route, override val pathFn: (BasePath) -> BasePath,
                         private val psA: PathLens<A>) : PathBinder {

    override infix operator fun <T> div(next: PathLens<T>) = PathBinder2(route, pathFn, psA, next)

    infix fun at(method: Method): RouteBinder<(A) -> HttpHandler> =
        RouteBinder(this, method, { fn, parts -> fn(parts[psA]) }, psA)
}

class PathBinder2<out A, out B>(override val route: Route, override val pathFn: (BasePath) -> BasePath,
                                private val psA: PathLens<A>,
                                private val psB: PathLens<B>) : PathBinder {
    override fun <T> div(next: PathLens<T>) = PathBinder3(route, pathFn, psA, psB, next)

    infix fun at(method: Method): RouteBinder<(A, B) -> HttpHandler> =
        RouteBinder(this, method, { fn, parts -> fn(parts[psA], parts[psB]) }, psA, psB)
}

class PathBinder3<out A, out B, out C>(override val route: Route, override val pathFn: (BasePath) -> BasePath,
                                       private val psA: PathLens<A>,
                                       private val psB: PathLens<B>,
                                       private val psC: PathLens<C>) : PathBinder {
    override fun <T> div(next: PathLens<T>) = PathBinder4(route, pathFn, psA, psB, psC, next)

    infix fun at(method: Method): RouteBinder<(A, B, C) -> HttpHandler> =
        RouteBinder(this, method, { fn, parts -> fn(parts[psA], parts[psB], parts[psC]) }, psA, psB, psC)
}

class PathBinder4<out A, out B, out C, out D>(override val route: Route, override val pathFn: (BasePath) -> BasePath,
                                              private val psA: PathLens<A>,
                                              private val psB: PathLens<B>,
                                              private val psC: PathLens<C>,
                                              private val psD: PathLens<D>) : PathBinder {
    override fun <T> div(next: PathLens<T>) = throw UnsupportedOperationException("No support for longer paths!")

    infix fun at(method: Method): RouteBinder<(A, B, C, D) -> HttpHandler> =
        RouteBinder(this, method, { fn, parts -> fn(parts[psA], parts[psB], parts[psC], parts[psD]) }, psA, psB, psC, psD)
}
