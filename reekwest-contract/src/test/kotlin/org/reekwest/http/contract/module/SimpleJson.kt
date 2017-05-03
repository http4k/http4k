package org.reekwest.http.contract.module

import org.reekwest.http.core.Response
import org.reekwest.http.core.Status
import org.reekwest.http.core.with
import org.reekwest.http.formats.Json
import org.reekwest.http.formats.JsonErrorResponseRenderer
import org.reekwest.http.lens.Failure

class SimpleJson<ROOT : NODE, out NODE : Any>(private val json: Json<ROOT, NODE>) : ModuleRenderer {

    override fun notFound(): Response = JsonErrorResponseRenderer(json).notFound()

    override fun badRequest(failures: List<Failure>) = JsonErrorResponseRenderer(json).badRequest(failures)

    private fun render(basePath: BasePath, route: ServerRoute) =
        route.pathBinder.core.method.toString() + ":" + route.describeFor(basePath) to
            json.string(route.pathBinder.core.route.core.description ?: "")

    override fun description(basePath: BasePath, routes: List<ServerRoute>): Response {
        return Response(Status.OK)
            .with(json.body().required() to json.obj("resources" to json.obj(routes.map { render(basePath, it) })))
    }
}