package org.http4k

import org.http4k.contract.BasePath
import org.http4k.contract.basePath
import org.http4k.contract.without
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.lens.LensFailure
import org.http4k.lens.PathLens
import org.http4k.routing.Router

abstract class SBB(val method: Method, val pathDef: PathDef, val desc: Desc) {
    abstract infix fun describedBy(new: Desc): SBB
    abstract fun toServerRoute(): ServerRoute2

    internal fun toRouter(contractRoot: BasePath, toHandler: (ExtractedParts) -> HttpHandler): Router = object : Router {
        override fun match(request: Request): HttpHandler? = matches(contractRoot, request, desc.core.validationFilter, pathDef.pathLenses.toList(), toHandler)
    }

    internal fun matches(contractRoot: BasePath, request: Request,
                         validationFilter: Filter,
                         lenses: List<PathLens<*>>,
                         toHandler: (ExtractedParts) -> HttpHandler): HttpHandler? =
        if (request.method == method && request.basePath().startsWith(pathDef.pathFn(contractRoot))) {
            try {
                request.without(pathDef.pathFn(contractRoot)).extract(lenses)?.let { validationFilter.then(toHandler(it)) }
            } catch (e: LensFailure) {
                null
            }
        } else null
}

class SBB0(method: Method, pathDef: PathDef, desc: Desc, private val fn: HttpHandler) : SBB(method, pathDef, desc) {
    override infix fun describedBy(new: Desc): SBB = SBB0(method, pathDef, desc, fn)
    override fun toServerRoute(): ServerRoute2 = ServerRoute2(this, { fn })
}

class SBB1<A>(method: Method, pathDef: PathDef, desc: Desc, private val fn: (A) -> HttpHandler, private val psA: PathLens<A>) : SBB(method, pathDef, desc) {
    override infix fun describedBy(new: Desc): SBB = SBB1(method, pathDef, desc, fn, psA)
    override fun toServerRoute(): ServerRoute2 = ServerRoute2(this, { parts -> fn(parts[psA]) })
}

internal class ExtractedParts(private val mapping: Map<PathLens<*>, *>) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(lens: PathLens<T>): T = mapping[lens] as T
}

private operator fun <T> BasePath.invoke(index: Int, fn: (String) -> T): T? = toList().let { if (it.size > index) fn(it[index]) else null }

private fun BasePath.extract(lenses: List<PathLens<*>>): ExtractedParts? =
    if (this.toList().size == lenses.size) ExtractedParts(lenses.mapIndexed { index, lens -> lens to this(index, lens::invoke) }.toMap()) else null
