package org.http4k.contract

import org.http4k.contract.security.Security
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK

object NoRenderer : ContractRenderer {
    override fun description(contractRoot: PathSegments, securities: List<Security>, routes: List<ContractRoute>) = Response(OK)
}

