package org.reekwest.kontrakt.module

import org.reekwest.http.core.Response
import org.reekwest.http.core.Status
import org.reekwest.kontrakt.lens.ExtractionFailure

interface ModuleRenderer {
    fun notFound(): Response

    fun badRequest(badParameters: Iterable<ExtractionFailure>): Response
}

object NoRenderer : ModuleRenderer {
//    override fun description(basePath: Path, security: Security, routes: Iterable<ServerRoute>): Response = Response(OK)

    override fun badRequest(badParameters: Iterable<ExtractionFailure>): Response = Response(Status.BAD_REQUEST)

    override fun notFound(): Response = Response(Status.NOT_FOUND)
}