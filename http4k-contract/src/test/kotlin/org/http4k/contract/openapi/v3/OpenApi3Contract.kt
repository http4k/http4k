package org.http4k.contract.openapi.v3

import org.http4k.contract.ContractRendererContract
import org.http4k.contract.meta
import org.http4k.contract.security.BearerAuthSecurity
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson.auto
import org.http4k.format.Json

abstract class OpenApi3Contract<NODE : Any>(json: Json<NODE>, openApi3: OpenApi3<NODE>) :
    ContractRendererContract<NODE>(json, openApi3) {
    override fun specificRoutes() = listOf(
        "/body_auto_schema" meta {
            receiving(Body.auto<ArbObject2>().toLens() to ArbObject2(
                "s",
                ArbObject1(Foo.bar),
                listOf(1),
                true
            ))
        } bindContract POST to { Response(OK) },
        "/body_auto_schema" meta {
            returning(Status.SEE_OTHER, Body.auto<ArbObject1>().toLens() to ArbObject1(Foo.bing))
        } bindContract GET to { Response(OK) },
        "/bearer_auth" meta {
            security = BearerAuthSecurity("foo")
        } bindContract POST to { Response(OK) }
    )
}