package org.http4k.contract

import org.http4k.contract.security.Security
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Failure

object NoRenderer : ContractRenderer {
    override fun description(contractRoot: PathSegments, security: Security, routes: List<ContractRoute>) = Response(OK)

    override fun badRequest(failures: List<Failure>) = Response(BAD_REQUEST)

    override fun notFound(): Response = Response(NOT_FOUND)
}

