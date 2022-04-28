package org.http4k.server

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.server.ServerConfig.StopMode
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.TypedData
import ratpack.server.RatpackServer
import ratpack.server.RatpackServerSpec
import ratpack.server.ServerConfig.builder
import java.time.Duration.ofSeconds

class Ratpack(port: Int = 8000, stopMode: StopMode) : ServerConfig {
    constructor(port: Int = 8000) : this(port, StopMode.Immediate)

    init {
        when (stopMode) {
            is StopMode.Delayed, is StopMode.Graceful -> throw ServerConfig.UnsupportedStopMode(stopMode)
            else -> {}
        }
    }

    private val serverConfig = builder().connectQueueSize(1000).port(port)

    override fun toServer(http: HttpHandler): Http4kServer {
        val server = RatpackServer.of { server: RatpackServerSpec ->
            server.serverConfig(serverConfig)
                .handler { RatpackHttp4kHandler(http) }
        }

        return object : Http4kServer {
            override fun start(): Http4kServer = apply { server.start() }

            override fun stop(): Http4kServer = apply { server.stop() }

            override fun port(): Int = server.bindPort
        }
    }
}

class RatpackHttp4kHandler(private val httpHandler: HttpHandler) : Handler {
    override fun handle(context: Context) {
        context.request.body.then { data ->
            val request = context.toHttp4kRequest(data)
            val response = httpHandler(request)
            response.pushTo(context)
        }
    }

    private fun Context.toHttp4kRequest(data: TypedData) = Request(Method.valueOf(request.method.name), request.rawUri)
        .let {
            request.headers.names.fold(it, { acc, nextHeaderName ->
                request.headers.getAll(nextHeaderName)
                    .fold(acc, { vAcc, nextHeaderValue ->
                        vAcc.header(nextHeaderName, nextHeaderValue)
                    })
            })
        }
        .body(data.inputStream, request.headers.get("content-length")?.toLongOrNull())
        .source(RequestSource(request.remoteAddress.host, request.remoteAddress.port))

    private fun Response.pushTo(context: Context) {
        headers.groupBy { it.first }
            .forEach { (name, values) ->
                context.response.headers.set(name, values.mapNotNull { it.second })
            }
        context.response.status(status.code)
        context.response.send(body.payload.array())
    }
}
