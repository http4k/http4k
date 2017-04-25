package org.reekwest.http.contract

interface ModuleRenderer {
    fun notFound(): org.reekwest.http.core.Response

    fun badRequest(badParameters: Iterable<ExtractionFailure>): org.reekwest.http.core.Response
}

object NoRenderer : ModuleRenderer {
//    override fun description(basePath: Path, security: Security, routes: Iterable<ServerRoute>): Response = Response(OK)

    override fun badRequest(badParameters: Iterable<ExtractionFailure>): org.reekwest.http.core.Response = org.reekwest.http.core.Response(org.reekwest.http.core.Status.Companion.BAD_REQUEST)

    override fun notFound(): org.reekwest.http.core.Response = org.reekwest.http.core.Response(org.reekwest.http.core.Status.Companion.NOT_FOUND)
}