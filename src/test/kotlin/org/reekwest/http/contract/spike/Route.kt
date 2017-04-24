package org.reekwest.http.contract.spike

import org.reekwest.http.contract.Lens
import org.reekwest.http.contract.spike.p2.APath
import org.reekwest.http.contract.spike.p2.Root
import org.reekwest.http.core.*

data class RouteResponse(val status: Status, val description: String?, val example: String?)

data class Route private constructor(private val name: String,
                                     private val description: String?,
                                     private val body: Lens<Request, *>?,
                                     private val produces: Set<ContentType> = emptySet(),
                                     private val consumes: Set<ContentType> = emptySet(),
                                     private val requestParams: Iterable<Lens<Request, *>> = emptyList(),
                                     private val responses: Iterable<RouteResponse> = emptyList()) {

    constructor(name: String, description: String? = null) : this(name, description, null)

    fun taking(new: Lens<Request, *>) = copy(requestParams = requestParams.plus(new))
    fun body(new: Lens<HttpMessage, *>) = copy(body = new)
    fun returning(new: Pair<Status, String>, description: String? = null) = copy(responses = responses.plus(RouteResponse(new.first, new.second, description)))
    fun producing(vararg new: ContentType) = copy(produces = produces.plus(new))
    fun consuming(vararg new: ContentType) = copy(consumes = consumes.plus(new))

    infix operator fun div(next: String): PathBuilder0 = PathBuilder0(this, { Root / next })
    infix operator fun <T> div(next: Lens<String, T>) = PathBuilder0(this, { Root }) / next

}


abstract class ServerRoute(val pathBuilder: PathBuilder, val method: Method, vararg val pathParams: Lens<String, *>) {

    fun matches(actualMethod: Method, basePath: APath, actualPath: APath): Boolean? = actualMethod == method && actualPath == pathBuilder.pathFn(basePath)

    abstract fun match(filter: Filter, basePath: APath): (Method, APath) -> HttpHandler?

    fun describeFor(basePath: APath): String = (pathBuilder.pathFn(basePath).toString()) + pathParams.map { it.toString() }.joinToString { "/" }
}

class RouteBinder<in T>(private val pathBuilder: PathBuilder,
                        private val method: Method,
                        private val invoker: (T, APath, Filter) -> HttpHandler?) {
     infix fun bind(fn: T): ServerRoute = object : ServerRoute(pathBuilder, method) {
        override fun match(filter: Filter, basePath: APath) =
            {
                actualMethod: Method, actualPath: APath ->
                matches(actualMethod, basePath, actualPath)?.let { invoker(fn, actualPath, filter) }
            }
    }
}
