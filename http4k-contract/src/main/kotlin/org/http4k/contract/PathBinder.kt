package org.http4k.contract

import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.lens.Header
import org.http4k.lens.LensFailure
import org.http4k.lens.Path
import org.http4k.lens.PathLens
import org.http4k.routing.Router

class ServerRoute internal constructor(private val pathBinder: PathBinder, private val toHandler: (ExtractedParts) -> HttpHandler) {
    internal val core = pathBinder.core.route.core
    internal val method = pathBinder.core.method
    internal val nonBodyParams = core.requestParams.plus(pathBinder.pathLenses).flatMap { it }

    internal val jsonRequest: Request? = core.request?.let { if (Header.Common.CONTENT_TYPE(it) == ContentType.APPLICATION_JSON) it else null }

    internal val tags = core.tags.toSet().sortedBy { it.name }

    internal fun router(contractRoot: BasePath): Router = pathBinder.toRouter(contractRoot, toHandler)

    internal fun describeFor(contractRoot: BasePath): String = pathBinder.describe(contractRoot)
}

abstract class PathBinder internal constructor(internal val core: Core, internal vararg val pathLenses: PathLens<*>) {
    abstract infix operator fun <T> div(next: PathLens<T>): PathBinder

    open infix operator fun div(next: String) = div(Path.fixed(next))

    internal fun toRouter(contractRoot: BasePath, toHandler: (ExtractedParts) -> HttpHandler): Router = object : Router {
        override fun match(request: Request): HttpHandler? = core.matches(contractRoot, request, pathLenses.toList(), toHandler)
    }

    internal fun describe(contractRoot: BasePath): String {
        return "${core.pathFn(contractRoot)}${if (pathLenses.isNotEmpty()) "/${pathLenses.joinToString("/")}" else ""}"
    }

    fun newRequest(baseUri: Uri = Uri.of("")): Request = Request(core.method, "").uri(baseUri.path(describe(Root)))

    companion object {
        internal data class Core(val route: Route, val method: Method, val pathFn: (BasePath) -> BasePath) {
            infix operator fun div(next: String) = copy(pathFn = { pathFn(it) / next })

            fun matches(contractRoot: BasePath, request: Request, lenses: List<PathLens<*>>, toHandler: (ExtractedParts) -> HttpHandler): HttpHandler? =
                if (request.method == method && request.basePath().startsWith(pathFn(contractRoot))) {
                    try {
                        request.without(pathFn(contractRoot)).extract(lenses)?.let { route.core.validationFilter.then(toHandler(it)) }
                    } catch (e: LensFailure) {
                        null
                    }
                } else null
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

    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <T> div(next: PathLens<T>) = PathBinder2(core, psA, next)

    infix fun bind(fn: (A) -> HttpHandler) = ServerRoute(this, { parts -> fn(parts[psA]) })
}

class PathBinder2<out A, out B> internal constructor(core: Core,
                                                     private val psA: PathLens<A>,
                                                     private val psB: PathLens<B>) : PathBinder(core, psA, psB) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <T> div(next: PathLens<T>) = PathBinder3(core, psA, psB, next)

    infix fun bind(fn: (A, B) -> HttpHandler) = ServerRoute(this, { parts -> fn(parts[psA], parts[psB]) })
}

class PathBinder3<out A, out B, out C> internal constructor(core: Core,
                                                            private val psA: PathLens<A>,
                                                            private val psB: PathLens<B>,
                                                            private val psC: PathLens<C>) : PathBinder(core, psA, psB, psC) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <T> div(next: PathLens<T>) = PathBinder4(core, psA, psB, psC, next)

    infix fun bind(fn: (A, B, C) -> HttpHandler) = ServerRoute(this, { parts -> fn(parts[psA], parts[psB], parts[psC]) })
}

class PathBinder4<out A, out B, out C, out D> internal constructor(core: Core,
                                                                   private val psA: PathLens<A>,
                                                                   private val psB: PathLens<B>,
                                                                   private val psC: PathLens<C>,
                                                                   private val psD: PathLens<D>) : PathBinder(core, psA, psB, psC, psD) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <T> div(next: PathLens<T>) = throw UnsupportedOperationException("No support for longer paths!")

    infix fun bind(fn: (A, B, C, D) -> HttpHandler) = ServerRoute(this, { parts -> fn(parts[psA], parts[psB], parts[psC], parts[psD]) })
}

internal class ExtractedParts(private val mapping: Map<PathLens<*>, *>) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(lens: PathLens<T>): T = mapping[lens] as T
}

private operator fun <T> BasePath.invoke(index: Int, fn: (String) -> T): T? = toList().let { if (it.size > index) fn(it[index]) else null }

private fun BasePath.extract(lenses: List<PathLens<*>>): ExtractedParts? =
    if (this.toList().size == lenses.size) ExtractedParts(lenses.mapIndexed { index, lens -> lens to this(index, lens::invoke) }.toMap()) else null
