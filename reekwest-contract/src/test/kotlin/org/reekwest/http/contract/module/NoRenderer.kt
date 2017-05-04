package org.reekwest.http.contract.module

import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.BAD_REQUEST
import org.reekwest.http.core.Status.Companion.NOT_FOUND
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.lens.Failure

object NoRenderer : ModuleRenderer {
    override fun description(moduleRoot: BasePath, security: Security, routes: List<ServerRoute>) = Response(OK)

    override fun badRequest(failures: List<Failure>): Response = Response(BAD_REQUEST)

    override fun notFound(): Response = Response(NOT_FOUND)
}

