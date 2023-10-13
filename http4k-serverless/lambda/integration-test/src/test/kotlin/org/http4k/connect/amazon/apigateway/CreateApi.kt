package org.http4k.connect.amazon.apigateway

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.connect.amazon.apigateway.ApiGatewayJackson.auto
import org.http4k.connect.amazon.apigatewayv2.model.ApiName
import org.http4k.connect.amazon.kClass

class CreateApi(val name: ApiName) : AwsApiGatewayAction<RestApiDetails>(kClass()) {
    override fun toRequest() = Request(Method.POST, "/restapis")
        .with(Body.auto<RestApi>().toLens() of RestApi(name.value))

    private data class RestApi(val name: String)
}
