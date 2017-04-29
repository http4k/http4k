package org.reekwest.http.contract.module

import org.reekwest.http.contract.lens.Failure
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.BAD_REQUEST
import org.reekwest.http.core.Status.Companion.NOT_FOUND
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.bodyString

interface ModuleRenderer {
    fun notFound(): Response

    fun badRequest(failures: Iterable<Failure>): Response

    fun description(basePath: BasePath, routes: Iterable<ServerRoute>): Response
}

object NoRenderer : ModuleRenderer {
    override fun description(basePath: BasePath, routes: Iterable<ServerRoute>) = Response(OK)

    override fun badRequest(failures: Iterable<Failure>): Response = Response(BAD_REQUEST)

    override fun notFound(): Response = Response(NOT_FOUND)
}

class SimpleJson : ModuleRenderer {
    override fun notFound(): Response = Response(NOT_FOUND)

    override fun badRequest(failures: Iterable<Failure>) = Response(BAD_REQUEST).bodyString(failures.joinToString())

    override fun description(basePath: BasePath, routes: Iterable<ServerRoute>) = Response(OK)
        .bodyString(routes.map { it.describeFor(basePath) }.joinToString())
//    override def badRequest(badParameters: Seq[ExtractionError]): Response = JsonErrorResponseRenderer.badRequest(badParameters)
//
//    override def notFound(request: Request): Response = JsonErrorResponseRenderer.notFound()
//
//    private def render(basePath: Path, route: ServerRoute[_, _]): Field =
//    route.method.toString() + ":" + route.describeFor(basePath) -> Argo.JsonFormat.string(route.routeSpec.summary)
//
//    override def description(basePath: Path, security: Security, routes: Seq[ServerRoute[_, _]]): Response = Ok(obj("resources" -> obj(routes.map(r => render(basePath, r)))))
}

