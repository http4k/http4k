package org.http4k.graphql

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.POST
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.filter.ServerFilters.InitialiseRequestContext
import org.http4k.lens.RequestContextKey
import org.http4k.lens.RequestContextLens
import org.http4k.routing.graphQL
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun App(): HttpHandler {
    val contexts = RequestContexts()
    val user = RequestContextKey.required<String>(contexts)

    return InitialiseRequestContext(contexts)
        .then(AddUserToContext(user))
        .then(
            routes(
                OPTIONS to { _ -> Response(OK) },
                POST to graphQL(MySchemaHandler(), user)
            )
        )
}

private fun AddUserToContext(user: RequestContextLens<String>) = Filter { next ->
    {
        next(it.with(user of it.method.toString()))
    }
}


fun main() {
    PrintRequestAndResponse()
        .then(App())
        .asServer(SunHttp(5000)).start()
}
