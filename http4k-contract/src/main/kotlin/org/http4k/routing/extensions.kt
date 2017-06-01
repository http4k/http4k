package org.http4k.routing

import org.http4k.contract.ContractRenderer
import org.http4k.contract.ContractRouter
import org.http4k.contract.NoRenderer
import org.http4k.contract.NoSecurity
import org.http4k.core.Filter
import org.http4k.core.then
import org.http4k.filter.ServerFilters

fun contractRoutes(contractRoot: String, renderer: ContractRenderer = NoRenderer, filter: Filter = Filter { it }) =
    ContractRouter(contractRoot, renderer, ServerFilters.CatchLensFailure.then(filter), NoSecurity, "", emptyList())
