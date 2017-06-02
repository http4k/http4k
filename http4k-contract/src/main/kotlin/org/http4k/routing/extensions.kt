package org.http4k.routing

import org.http4k.contract.ContractRenderer
import org.http4k.contract.ContractRouter
import org.http4k.contract.NoRenderer
import org.http4k.contract.NoSecurity
import org.http4k.filter.ServerFilters
import org.http4k.contract.ContractRouter.Companion.Handler as ContractHandler

infix fun String.by(router: ContractRouter): ContractRouter = router.withBasePath(this)

fun contract(renderer: ContractRenderer = NoRenderer): ContractRouter =
    ContractRouter(ContractHandler("", renderer, ServerFilters.CatchLensFailure, NoSecurity, "", emptyList()))
