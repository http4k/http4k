package org.http4k.bridge

import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.RoutingContext
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import java.io.ByteArrayInputStream


fun VertxToHttp4kHandler(http: HttpHandler): (RoutingContext) -> Unit = { ctx ->
    ctx.request().asHttp4k()
        .map(http)
        .map { it.into(ctx.response()) }
}

fun Response.into(response: HttpServerResponse) =
    headers
        .fold(response) { acc, (k, v) -> acc.putHeader(k, v) }
        .setStatusCode(status.code).setStatusMessage(status.description)
        .end(Buffer.buffer(body.payload.array()))

fun HttpServerRequest.asHttp4k() = body()
    .map {
        headers()
            .fold(Request(Method.valueOf(method().name()), uri())) { acc, (k, v) -> acc.header(k, v) }
            .body(ByteArrayInputStream(it.bytes))
    }

