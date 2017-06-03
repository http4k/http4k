package org.http4k

import org.http4k.contract.BasePath
import org.http4k.contract.basePath
import org.http4k.contract.without
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.lens.Header
import org.http4k.lens.LensFailure
import org.http4k.lens.PathLens
import org.http4k.routing.Router

class ServerRoute2 internal constructor(private val method: Method,
                                        private val pathDef: PathDef,
                                        private val toHandler: (ExtractedParts) -> HttpHandler,
                                        private val desc: Desc = Desc()) {

    infix fun describedBy(new: Desc) = ServerRoute2(method, pathDef, toHandler, new)

    internal val nonBodyParams = desc.core.requestParams.plus(pathDef.pathLenses).flatMap { it }

    internal val jsonRequest: Request? = desc.core.request?.let { if (Header.Common.CONTENT_TYPE(it) == ContentType.APPLICATION_JSON) it else null }

    internal val tags = desc.core.tags.toSet().sortedBy { it.name }

    internal fun toRouter(contractRoot: BasePath): Router = object : Router {
        override fun match(request: Request): HttpHandler? =
            if (request.method == method && request.basePath().startsWith(pathDef.pathFn(contractRoot))) {
                try {
                    val without = request.without(pathDef.pathFn(contractRoot))
                    val extract = without.extract(pathDef.pathLenses.toList())
                    extract?.let { desc.core.then(toHandler(it)) }
                } catch (e: LensFailure) {
                    null
                }
            } else null
    }

    internal fun describeFor(contractRoot: BasePath): String = pathDef.describe(contractRoot)
}

internal class ExtractedParts(private val mapping: Map<PathLens<*>, *>) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(lens: PathLens<T>): T = mapping[lens] as T
}

private operator fun <T> BasePath.invoke(index: Int, fn: (String) -> T): T? = toList().let { if (it.size > index) fn(it[index]) else null }

private fun BasePath.extract(lenses: List<PathLens<*>>): ExtractedParts? =
    if (this.toList().size == lenses.size) ExtractedParts(lenses.mapIndexed { index, lens -> lens to this(index, lens::invoke) }.toMap()) else null

