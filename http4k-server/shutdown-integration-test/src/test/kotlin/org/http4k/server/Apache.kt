package org.http4k.server

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

fun main() {
    { req: Request -> Response(Status.OK).body("hello from Apache") }.asServer(ApacheServer()).start()
}
