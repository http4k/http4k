package org.http4k.contract

import org.http4k.contract.simple.SimpleJson
import org.http4k.format.Jackson

class FunctionContractRoutingHttpHandlerTest : ContractRoutingHttpHandlerContract() {
    @Suppress("DEPRECATION")
    override val handler = contract(SimpleJson(Jackson), *contractRoutes.toTypedArray())
}

class DslContractRoutingHttpHandlerTest : ContractRoutingHttpHandlerContract() {
    override val handler = contract {
        renderer = SimpleJson(Jackson)
        routes += contractRoutes.toList()
    }
}