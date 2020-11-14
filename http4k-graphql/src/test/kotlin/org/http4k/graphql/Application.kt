package org.http4k.graphql

import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.routing.graphQL
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun App() = routes(
    OPTIONS to { _ -> Response(OK) },
    POST to graphQL(MySchemaHandler())
)

fun main() {
    PrintRequestAndResponse()
        .then(App())
        .asServer(SunHttp(5000)).start()
}
