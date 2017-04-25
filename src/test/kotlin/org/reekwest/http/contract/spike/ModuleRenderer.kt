package org.reekwest.http.contract.spike

import org.reekwest.http.contract.ExtractionFailure
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.BAD_REQUEST
import org.reekwest.http.core.Status.Companion.NOT_FOUND

interface ModuleRenderer {
    fun notFound(): Response

    fun badRequest(badParameters: Iterable<ExtractionFailure>): Response
}

object NoRenderer : ModuleRenderer {
//    override fun description(basePath: Path, security: Security, routes: Iterable<ServerRoute>): Response = Response(OK)

    override fun badRequest(badParameters: Iterable<ExtractionFailure>): Response = Response(BAD_REQUEST)

    override fun notFound(): Response = Response(NOT_FOUND)
}