package org.reekwest.kontrakt

import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Method
import org.reekwest.http.core.Request
import org.reekwest.http.core.Status

data class RouteResponse(val status: Status, val description: String?, val example: String?)

class Route private constructor(private val core: Core) : Iterable<Lens<Request, *>> {
    constructor(name: String, description: String? = null) : this(Core(name, description, null))

    override fun iterator(): Iterator<Lens<Request, *>> = core.requestParams.plus(core.body?.let { listOf(it) } ?: emptyList<Lens<Request, *>>()).iterator()

    fun header(new: HeaderLens<*>) = Route(core.copy(requestParams = core.requestParams.plus(new)))
    fun query(new: QueryLens<*>) = Route(core.copy(requestParams = core.requestParams.plus(new)))
    fun body(new: Lens<HttpMessage, *>) = Route(core.copy(body = new))
    fun returning(new: Pair<Status, String>, description: String? = null) = Route(core.copy(responses = core.responses.plus(RouteResponse(new.first, new.second, description))))
    fun producing(vararg new: ContentType) = Route(core.copy(produces = core.produces.plus(new)))
    fun consuming(vararg new: ContentType) = Route(core.copy(consumes = core.consumes.plus(new)))

    infix operator fun div(next: String): PathBinder0 = PathBinder0(this, { Root / next })
    infix operator fun <T> div(next: Lens<String, T>) = PathBinder0(this, { Root }) / next

    companion object {
        private data class Core(val name: String,
                                val description: String?,
                                val body: BodyLens<*>?,
                                val produces: kotlin.collections.Set<ContentType> = emptySet(),
                                val consumes: kotlin.collections.Set<ContentType> = emptySet(),
                                val requestParams: List<Lens<Request, *>> = emptyList(),
                                val responses: List<RouteResponse> = emptyList())
    }
}

abstract class ServerRoute(val pathBuilder: PathBinder, val method: Method, vararg val pathParams: Lens<String, *>) {

    abstract fun match(basePath: BasePath): (Method, BasePath) -> HttpHandler?

    fun describeFor(basePath: BasePath): String = (pathBuilder.pathFn(basePath).toString()) + pathParams.map { it.toString() }.joinToString { "/" }
}

internal class ExtractedParts(private val mapping: Map<PathLens<*>, *>) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(lens: PathLens<T>): T = mapping[lens] as T
}

class RouteBinder<in T> internal constructor(private val pathBinder: PathBinder,
                                             private val method: Method,
                                             private val invoker: (T, ExtractedParts) -> HttpHandler,
                                             private vararg val pathLenses: PathLens<*>) {

    private fun matches(actualMethod: Method, basePath: BasePath, actualPath: BasePath) = actualMethod == method && actualPath == pathBinder.pathFn(basePath)

    infix fun bind(fn: T): ServerRoute = object : ServerRoute(pathBinder, method) {
        override fun match(basePath: BasePath) =
            {
                actualMethod: Method, actualPath: BasePath ->
                matches(actualMethod, basePath, actualPath)?.let {
                    from(actualPath)?.let { invoker(fn, it) }
                }
            }
    }

    private fun from(path: BasePath) = try {
        if (path.toList().size == pathLenses.size) {
            ExtractedParts(mapOf(*pathLenses.mapIndexed { index, lens -> lens to path(index, lens) }.toTypedArray()))
        } else {
            null
        }
    } catch (e: ContractBreach) {
        null
    }
}