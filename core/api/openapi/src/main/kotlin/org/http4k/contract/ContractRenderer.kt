package org.http4k.contract

import org.http4k.core.Response
import org.http4k.security.Security

fun interface ContractRenderer : ErrorResponseRenderer {
    fun description(
        contractRoot: PathSegments,
        security: Security?,
        routes: List<ContractRoute>,
        tags: Set<Tag>,
        webhooks: Map<String, List<WebCallback>>
    ): Response
}
