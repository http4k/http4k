package org.http4k.contract

import org.http4k.http.core.Response
import org.http4k.http.core.Status.Companion.BAD_REQUEST
import org.http4k.http.core.Status.Companion.NOT_FOUND
import org.http4k.http.core.Status.Companion.OK
import org.http4k.http.lens.Failure

object NoRenderer : ModuleRenderer {
    override fun description(moduleRoot: BasePath, security: Security, routes: List<ServerRoute>) = Response(OK)

    override fun badRequest(failures: List<Failure>): Response = Response(BAD_REQUEST)

    override fun notFound(): Response = Response(NOT_FOUND)
}

