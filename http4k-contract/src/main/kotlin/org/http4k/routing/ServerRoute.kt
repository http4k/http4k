package org.http4k.routing

import org.http4k.contract.PathSegments
import org.http4k.contract.Root
import org.http4k.contract.pathSegments
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

class ServerRoute internal constructor(internal val method: Method,
                                       internal val routeSpec: RouteSpec,
                                       internal val toHandler: (ExtractedParts) -> HttpHandler,
                                       internal val meta: RouteMeta = RouteMeta()) {

    internal val nonBodyParams = routeSpec.requestParams.plus(routeSpec.pathLenses).flatMap { it }

    internal val jsonRequest: Request? = meta.request?.let { if (CONTENT_TYPE(it) == APPLICATION_JSON) it else null }

    internal val tags = meta.tags.toSet().sortedBy { it.name }

    fun newRequest(baseUri: Uri): Request = Request(method, "").uri(baseUri.path(routeSpec.describe(Root)))

    internal fun toRouter(contractRoot: PathSegments): Router = object : Router {

        override fun toString(): String = "${method.name}: ${routeSpec.describe(contractRoot)}"

        override fun match(request: Request): HttpHandler? =
            if (request.method == method && request.pathSegments().startsWith(routeSpec.pathFn(contractRoot))) {
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

    internal fun describeFor(contractRoot: PathSegments): String = routeSpec.describe(contractRoot)

    override fun toString(): String = "${method.name}: ${routeSpec.describe(Root)}"
}

internal class ExtractedParts(private val mapping: Map<PathLens<*>, *>) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(lens: PathLens<T>): T = mapping[lens] as T
}

private operator fun <T> PathSegments.invoke(index: Int, fn: (String) -> T): T? = toList().let { if (it.size > index) fn(it[index]) else null }

private fun PathSegments.extract(lenses: List<PathLens<*>>): ExtractedParts? =
    if (this.toList().size == lenses.size) ExtractedParts(lenses.mapIndexed { index, lens -> lens to this(index, lens::invoke) }.toMap()) else null

