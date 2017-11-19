package org.http4k.contract


import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.lens.Header.Common.CONTENT_TYPE
import org.http4k.lens.LensFailure
import org.http4k.lens.PathLens
import org.http4k.routing.Router

class ContractRoute internal constructor(internal val method: Method,
                                         internal val spec: ContractRouteSpec,
                                         internal val meta: RouteMeta,
                                         internal val toHandler: (ExtractedParts) -> HttpHandler) {

    internal val nonBodyParams = meta.requestParams.plus(spec.pathLenses).flatMap { it }

    internal val jsonRequest: Request? = meta.request?.let { if (CONTENT_TYPE(it) == APPLICATION_JSON) it else null }

    internal val tags = meta.tags.toSet().sortedBy { it.name }

    fun newRequest(baseUri: Uri): Request = Request(method, "").uri(baseUri.path(spec.describe(Root)))

    internal fun toRouter(contractRoot: PathSegments): Router = object : Router {

        override fun toString(): String = "${method.name}: ${spec.describe(contractRoot)}"

        override fun match(request: Request): HttpHandler? =
            if (request.method == method && request.pathSegments().startsWith(spec.pathFn(contractRoot))) {
                try {
                    request.without(spec.pathFn(contractRoot))
                        .extract(spec.pathLenses.toList())
                        ?.let {
                            spec.then(toHandler(it))
                        }
                } catch (e: LensFailure) {
                    null
                }
            } else null
    }

    internal fun describeFor(contractRoot: PathSegments): String = spec.describe(contractRoot)

    override fun toString(): String = "${method.name}: ${spec.describe(Root)}"
}

internal class ExtractedParts(private val mapping: Map<PathLens<*>, *>) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(lens: PathLens<T>): T = mapping[lens] as T
}

private operator fun <T> PathSegments.invoke(index: Int, fn: (String) -> T): T? = toList().let { if (it.size > index) fn(it[index]) else null }

private fun PathSegments.extract(lenses: List<PathLens<*>>): ExtractedParts? =
    if (this.toList().size == lenses.size) ExtractedParts(lenses.mapIndexed { index, lens -> lens to this(index, lens::invoke) }.toMap()) else null

