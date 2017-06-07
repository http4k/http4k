package org.http4k.contract

import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Json
import org.http4k.format.JsonErrorResponseRenderer
import org.http4k.lens.Failure
import org.http4k.routing.ContractRoute

class SimpleJson<ROOT : NODE, out NODE : Any>(private val json: Json<ROOT, NODE>) : ContractRenderer {

    override fun notFound(): Response = JsonErrorResponseRenderer(json).notFound()

    override fun badRequest(failures: List<Failure>) = JsonErrorResponseRenderer(json).badRequest(failures)

    private fun render(pathSegments: PathSegments, route: ContractRoute) =
        route.method.toString() + ":" + route.describeFor(pathSegments) to json.string(route.meta.summary)

    override fun description(contractRoot: PathSegments, security: Security, routes: List<ContractRoute>): Response {
        return Response(OK)
            .with(json.body().toLens() of json.obj("resources" to json.obj(routes.map { render(contractRoot, it) })))
    }
}