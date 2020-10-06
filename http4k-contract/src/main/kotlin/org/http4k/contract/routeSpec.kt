package org.http4k.contract

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.lens.Path
import org.http4k.lens.PathLens

abstract class ContractRouteSpec internal constructor(
    val pathFn: (PathSegments) -> PathSegments,
    val routeMeta: RouteMeta,
    vararg val pathLenses: PathLens<*>
) {
    abstract infix operator fun <T> div(next: PathLens<T>): ContractRouteSpec

    open infix operator fun div(next: String) = div(Path.fixed(next))

    internal fun describe(contractRoot: PathSegments): String = "${pathFn(contractRoot)}${if (pathLenses.isNotEmpty()) "/${pathLenses.joinToString("/")}" else ""}"

    open inner class ContractRequestBuilder(internal val method: Method) {
        fun newRequest(baseUri: Uri = Uri.of("")) = Request(method, "").uri(baseUri.path(describe(Root)))
    }

    abstract infix fun bindContract(method: Method): ContractRequestBuilder
}

class ContractRouteSpec0 internal constructor(pathFn: (PathSegments) -> PathSegments, routeMeta: RouteMeta) : ContractRouteSpec(pathFn, routeMeta) {
    override infix operator fun div(next: String) = ContractRouteSpec0({ it / next }, routeMeta)

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec1(pathFn, routeMeta, next)

    inner class Binder(method: Method) : ContractRequestBuilder(method) {
        infix fun to(fn: HttpHandler) = with(this@ContractRouteSpec0) { ContractRoute(method, this, routeMeta) { fn } }
    }

    override infix fun bindContract(method: Method) = Binder(method)
}

class ContractRouteSpec1<out A> internal constructor(pathFn: (PathSegments) -> PathSegments, routeMeta: RouteMeta, val a: PathLens<A>) : ContractRouteSpec(pathFn, routeMeta, a) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec2(pathFn, routeMeta, a, next)

    inner class Binder(method: Method) : ContractRequestBuilder(method) {
        infix fun to(fn: (A) -> HttpHandler) = with(this@ContractRouteSpec1) {
            ContractRoute(method, this, routeMeta) { fn(it[a]) }
        }
    }

    override infix fun bindContract(method: Method) = Binder(method)
}

class ContractRouteSpec2<out A, out B> internal constructor(pathFn: (PathSegments) -> PathSegments, routeMeta: RouteMeta, val a: PathLens<A>, val b: PathLens<B>) : ContractRouteSpec(pathFn, routeMeta, a, b) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec3(pathFn, routeMeta, a, b, next)

    inner class Binder(method: Method) : ContractRequestBuilder(method) {
        infix fun to(fn: (A, B) -> HttpHandler) = with(this@ContractRouteSpec2) {
            ContractRoute(method, this, routeMeta) { fn(it[a], it[b]) }
        }
    }

    override infix fun bindContract(method: Method) = Binder(method)
}

class ContractRouteSpec3<out A, out B, out C> internal constructor(
    pathFn: (PathSegments) -> PathSegments,
    routeMeta: RouteMeta,
    val a: PathLens<A>,
    val b: PathLens<B>,
    val c: PathLens<C>
) : ContractRouteSpec(pathFn, routeMeta, a, b, c) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec4(pathFn, routeMeta, a, b, c, next)

    inner class Binder(method: Method) : ContractRequestBuilder(method) {
        infix fun to(fn: (A, B, C) -> HttpHandler) = with(this@ContractRouteSpec3) {
            ContractRoute(method, this, routeMeta) { fn(it[a], it[b], it[c]) }
        }
    }

    override infix fun bindContract(method: Method) = Binder(method)
}

class ContractRouteSpec4<out A, out B, out C, out D> internal constructor(
    pathFn: (PathSegments) -> PathSegments,
    routeMeta: RouteMeta,
    val a: PathLens<A>,
    val b: PathLens<B>,
    val c: PathLens<C>,
    val d: PathLens<D>
) : ContractRouteSpec(pathFn, routeMeta, a, b, c, d) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec5(pathFn, routeMeta, a, b, c, d, next)

    inner class Binder(method: Method) : ContractRequestBuilder(method) {
        infix fun to(fn: (A, B, C, D) -> HttpHandler) = with(this@ContractRouteSpec4) {
            ContractRoute(method, this, routeMeta) { fn(it[a], it[b], it[c], it[d]) }
        }
    }

    override infix fun bindContract(method: Method) = Binder(method)
}

class ContractRouteSpec5<out A, out B, out C, out D, out E> internal constructor(
    pathFn: (PathSegments) -> PathSegments,
    routeMeta: RouteMeta,
    val a: PathLens<A>,
    val b: PathLens<B>,
    val c: PathLens<C>,
    val d: PathLens<D>,
    val e: PathLens<E>
) : ContractRouteSpec(pathFn, routeMeta, a, b, c, d, e) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec6(pathFn, routeMeta, a, b, c, d, e, next)

    inner class Binder(method: Method) : ContractRequestBuilder(method) {
        infix fun to(fn: (A, B, C, D, E) -> HttpHandler) = with(this@ContractRouteSpec5) {
            ContractRoute(method, this, routeMeta) { fn(it[a], it[b], it[c], it[d], it[e]) }
        }
    }

    override infix fun bindContract(method: Method) = Binder(method)
}

class ContractRouteSpec6<out A, out B, out C, out D, out E, out F> internal constructor(
    pathFn: (PathSegments) -> PathSegments,
    routeMeta: RouteMeta,
    val a: PathLens<A>,
    val b: PathLens<B>,
    val c: PathLens<C>,
    val d: PathLens<D>,
    val e: PathLens<E>,
    val f: PathLens<F>
) : ContractRouteSpec(pathFn, routeMeta, a, b, c, d, e, f) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec7(pathFn, routeMeta, a, b, c, d, e, f, next)

    inner class Binder(method: Method) : ContractRequestBuilder(method) {
        infix fun to(fn: (A, B, C, D, E, F) -> HttpHandler) = with(this@ContractRouteSpec6) {
            ContractRoute(method, this, routeMeta) { fn(it[a], it[b], it[c], it[d], it[e], it[f]) }
        }
    }

    override infix fun bindContract(method: Method) = Binder(method)
}

class ContractRouteSpec7<out A, out B, out C, out D, out E, out F, out G> internal constructor(
    pathFn: (PathSegments) -> PathSegments,
    routeMeta: RouteMeta,
    val a: PathLens<A>,
    val b: PathLens<B>,
    val c: PathLens<C>,
    val d: PathLens<D>,
    val e: PathLens<E>,
    val f: PathLens<F>,
    val g: PathLens<G>
) : ContractRouteSpec(pathFn, routeMeta, a, b, c, d, e, f, g) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec8(pathFn, routeMeta, a, b, c, d, e, f, g, next)

    inner class Binder(method: Method) : ContractRequestBuilder(method) {
        infix fun to(fn: (A, B, C, D, E, F, G) -> HttpHandler) = with(this@ContractRouteSpec7) {
            ContractRoute(method, this, routeMeta) { fn(it[a], it[b], it[c], it[d], it[e], it[f], it[g]) }
        }
    }

    override infix fun bindContract(method: Method) = Binder(method)
}

class ContractRouteSpec8<out A, out B, out C, out D, out E, out F, out G, out H> internal constructor(
    pathFn: (PathSegments) -> PathSegments,
    routeMeta: RouteMeta,
    val a: PathLens<A>,
    val b: PathLens<B>,
    val c: PathLens<C>,
    val d: PathLens<D>,
    val e: PathLens<E>,
    val f: PathLens<F>,
    val g: PathLens<G>,
    val h: PathLens<H>
) : ContractRouteSpec(pathFn, routeMeta, a, b, c, d, e, f, g, h) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec9(pathFn, routeMeta, a, b, c, d, e, f, g, h, next)

    inner class Binder(method: Method) : ContractRequestBuilder(method) {
        infix fun to(fn: (A, B, C, D, E, F, G, H) -> HttpHandler) = with(this@ContractRouteSpec8) {
            ContractRoute(method, this, routeMeta) { fn(it[a], it[b], it[c], it[d], it[e], it[f], it[g], it[h]) }
        }
    }

    override infix fun bindContract(method: Method) = Binder(method)
}

class ContractRouteSpec9<out A, out B, out C, out D, out E, out F, out G, out H, out I> internal constructor(
    pathFn: (PathSegments) -> PathSegments,
    routeMeta: RouteMeta,
    val a: PathLens<A>,
    val b: PathLens<B>,
    val c: PathLens<C>,
    val d: PathLens<D>,
    val e: PathLens<E>,
    val f: PathLens<F>,
    val g: PathLens<G>,
    val h: PathLens<H>,
    val i: PathLens<I>
) : ContractRouteSpec(pathFn, routeMeta, a, b, c, d, e, f, g, h, i) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = ContractRouteSpec10(pathFn, routeMeta, a, b, c, d, e, f, g, h, i, next)

    inner class Binder(method: Method) : ContractRequestBuilder(method) {
        infix fun to(fn: (A, B, C, D, E, F, G, H, I) -> HttpHandler): ContractRoute =
            with(this@ContractRouteSpec9) {
                ContractRoute(method, this, routeMeta) { fn(it[a], it[b], it[c], it[d], it[e], it[f], it[g], it[h], it[i]) }
            }
    }

    override infix fun bindContract(method: Method) = Binder(method)
}

class ContractRouteSpec10<out A, out B, out C, out D, out E, out F, out G, out H, out I, out J> internal constructor(
    pathFn: (PathSegments) -> PathSegments,
    routeMeta: RouteMeta,
    val a: PathLens<A>,
    val b: PathLens<B>,
    val c: PathLens<C>,
    val d: PathLens<D>,
    val e: PathLens<E>,
    val f: PathLens<F>,
    val g: PathLens<G>,
    val h: PathLens<H>,
    val i: PathLens<I>,
    val j: PathLens<J>
) : ContractRouteSpec(pathFn, routeMeta, a, b, c, d, e, f, g, h, i, j) {
    override infix operator fun div(next: String) = div(Path.fixed(next))

    override infix operator fun <NEXT> div(next: PathLens<NEXT>) = throw UnsupportedOperationException("no longer paths!")

    inner class Binder(method: Method) : ContractRequestBuilder(method) {
        infix fun to(fn: (A, B, C, D, E, F, G, H, I, J) -> HttpHandler) = with(this@ContractRouteSpec10) {
            ContractRoute(method, this, routeMeta) { fn(it[a], it[b], it[c], it[d], it[e], it[f], it[g], it[h], it[i], it[j]) }
        }
    }

    override infix fun bindContract(method: Method) = Binder(method)
}
