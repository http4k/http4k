package org.reekwest.http.contract.spike

import org.reekwest.http.contract.BodyLens
import org.reekwest.http.contract.HeaderLens
import org.reekwest.http.contract.Lens
import org.reekwest.http.contract.QueryLens
import org.reekwest.http.core.*

data class RouteResponse(val status: Status, val description: String?, val example: String?)

data class Route private constructor(private val name: String,
                                     private val description: String?,
                                     private val body: BodyLens<*>?,
                                     private val produces: Set<ContentType> = emptySet(),
                                     private val consumes: Set<ContentType> = emptySet(),
                                     private val requestParams: Iterable<Lens<Request, *>> = emptyList(),
                                     private val responses: Iterable<RouteResponse> = emptyList()) {

    constructor(name: String, description: String? = null) : this(name, description, null)

    fun header(new: HeaderLens<*>) = copy(requestParams = requestParams.plus(new))
    fun query(new: QueryLens<*>) = copy(requestParams = requestParams.plus(new))
    fun body(new: Lens<HttpMessage, *>) = copy(body = new)
    fun returning(new: Pair<Status, String>, description: String? = null) = copy(responses = responses.plus(RouteResponse(new.first, new.second, description)))
    fun producing(vararg new: ContentType) = copy(produces = produces.plus(new))
    fun consuming(vararg new: ContentType) = copy(consumes = consumes.plus(new))

    infix operator fun div(next: String): PathBinder0 = PathBinder0(this, { Root / next })
    infix operator fun <T> div(next: Lens<String, T>) = PathBinder0(this, { Root }) / next

}


abstract class ServerRoute(val pathBuilder: PathBinder, val method: Method, vararg val pathParams: Lens<String, *>) {

    fun matches(actualMethod: Method, basePath: PathBuilder, actualPath: PathBuilder): Boolean? = actualMethod == method && actualPath == pathBuilder.pathFn(basePath)

    abstract fun match(filter: Filter, basePath: PathBuilder): (Method, PathBuilder) -> HttpHandler?

    fun describeFor(basePath: PathBuilder): String = (pathBuilder.pathFn(basePath).toString()) + pathParams.map { it.toString() }.joinToString { "/" }
}

class RouteBinder<in T>(private val pathExtractor: PathExtractor,
                        private val pathBuilder: PathBinder,
                        private val method: Method,
                        private val invoker: (T, ExtractedParts, Filter) -> HttpHandler?) {
    infix fun bind(fn: T): ServerRoute = object : ServerRoute(pathBuilder, method) {
        override fun match(filter: Filter, basePath: PathBuilder) =
            {
                actualMethod: Method, actualPath: PathBuilder ->
                matches(actualMethod, basePath, actualPath)?.let {
                    pathExtractor.from(actualPath)?.let { invoker(fn, it, filter) }
                }
            }
    }
}