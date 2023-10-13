package org.http4k.connect.amazon.apigateway

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.connect.amazon.apigateway.ApiGatewayJackson.auto
import org.http4k.connect.amazon.apigatewayv2.model.ApiId
import org.http4k.connect.amazon.kClass

class CreateIntegrationResponse(private val apiId: ApiId,
                                private val resource: RestResourceDetails) : AwsApiGatewayAction<Unit>(kClass()) {
    override fun toRequest() =
        Request(Method.PUT, "/restapis/${apiId.value}/resources/${resource.id}/methods/ANY/integration/responses/200")
            .with(Body.auto<CreateIntegrationResponseRequest>().toLens() of CreateIntegrationResponseRequest())

    private data class CreateIntegrationResponseRequest(val selectionPattern: String = "\\d{3}",
                                                        val contentHandling: String = "CONVERT_TO_TEXT")
}
