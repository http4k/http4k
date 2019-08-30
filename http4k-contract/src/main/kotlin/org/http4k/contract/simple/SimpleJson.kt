package org.http4k.contract.simple

import org.http4k.contract.ContractRenderer
import org.http4k.contract.ContractRoute
import org.http4k.contract.ErrorResponseRenderer
import org.http4k.contract.JsonErrorResponseRenderer
import org.http4k.contract.PathSegments
import org.http4k.contract.security.Security
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Json

class SimpleJson<out NODE>(private val json: Json<NODE>) : ContractRenderer, ErrorResponseRenderer by JsonErrorResponseRenderer(json) {
    private fun render(pathSegments: PathSegments, route: ContractRoute) =
        route.method.toString() + ":" + route.describeFor(pathSegments) to json.string(route.meta.summary)

    override fun description(contractRoot: PathSegments, security: Security?, routes: List<ContractRoute>): Response =
        Response(Status.OK)
            .with(json { body().toLens().of<Response>(obj("resources" to obj(routes.map { render(contractRoot, it) }))) })
}