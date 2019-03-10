package org.http4k.contract

import org.http4k.contract.PreFlightExtraction.Companion.All
import org.http4k.core.Method
import org.http4k.lens.Path
import org.http4k.lens.PathLens
import org.http4k.util.Appendable

fun contract(fn: ContractBuilder.() -> Unit) = ContractBuilder().apply(fn).run {
    ContractRoutingHttpHandler(renderer, security, descriptionPath, preFlightExtraction, routes.all)
}

class ContractBuilder internal constructor() {
    var renderer: ContractRenderer = NoRenderer
    var security: Security = NoSecurity
    var descriptionPath = ""
    var preFlightExtraction: PreFlightExtraction = All
    var routes = Appendable<ContractRoute>()
}

operator fun <A> String.div(next: PathLens<A>): ContractRouteSpec1<A> = ContractRouteSpec0(toBaseFn(this), RouteMeta()) / next

operator fun <A> PathLens<A>.div(next: String): ContractRouteSpec2<A, String> = this / Path.fixed(next)
operator fun <A, B> PathLens<A>.div(next: PathLens<B>): ContractRouteSpec2<A, B> = ContractRouteSpec1({ it }, RouteMeta(), this) / next

infix fun String.bind(router: ContractRoutingHttpHandler): ContractRoutingHttpHandler = router.withBasePath(this)

infix fun <A> PathLens<A>.bindContract(method: Method) = ContractRouteSpec1({ it }, RouteMeta(), this).bindContract(method)

infix fun String.bindContract(method: Method) = ContractRouteSpec0(toBaseFn(this), RouteMeta()).bindContract(method)

infix fun String.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec0(toBaseFn(this), routeMetaDsl(new))
infix fun ContractRouteSpec0.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec0(pathFn, routeMetaDsl(new))
infix fun <A> ContractRouteSpec1<A>.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec1(pathFn, routeMetaDsl(new), a)
infix fun <A, B> ContractRouteSpec2<A, B>.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec2(pathFn, routeMetaDsl(new), a, b)
infix fun <A, B, C> ContractRouteSpec3<A, B, C>.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec3(pathFn, routeMetaDsl(new), a, b, c)
infix fun <A, B, C, D> ContractRouteSpec4<A, B, C, D>.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec4(pathFn, routeMetaDsl(new), a, b, c, d)
infix fun <A, B, C, D, E> ContractRouteSpec5<A, B, C, D, E>.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec5(pathFn, routeMetaDsl(new), a, b, c, d, e)
infix fun <A, B, C, D, E, F> ContractRouteSpec6<A, B, C, D, E, F>.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec6(pathFn, routeMetaDsl(new), a, b, c, d, e, f)
infix fun <A, B, C, D, E, F, G> ContractRouteSpec7<A, B, C, D, E, F, G>.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec7(pathFn, routeMetaDsl(new), a, b, c, d, e, f, g)
infix fun <A, B, C, D, E, F, G, H> ContractRouteSpec8<A, B, C, D, E, F, G, H>.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec8(pathFn, routeMetaDsl(new), a, b, c, d, e, f, g, h)
infix fun <A, B, C, D, E, F, G, H, I> ContractRouteSpec9<A, B, C, D, E, F, G, H, I>.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec9(pathFn, routeMetaDsl(new), a, b, c, d, e, f, g, h, i)
infix fun <A, B, C, D, E, F, G, H, I, J> ContractRouteSpec10<A, B, C, D, E, F, G, H, I, J>.meta(new: RouteMetaDsl.() -> Unit) = ContractRouteSpec10(pathFn, routeMetaDsl(new), a, b, c, d, e, f, g, h, i, j)

internal fun toBaseFn(path: String): (PathSegments) -> PathSegments = PathSegments(path).let { { old: PathSegments -> old / it } }

@Deprecated("Replaced with DSL version using contract { routes += serverRoutes.toList() }", ReplaceWith("BROKEN! use example"))
fun contract(vararg serverRoutes: ContractRoute) = contract {
    routes += serverRoutes.toList()
}

@Deprecated("Replaced with DSL version using contract { routes += serverRoutes.toList(); this.renderer = renderer }",
    ReplaceWith("BROKEN! use example"))
fun contract(renderer: ContractRenderer, vararg serverRoutes: ContractRoute) = contract {
    this.renderer = renderer
    this.routes += serverRoutes.toList()
}

@Deprecated("Replaced with DSL version using contract { routes += serverRoutes.toList(); this.renderer = renderer }",
    ReplaceWith("BROKEN! use example"))
fun contract(renderer: ContractRenderer, descriptionPath: String, vararg serverRoutes: ContractRoute) = contract {
    this.renderer = renderer
    this.descriptionPath = descriptionPath
    this.routes += serverRoutes.toList()
}

@Deprecated("Replaced with DSL version using contract { routes += serverRoutes.toList(); this.renderer = renderer; this.descriptionPath = descriptionPath; this.security = security }",
    ReplaceWith("BROKEN! use example"))
fun contract(renderer: ContractRenderer = NoRenderer, descriptionPath: String = "", security: Security = NoSecurity, vararg serverRoutes: ContractRoute) = contract {
    this.renderer = renderer
    this.security = security
    this.descriptionPath = descriptionPath
    this.routes += serverRoutes.toList()
}