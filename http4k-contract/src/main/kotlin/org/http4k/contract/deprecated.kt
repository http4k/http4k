package org.http4k.contract

import org.http4k.lens.BodyLens
import org.http4k.lens.HeaderLens
import org.http4k.lens.QueryLens

@Deprecated("use String.meta instead to define contract")
infix fun String.query(new: QueryLens<*>) = ContractRouteSpec0(toBaseFn(this), RouteMeta(requestParams = listOf(new)))

@Deprecated("use String.meta instead to define contract")
infix fun String.header(new: HeaderLens<*>) = ContractRouteSpec0(toBaseFn(this), RouteMeta(requestParams = listOf(new)))

@Deprecated("use String.meta instead to define contract")
infix fun String.body(new: BodyLens<*>) = ContractRouteSpec0(toBaseFn(this), RouteMeta(body = new))

@Deprecated("use ContractRouteSpec0.meta instead to define contract")
infix fun ContractRouteSpec0.query(new: QueryLens<*>) = ContractRouteSpec0(pathFn, routeMeta + new)

@Deprecated("use ContractRouteSpec0.meta instead to define contract")
infix fun ContractRouteSpec0.header(new: HeaderLens<*>) = ContractRouteSpec0(pathFn, routeMeta + new)

@Deprecated("use ContractRouteSpec0.meta instead to define contract")
infix fun ContractRouteSpec0.body(new: BodyLens<*>) = ContractRouteSpec0(pathFn, routeMeta.copy(body = new))

@Deprecated("use ContractRouteSpec1.meta instead to define contract")
infix fun <A> ContractRouteSpec1<A>.query(new: QueryLens<*>) = ContractRouteSpec1(pathFn, routeMeta + new, a)

@Deprecated("use ContractRouteSpec1.meta instead to define contract")
infix fun <A> ContractRouteSpec1<A>.header(new: HeaderLens<*>) = ContractRouteSpec1(pathFn, routeMeta + new, a)

@Deprecated("use ContractRouteSpec1.meta instead to define contract")
infix fun <A> ContractRouteSpec1<A>.body(new: BodyLens<*>) = ContractRouteSpec1(pathFn, routeMeta + new, a)

@Deprecated("use ContractRouteSpec2.meta instead to define contract")
infix fun <A, B> ContractRouteSpec2<A, B>.query(new: QueryLens<*>) = ContractRouteSpec2(pathFn, routeMeta + new, a, b)

@Deprecated("use ContractRouteSpec2.meta instead to define contract")
infix fun <A, B> ContractRouteSpec2<A, B>.header(new: HeaderLens<*>) = ContractRouteSpec2(pathFn, routeMeta + new, a, b)

@Deprecated("use ContractRouteSpec2.meta instead to define contract")
infix fun <A, B> ContractRouteSpec2<A, B>.body(new: BodyLens<*>) = ContractRouteSpec2(pathFn, routeMeta + new, a, b)

@Deprecated("use ContractRouteSpec3.meta instead to define contract")
infix fun <A, B, C> ContractRouteSpec3<A, B, C>.query(new: QueryLens<*>) = ContractRouteSpec3(pathFn, routeMeta + new, a, b, c)

@Deprecated("use ContractRouteSpec3.meta instead to define contract")
infix fun <A, B, C> ContractRouteSpec3<A, B, C>.header(new: HeaderLens<*>) = ContractRouteSpec3(pathFn, routeMeta + new, a, b, c)

@Deprecated("use ContractRouteSpec3.meta instead to define contract")
infix fun <A, B, C> ContractRouteSpec3<A, B, C>.body(new: BodyLens<*>) = ContractRouteSpec3(pathFn, routeMeta + new, a, b, c)

@Deprecated("use ContractRouteSpec4.meta instead to define contract")
infix fun <A, B, C, D> ContractRouteSpec4<A, B, C, D>.query(new: QueryLens<*>) = ContractRouteSpec4(pathFn, routeMeta + new, a, b, c, d)

@Deprecated("use ContractRouteSpec4.meta instead to define contract")
infix fun <A, B, C, D> ContractRouteSpec4<A, B, C, D>.header(new: HeaderLens<*>) = ContractRouteSpec4(pathFn, routeMeta + new, a, b, c, d)

@Deprecated("use ContractRouteSpec4.meta instead to define contract")
infix fun <A, B, C, D> ContractRouteSpec4<A, B, C, D>.body(new: BodyLens<*>) = ContractRouteSpec4(pathFn, routeMeta + new, a, b, c, d)

@Deprecated("use ContractRouteSpec.meta instead to define contract")
infix fun ContractRoute.meta(new: RouteMeta) = ContractRoute(method, spec, new, toHandler)
