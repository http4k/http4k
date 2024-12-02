package org.http4k.core

import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.experimental.RoutedHttpHandler

typealias HttpHandler = (request: Request) -> Response

fun interface Filter : (HttpHandler) -> HttpHandler {
    companion object
}

val Filter.Companion.NoOp: Filter get() = Filter { it }

fun Filter.then(next: Filter): Filter = Filter { this(next(it)) }

fun Filter.then(next: HttpHandler): HttpHandler = this(next)

fun Filter.then(next: RoutingHttpHandler): RoutingHttpHandler = next.withFilter(this)

fun Filter.then(next: RoutedHttpHandler): RoutedHttpHandler = next.withFilter(this)
