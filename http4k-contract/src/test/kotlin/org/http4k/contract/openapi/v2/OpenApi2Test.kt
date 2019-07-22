package org.http4k.contract.openapi.v2

import argo.jdom.JsonNode
import org.http4k.contract.*
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.core.*
import org.http4k.format.Argo
import org.http4k.lens.*
import org.http4k.routing.bind
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test

class OpenApi2Test : ContractRendererContract<JsonNode>(Argo, OpenApi2(ApiInfo("title", "1.2", "module description"), Argo)) {

    @Test
    fun `renders root path correctly when bind path and root path match`(approver: Approver) {

        val router = "/" bind contract {
            renderer = rendererToUse
            security = ApiKeySecurity(Query.required("the_api_key"), { true })
            routes += "/" bindContract Method.GET to { Response(Status.OK) }
            descriptionPath = "/docs"
        }

        approver.assertApproved(router(Request(Method.GET, "/docs?the_api_key=somevalue")))
    }

}
