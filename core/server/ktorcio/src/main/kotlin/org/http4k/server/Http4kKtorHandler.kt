package org.http4k.server

import io.ktor.server.application.PipelineCall
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status

class Http4kKtorHandler(private val http: HttpHandler) {
    suspend operator fun invoke(it: PipelineCall) {
        it.response.fromHttp4K(it.request.asHttp4k()?.let(http) ?: Response(Status.NOT_IMPLEMENTED))
    }
}
