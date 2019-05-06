package org.http4k.contract.openapi3

import org.http4k.contract.ApiInfo
import org.http4k.contract.BearerAuthSecurity
import org.http4k.contract.ContractRendererContract
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.format.Jackson.auto

data class ArbObject1(val anotherString: String)
data class ArbObject2(val string: String, val child: ArbObject1?, val numbers: List<Int>, val bool: Boolean)

class OpenApi3Test : ContractRendererContract(OpenApi3(ApiInfo("title", "1.2", "module description"))) {

    @Disabled
    override fun `renders as expected`(approver: Approver) {
    }

    override fun specificRoutes() = listOf(
        "/body_auto_schema" meta {
            receiving(Body.auto<ArbObject2>().toLens() to ArbObject2(
                "s",
                ArbObject1("s2"),
                listOf(1),
                true
            ))
        } bindContract POST to { Response(OK) },
        "/body_auto_schema" meta {
            returning(SEE_OTHER, Body.auto<ArbObject1>().toLens() to ArbObject1("s2"))
        } bindContract GET to { Response(OK) },
        "/bearer_auth" meta {
            security = BearerAuthSecurity("foo")
        } bindContract POST to { Response(OK) }
    )
}