package org.http4k.testing

import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.enum
import org.http4k.server.ApacheServer
import org.http4k.server.ServerConfig
import org.http4k.server.Undertow
import org.http4k.server.asServer

fun main() {
    val backendKey = EnvironmentKey.enum({ ServerBackend.valueOf(it) }, ServerBackend::name).required("BACKEND")

    val selectedBackend = backendKey(Environment.ENV)

    println("Selected $selectedBackend")

    val app = { _: Request -> Response(Status.OK).body("hello from http4k") }

    val server = app.asServer(selectedBackend()).apply { start() }

    Runtime.getRuntime().addShutdownHook(Thread { server.stop() })
}

enum class ServerBackend : () -> ServerConfig {
    Apache {
        override fun invoke() = ApacheServer(8000)
    },
    Undertow {
        override fun invoke(): ServerConfig = Undertow(8000)
    }
}
