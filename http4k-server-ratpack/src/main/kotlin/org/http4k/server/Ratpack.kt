package org.http4k.server

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.TypedData
import ratpack.server.RatpackServer
import ratpack.server.RatpackServerSpec
import ratpack.server.ServerConfig.builder

class Ratpack(port: Int = 8000) : ServerConfig {
    private val serverConfig = builder().port(port)

    override fun toServer(httpHandler: HttpHandler): Http4kServer {
        val server = RatpackServer.of { server: RatpackServerSpec ->
            server.serverConfig(serverConfig)
                .handler { RatpackHttp4kHandler(httpHandler) }
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


fun main() {
    { req: Request -> Response(Status.OK).body("foobar") }.asServer(Ratpack(9000)).start()
}
