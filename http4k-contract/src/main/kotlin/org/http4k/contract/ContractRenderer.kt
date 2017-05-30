package org.http4k.contract

import org.http4k.core.Response
import org.http4k.lens.Failure

interface ContractRenderer {
    fun notFound(): Response

    fun badRequest(failures: List<Failure>): Response

    fun description(contractRoot: BasePath, security: Security, routes: List<ServerRoute>): Response
}

