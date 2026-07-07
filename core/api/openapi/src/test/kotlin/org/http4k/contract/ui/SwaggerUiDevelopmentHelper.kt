package org.http4k.contract.ui

import org.http4k.contract.contract
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    val api = contract {
        renderer = OpenApi3(ApiInfo("Cat Shelter", "1"))
        descriptionPath = "openapi.json"

        routes += "/cats" meta {
            operationId = "listCats"
        } bindContract Method.GET to { _ -> Response(Status.OK) }

        routes += "/cats" meta {
            operationId = "createCat"
        } bindContract Method.POST to { _ -> Response(Status.OK) }
    }

    val ui = swaggerUiLite {
        pageTitle = "Cat Shelter"
        url = "openapi.json"
        displayOperationId = true
    }

    routes(api, ui)
        .asServer(SunHttp(0))
        .start()
        .also { println("http://localhost:${it.port()}") }
}
