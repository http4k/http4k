package org.http4k.contract

import org.http4k.contract.openapi.v3.ServerObject
import org.http4k.contract.security.Security
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK

object NoRenderer : ContractRenderer {
    override fun description(contractRoot: PathSegments, security: Security?, routes: List<ContractRoute>, servers: List<ServerObject>) = Response(OK)
}

