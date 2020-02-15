package org.http4k.contract.openapi.v2

import argo.jdom.JsonNode
import org.http4k.contract.ContractRendererContract
import org.http4k.contract.bindContract
import org.http4k.contract.contract
import org.http4k.contract.openapi.AddSimpleFieldToRootNode
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.contract.security.ImplicitOAuthSecurity
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Method.*
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.format.Argo
import org.http4k.lens.Query
import org.http4k.routing.bind
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test

class OpenApi2Test : ContractRendererContract<JsonNode>(
    Argo,
    OpenApi2(
        ApiInfo("title", "1.2", "module description"),
        Argo,
        Uri.of("http://example.org:8000"),
        listOf(AddSimpleFieldToRootNode)
    )
) {

    @Test
    fun `renders root path correctly when bind path and root path match`(approver: Approver) {
        val router = "/" bind contract {
            renderer = rendererToUse
            security = ApiKeySecurity(Query.required("the_api_key"), { true })
            routes += "/" bindContract GET to { Response(OK) }
            descriptionPath = "/docs"
        }

        approver.assertApproved(router(Request(GET, "/docs?the_api_key=somevalue")))
    }

    @Test
    fun `renders Google Cloud Endpoints OAuth2`(approver: Approver) {
        val router = "/" bind contract {
            renderer = rendererToUse
            security = ImplicitOAuthSecurity(
                name = "oauth2",
                authorizationUrl = Uri.of(""),
                extraFields = mapOf(
                    "x-google-issuer" to "example-google-issuer",
                    "x-google-jwks_uri" to "http://example.org/jwks",
                    "x-google-audiences" to "client-id1,client-id2"
                ),
                filter = Filter.NoOp
            )
            routes += "/example" bindContract GET to { Response(OK) }
        }

        approver.assertApproved(router(Request(GET, "/")))
    }

}
