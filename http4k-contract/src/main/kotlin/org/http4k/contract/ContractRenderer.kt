package org.http4k.contract

import org.http4k.core.Response
import org.http4k.lens.Failure
import org.http4k.routing.ServerRoute

interface ContractRenderer {
    fun notFound(): Response

    fun badRequest(failures: List<Failure>): Response

    fun description(contractRoot: BasePath, security: Security, routes: List<ServerRoute>): Response
}

