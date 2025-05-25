package org.http4k.bridge

import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import java.io.ByteArrayInputStream

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

