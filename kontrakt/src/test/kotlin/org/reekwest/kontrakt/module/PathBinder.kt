package org.reekwest.kontrakt.module

import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method
import org.reekwest.http.core.Request
import org.reekwest.http.core.then
import org.reekwest.kontrakt.ContractBreach
import org.reekwest.kontrakt.Path
import org.reekwest.kontrakt.PathLens

class ServerRoute internal constructor(internal val pathBinder: PathBinder, private val toHandler: (ExtractedParts) -> HttpHandler) {
    fun router(moduleRoot: BasePath): Router = pathBinder.toRouter(moduleRoot, toHandler)

    fun describeFor(basePath: BasePath): String = pathBinder.describe(basePath)
}

internal class ExtractedParts private constructor(private val mapping: Map<PathLens<*>, *>) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(lens: PathLens<T>): T = mapping[lens] as T

    companion object {
        fun from(path: BasePath, lenses: List<PathLens<*>>) = if (path.toList().size == lenses.size)
            ExtractedParts(lenses.mapIndexed { index, lens -> lens to path(index, lens) }.toMap())
        else null
    }
}

abstract class PathBinder internal constructor(internal val core: Core, vararg val pathLenses: PathLens<*>) {
    abstract infix operator fun <T> div(next: PathLens<T>): PathBinder

    open infix operator fun div(next: String): PathBinder = div(Path.fixed(next))

    internal fun toRouter(moduleRoot: BasePath, toHandler: (ExtractedParts) -> HttpHandler): Router =
        {
            if (core.matches(moduleRoot, it)) {
                try {
                    ExtractedParts.from(BasePath(it.uri.path), pathLenses.toList())
                        ?.let { core.route.validationFilter.then(toHandler(it)) }
                } catch (e: ContractBreach) {
                    null
                }
            } else null
        }

    fun describe(basePath: BasePath) = (core.pathFn(basePath).toString()) + pathLenses.map { it.toString() }.joinToString { "/" }

    companion object {
        internal data class Core(val route: Route, val method: Method, val pathFn: (BasePath) -> BasePath) {
            infix operator fun div(next: String) = copy(pathFn = { pathFn(it) / next })
            fun matches(moduleRoot: BasePath, request: Request): Boolean {
                return request.method == method && BasePath(request.uri.path).startsWith(pathFn(moduleRoot))
            }
        }
    }
}

class PathBinder0 internal constructor(core: Core) : PathBinder(core) {

    override infix operator fun div(next: String) = PathBinder0(core / next)

    override infix operator fun <T> div(next: PathLens<T>) = PathBinder1(core, next)

    infix fun bind(handler: HttpHandler) = ServerRoute(this, { handler })
}

class PathBinder1<out A> internal constructor(core: Core,
                                              private val psA: PathLens<A>) : PathBinder(core, psA) {

    override infix operator fun <T> div(next: PathLens<T>) = PathBinder2(core, psA, next)

    infix fun bind(fn: (A) -> HttpHandler) = ServerRoute(this, { parts -> fn(parts[psA]) })
}

class PathBinder2<out A, out B> internal constructor(core: Core,
                                                     private val psA: PathLens<A>,
                                                     private val psB: PathLens<B>) : PathBinder(core, psA, psB) {
    override fun <T> div(next: PathLens<T>) = throw UnsupportedOperationException("No support for longer paths!")

    infix fun bind(fn: (A, B) -> HttpHandler) = ServerRoute(this, { parts -> fn(parts[psA], parts[psB]) })
}
//
//class PathBinder3<out A, out B, out C>(override val route: Route, override val pathFn: (BasePath) -> BasePath,
//                                       private val psA: PathLens<A>,
//                                       private val psB: PathLens<B>,
//                                       private val psC: PathLens<C>) : PathBinder {
//    override fun <T> div(next: PathLens<T>) = PathBinder4(route, pathFn, psA, psB, psC, next)
//
//    infix fun at(method: Method): RouteBinder<(A, B, C) -> HttpHandler> =
//        RouteBinder(this, method, { fn, parts -> fn(parts[psA], parts[psB], parts[psC]) }, psA, psB, psC)
//}
//
//class PathBinder4<out A, out B, out C, out D>(override val route: Route, override val pathFn: (BasePath) -> BasePath,
//                                              private val psA: PathLens<A>,
//                                              private val psB: PathLens<B>,
//                                              private val psC: PathLens<C>,
//                                              private val psD: PathLens<D>) : PathBinder {
//    override fun <T> div(next: PathLens<T>) = throw UnsupportedOperationException("No support for longer paths!")
//
//    infix fun at(method: Method): RouteBinder<(A, B, C, D) -> HttpHandler> =
//        RouteBinder(this, method, { fn, parts -> fn(parts[psA], parts[psB], parts[psC], parts[psD]) }, psA, psB, psC, psD)
//}
