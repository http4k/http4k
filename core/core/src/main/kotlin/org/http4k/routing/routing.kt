package org.http4k.routing

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED


fun Method.asRouter() = Router("method == $this", notMatchedStatus = METHOD_NOT_ALLOWED) { it.method == this }

fun Method.and(router: Router) = asRouter().and(router)

/**
 * Apply routing predicate to a query
 */
fun query(name: String, fn: (String) -> Boolean) =
    { req: Request -> req.queries(name).filterNotNull().any(fn) }.asRouter("Query $name matching $fn")

/**
 * Apply routing predicate to a query
 */
fun query(name: String, value: String) = query(name, Matcher.equalTo(value))

/**
 * Ensure all queries are present
 */
fun queries(vararg names: String) =
    { req: Request -> names.all { req.query(it) != null } }.asRouter("Queries ${names.toList()}")

/**
 * Apply routing predicate to a header
 */
fun header(name: String, fn: (String) -> Boolean) =
    { req: Request -> req.headerValues(name).filterNotNull().any(fn) }.asRouter("Header $name matching $fn")

/**
 * Apply routing predicate to a header
 */
fun header(name: String, value: String) = header(name, Matcher.equalTo(value))

/**
 * Ensure all headers are present
 */
fun headers(vararg names: String) =
    { req: Request -> names.all { req.header(it) != null } }.asRouter("Headers ${names.toList()}")

/**
 * Ensure body matches predicate
 */
fun body(fn: (Body) -> Boolean) = { it: Request -> fn(it.body) }.asRouter("Body matching $fn")

/**
 * Ensure body string matches predicate
 */
@JvmName("bodyMatches")
fun body(fn: (String) -> Boolean) = { req: Request -> fn(req.bodyString()) }.asRouter("Body matching $fn")

interface Matcher: (String) -> Boolean{
    companion object{
        fun equalTo(value: String) = object :Matcher{
            override fun invoke(p1: String) = p1 == value
            override fun toString(): String = value
        }
    }
}
