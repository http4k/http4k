package org.http4k.contract.openapi.v3

import org.http4k.contract.BearerAuthSecurity
import org.http4k.contract.ContractRendererContract
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.format.Jackson
import org.http4k.format.Jackson.auto

class OpenApi3Test : ContractRendererContract(OpenApi3(ApiInfo("title", "1.2", "module description"), Jackson)) {
    override fun specificRoutes() = listOf(
        "/body_auto_schema" meta {
            receiving(Body.auto<ArbObject2>().toLens() to ArbObject2(
                "s",
                ArbObject1(Foo.bar),
                listOf(1),
                true
            ))
        } bindContract POST to { Response(Status.OK) },
        "/body_auto_schema" meta {
            returning(SEE_OTHER, Body.auto<ArbObject1>().toLens() to ArbObject1(Foo.bing))
        } bindContract GET to { Response(Status.OK) },
        "/bearer_auth" meta {
            security = BearerAuthSecurity("foo")
        } bindContract POST to { Response(Status.OK) }
    )
}