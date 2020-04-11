package org.http4k.core

import org.http4k.core.Method.GET
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes

val myHandler: HttpHandler = { request -> Response(Status.OK).body(request.uri.toString()) }
val app = routes(
    "/public" bind GET to myHandler,
    "/private" bind GET to ServerFilters.BasicAuth("app", "user", "password")
        .then(myHandler)
)

val myAuthFilter = Filter