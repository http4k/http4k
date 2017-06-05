package org.http4k.routing

import org.http4k.contract.BasePath
import org.http4k.contract.Root
import org.http4k.contract.basePath
import org.http4k.contract.without
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.lens.Header.Common.CONTENT_TYPE
import org.http4k.lens.LensFailure
import org.http4k.lens.PathLens

class ServerRoute internal constructor(val method: Method,
                                       val routeSpec: RouteSpec,
                                       private val toHandler: (ExtractedParts) -> HttpHandler,
                                       val meta: RouteMeta = RouteMeta()) {

    infix fun with(new: RouteMeta) = ServerRoute(method, routeSpec, toHandler, new)

    internal val nonBodyParams = routeSpec.requestParams.plus(routeSpec.pathLenses).flatMap { it }

    internal val jsonRequest: Request? = meta.request?.let { if (CONTENT_TYPE(it) == APPLICATION_JSON) it else null }

    internal val tags = meta.tags.toSet().sortedBy { it.name }

    fun newRequest(baseUri: Uri): Request = Request(method, "").uri(baseUri.path(routeSpec.describe(Root)))

    internal fun toRouter(contractRoot: BasePath): Router = object : Router {
        override fun match(request: Request): HttpHandler? {
            val startsWith = request.basePath().startsWith(routeSpec.pathFn(contractRoot))
//            println(request.basePath())
//            println(" @ " + routeSpec.pathFn(contractRoot))
            return if (request.method == method && startsWith) {
                try {
                    request.without(routeSpec.pathFn(contractRoot))
                        .extract(routeSpec.pathLenses.toList())
                        ?.let {
                            routeSpec.then(toHandler(it))
                        }
                } catch (e: LensFailure) {
                    null
                }
            } else null
        }
    }

    internal fun describeFor(contractRoot: BasePath): String = routeSpec.describe(contractRoot)
}

internal class ExtractedParts(private val mapping: Map<PathLens<*>, *>) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(lens: PathLens<T>): T = mapping[lens] as T
}

private operator fun <T> BasePath.invoke(index: Int, fn: (String) -> T): T? = toList().let { if (it.size > index) fn(it[index]) else null }

private fun BasePath.extract(lenses: List<PathLens<*>>): ExtractedParts? =
    if (this.toList().size == lenses.size) ExtractedParts(lenses.mapIndexed { index, lens -> lens to this(index, lens::invoke) }.toMap()) else null

