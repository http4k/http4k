package org.http4k.contract

import org.http4k.http.core.Response
import org.http4k.http.core.Status.Companion.OK
import org.http4k.http.core.with
import org.http4k.http.formats.Json
import org.http4k.http.formats.JsonErrorResponseRenderer
import org.http4k.http.lens.Failure

class SimpleJson<ROOT : NODE, out NODE : Any>(private val json: Json<ROOT, NODE>) : ModuleRenderer {

    override fun notFound(): Response = JsonErrorResponseRenderer(json).notFound()

    override fun badRequest(failures: List<Failure>) = JsonErrorResponseRenderer(json).badRequest(failures)

    private fun render(basePath: BasePath, route: ServerRoute) =
        route.method.toString() + ":" + route.describeFor(basePath) to json.string(route.core.summary)

    override fun description(moduleRoot: BasePath, security: Security, routes: List<ServerRoute>): Response {
        return Response(OK)
            .with(json.body().required() to json.obj("resources" to json.obj(routes.map { render(moduleRoot, it) })))
    }
}