package org.http4k.testing

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.server.ApacheServer
import org.http4k.server.asServer

fun main() {
    val app = { req: Request -> Response(Status.OK).body("hello from http4k") }

    val server = app.asServer(ApacheServer(8000)).apply { start() }

    Runtime.getRuntime().addShutdownHook(Thread { server.stop() })
}
