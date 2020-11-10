package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.contract.simple.SimpleJson
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.routes
import org.http4k.routing.bind
import org.junit.jupiter.api.Test

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

    @Test
    fun `can bind under a route match`() {
        val app =
            routes("/hello" bind routes("/there" bind
                contract {
                    renderer = SimpleJson(Jackson)
                    routes += validPath bindContract GET to { Response(OK) }
                })
            )
        assertThat(app(Request(GET, "/hello/there$validPath")), hasStatus(OK))
    }
}
