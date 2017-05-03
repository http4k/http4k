package org.reekwest.http.contract.module

import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.BAD_REQUEST
import org.reekwest.http.core.Status.Companion.NOT_FOUND
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.with
import org.reekwest.http.formats.Json
import org.reekwest.http.lens.Failure

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

class SimpleJson<ROOT : NODE, out NODE : Any>(private val json: Json<ROOT, NODE>) : ModuleRenderer {

    override fun notFound(): Response = Response(NOT_FOUND)

    override fun badRequest(failures: Iterable<Failure>) = Response(BAD_REQUEST).body(failures.joinToString())

    private fun render(basePath: BasePath, route: ServerRoute) =
        route.pathBinder.core.method.toString() + ":" + route.describeFor(basePath) to
            json.string(route.pathBinder.core.route.core.description ?: "")

    override fun description(basePath: BasePath, routes: Iterable<ServerRoute>): Response {
        return Response(OK)
            .with(json.body().required() to json.obj("resources" to json.obj(routes.map { render(basePath, it) })))
    }
//    override def badRequest(badParameters: Seq[ExtractionError]): Response = JsonErrorResponseRenderer.badRequest(badParameters)
//
//    override def notFound(request: Request): Response = JsonErrorResponseRenderer.notFound()
}

