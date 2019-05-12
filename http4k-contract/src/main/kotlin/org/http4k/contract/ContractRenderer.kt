package org.http4k.contract

import org.http4k.contract.security.Security
import org.http4k.core.Response

interface ContractRenderer : ErrorResponseRenderer {
    fun description(contractRoot: PathSegments, security: Security, routes: List<ContractRoute>): Response
}

