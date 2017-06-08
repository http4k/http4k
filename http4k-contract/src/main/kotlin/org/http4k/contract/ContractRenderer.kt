package org.http4k.contract

import org.http4k.core.Response
import org.http4k.lens.Failure

interface ContractRenderer {
    fun notFound(): Response

    fun badRequest(failures: List<Failure>): Response

    fun description(contractRoot: PathSegments, security: Security, routes: List<ContractRoute>): Response
}

