package org.http4k.server

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.TypedData

class RatpackHttp4kHandler(private val httpHandler: HttpHandler) : Handler {
    override fun handle(context: Context) {
        context.request.body.then { data ->
            (context.toHttp4kRequest(data)?.let(httpHandler) ?: Response(NOT_IMPLEMENTED)).pushTo(context)
        }
    }

    private fun Context.toHttp4kRequest(data: TypedData) = Method.supportedOrNull(request.method.name)
        ?.let {
            Request(it, request.rawUri)
                .let {
                    request.headers.names.fold(it) { acc, nextHeaderName ->
                        request.headers.getAll(nextHeaderName)
                            .fold(acc) { vAcc, nextHeaderValue ->
                                vAcc.header(nextHeaderName, nextHeaderValue)
                            }
                    }
                }
                .body(data.inputStream, request.headers.get("content-length")?.toLongOrNull())
                .source(RequestSource(request.remoteAddress.host, request.remoteAddress.port))
        }

    private fun Response.pushTo(context: Context) {
        headers.groupBy { it.first }
            .forEach { (name, values) ->
                context.response.headers.set(name, values.mapNotNull { it.second })
            }
        context.response.status(status.code)
        context.response.send(body.payload.array())
    }
}
