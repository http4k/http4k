package org.http4k.server

import io.helidon.common.http.Http.Status.create
import io.helidon.nima.webserver.http.Handler
import io.helidon.nima.webserver.http.ServerRequest
import io.helidon.nima.webserver.http.ServerResponse
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Uri

fun HelidonHandler(http: HttpHandler) = Handler { req, res ->
    (req.toHttp4k()?.let(http) ?: Response(NOT_IMPLEMENTED)).into(res)
}

private fun Response.into(res: ServerResponse) = res.apply {
    status(create(status.code, status.description))
    this@into.headers.groupBy { it.first }.forEach {
        header(it.key, *it.value.map { it.second ?: "" }.toTypedArray())
    }
    outputStream().use { body.stream.copyTo(it) }
}

private fun ServerRequest.toHttp4k(): Request? =
    Method.supportedOrNull(prologue().method().text())
        ?.let {
            Request(it, Uri.of(path().rawPath() + query()), prologue().protocolVersion())
                .body(content().inputStream(), headers().contentLength().let {
                    if(it.isPresent) it.asLong else null
                })
                .headers(
                    headers()
                        .flatMap { value -> value.allValues().map { value.headerName().defaultCase() to it } }
                )
                .source(RequestSource(remotePeer().host(), remotePeer().port()))
        }
