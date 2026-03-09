/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package wiretap.examples

import org.http4k.contract.bindContract
import org.http4k.contract.contract
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.security.BasicAuthSecurity

fun OpenApiApp() = contract {
    renderer = OpenApi3(ApiInfo("My OpenApi App", "1.0"))
    security = BasicAuthSecurity("") { true }
    descriptionPath = "/openapi"
    routes += route()
    routes += AnotherRoute()
}

private fun AnotherRoute() = "route2" bindContract POST to { _ -> Response(OK).body("bar") }

private fun route() = "route1" meta {
    summary = "A route"
    description = "isn't this a nice endpoint"
    returning(OK)
} bindContract GET to { _ -> Response(OK).body("bar") }
