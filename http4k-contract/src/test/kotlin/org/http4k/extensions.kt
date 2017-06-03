package org.http4k

import org.http4k.SBB.Companion.Core
import org.http4k.contract.BasePath
import org.http4k.contract.ContractRenderer
import org.http4k.contract.ContractRoutingHttpHandler
import org.http4k.contract.NoRenderer
import org.http4k.contract.NoSecurity
import org.http4k.contract.Security
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.lens.Path
import org.http4k.lens.PathLens
import org.http4k.routing.Router
import org.http4k.contract.ContractRoutingHttpHandler.Companion.Handler as ContractHandler

infix fun String.by(router: ContractRoutingHttpHandler): ContractRoutingHttpHandler = router.withBasePath(this)

interface ContractBuilder {
    operator fun invoke(vararg sbbs: SBB): ContractRoutingHttpHandler
}

fun cont(renderer: ContractRenderer = NoRenderer, descriptionPath: String = "", security: Security = NoSecurity) =
    object : ContractBuilder {
        override fun invoke(vararg sbbs: SBB): ContractRoutingHttpHandler = TODO()
    }

class ServerRoute2 internal constructor(private val sbb: SBB, private val toHandler: (ExtractedParts) -> HttpHandler) {
    internal fun router(contractRoot: BasePath): Router = sbb.toRouter(contractRoot, toHandler)
}

abstract class PB internal constructor(val core: PCore, vararg val pathLenses: PathLens<*>) {
    abstract infix operator fun <T> div(next: PathLens<T>): PB

    open infix operator fun div(next: String) = div(Path.fixed(next))
    internal data class PCore(val pathFn: (BasePath) -> BasePath) {
        infix operator fun div(next: String) = copy(pathFn = { pathFn(it) / next })
    }

}

class PB0 internal constructor(pathFn: (BasePath) -> BasePath) : PB(PCore(pathFn)) {

    override infix operator fun div(next: String) = PB0 { it }

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = PB1(core, next)
}

class PB1<out A> internal constructor(core: PCore, val a: PathLens<A>) : PB(core) {
    override infix operator fun div(next: String) = throw UnsupportedOperationException("no longer paths!")

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = throw UnsupportedOperationException("no longer paths!")
}

operator fun <A> String.div(next: PathLens<A>): PB1<A> = PB0 { it } / next

abstract class SBB(val core: Core, val desc: Desc) {
    abstract infix fun describedBy(new: Desc): SBB
    abstract fun toServerRoute(): ServerRoute2

    internal fun toRouter(contractRoot: BasePath, toHandler: (ExtractedParts) -> HttpHandler): Router = object : Router {
        override fun match(request: Request): HttpHandler? = TODO()
    }

    companion object {
        data class Core(val method: Method, val pb: PB)
    }
}

class SBB0(core: Core, desc: Desc, private val fn: HttpHandler) : SBB(core, desc) {
    override infix fun describedBy(new: Desc): SBB = SBB0(core, desc, fn)
    override fun toServerRoute(): ServerRoute2 = TODO()
}

class SBB1<A>(core: Core, desc: Desc, private val fn: (A) -> HttpHandler) : SBB(core, desc) {
    override infix fun describedBy(new: Desc): SBB = SBB1(core, desc, fn)
    override fun toServerRoute(): ServerRoute2 = TODO()
}

infix fun Pair<Method, String>.bindTo(fn: HttpHandler): SBB = SBB0(Core(first, PB0 { it / second }), Desc(), fn)

@JvmName("bind0")
infix fun Pair<Method, PB0>.bindTo(fn: HttpHandler): SBB = SBB0(Core(first, second), Desc(), fn)

@JvmName("bind1")
infix fun <A> Pair<Method, PB1<A>>.bindTo(fn: (A) -> HttpHandler): SBB = SBB1(Core(first, second), Desc(), fn)

internal class ExtractedParts(private val mapping: Map<PathLens<*>, *>) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(lens: PathLens<T>): T = mapping[lens] as T
}

private operator fun <T> BasePath.invoke(index: Int, fn: (String) -> T): T? = toList().let { if (it.size > index) fn(it[index]) else null }

private fun BasePath.extract(lenses: List<PathLens<*>>): ExtractedParts? =
    if (this.toList().size == lenses.size) ExtractedParts(lenses.mapIndexed { index, lens -> lens to this(index, lens::invoke) }.toMap()) else null
