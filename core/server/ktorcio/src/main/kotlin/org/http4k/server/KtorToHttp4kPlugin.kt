package org.http4k.server

import io.ktor.server.application.createApplicationPlugin
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status

fun KtorToHttp4kPlugin(http: HttpHandler) = createApplicationPlugin(name = "http4k") {
    onCall {
        it.response.fromHttp4K(it.request.asHttp4k()?.let(http) ?: Response(Status.NOT_IMPLEMENTED))
    }
}
