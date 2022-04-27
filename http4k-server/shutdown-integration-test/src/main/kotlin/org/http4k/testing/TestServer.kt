package org.http4k.testing

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.server.ApacheServer
import org.http4k.server.asServer

fun main() {
    { req: Request -> Response(Status.OK).body("hello from Apache") }.asServer(ApacheServer(8000)).start()
}
