package org.http4k.contract

import org.http4k.contract.security.Security
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND

object NoRenderer : ContractRenderer {
    override fun description(
        contractRoot: PathSegments,
        security: Security?,
        routes: List<ContractRoute>,
        tags: Set<Tag>,
        webhooks: Map<String, List<WebCallback>>
    ): Response = Response(NOT_FOUND)
}
