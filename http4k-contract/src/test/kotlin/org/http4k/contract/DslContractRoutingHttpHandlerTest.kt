package org.http4k.contract

import org.http4k.contract.simple.SimpleJson
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson

class FunctionContractRoutingHttpHandlerTest : ContractRoutingHttpHandlerContract() {
    @Suppress("DEPRECATION")
    override val handler =
        ServerFilters.CatchAll()
            .then(
                contract(SimpleJson(Jackson), *contractRoutes.toTypedArray())
            )
}

class DslContractRoutingHttpHandlerTest : ContractRoutingHttpHandlerContract() {
    override val handler =
        ServerFilters.CatchAll()
            .then(
                contract {
                    renderer = SimpleJson(Jackson)
                    routes += contractRoutes.toList()
                }
            )
}
