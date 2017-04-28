package org.reekwest.kontrakt.module

import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Method
import org.reekwest.http.core.Request
import org.reekwest.http.core.Status
import org.reekwest.kontrakt.BodyLens
import org.reekwest.kontrakt.HeaderLens
import org.reekwest.kontrakt.Lens
import org.reekwest.kontrakt.PathLens
import org.reekwest.kontrakt.QueryLens
import org.reekwest.kontrakt.module.PathBinder.Companion.Core

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

    infix fun at(method: Method): PathBinder0 = PathBinder0(Core(this, method, { Root }))

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

abstract class ServerRoute(val pathBinder: PathBinder, vararg val pathParams: Lens<String, *>) {

    abstract fun match(basePath: BasePath): (Method, BasePath) -> HttpHandler?

    fun describeFor(basePath: BasePath): String = (pathBinder.core.pathFn(basePath).toString()) + pathParams.map { it.toString() }.joinToString { "/" }
}

internal class ExtractedParts(private val mapping: Map<PathLens<*>, *>) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(lens: PathLens<T>): T = mapping[lens] as T
}

class RouteBinder<in T> internal constructor(private val pathBinder: PathBinder,
                                             private val invoker: (T, ExtractedParts) -> HttpHandler) {

    fun bind(fn: T): ServerRoute = object : ServerRoute(pathBinder) {
        override fun match(basePath: BasePath) =
            {
                actualMethod: Method, actualPath: BasePath ->
                pathBinder.core.matches(actualMethod, basePath, actualPath).let {
                    pathBinder.from(actualPath)?.let { invoker(fn, it) }
                }
            }
    }
}