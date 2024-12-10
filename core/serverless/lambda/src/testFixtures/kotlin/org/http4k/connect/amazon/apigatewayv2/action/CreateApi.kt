package org.http4k.connect.amazon.apigatewayv2.action

import org.http4k.connect.amazon.apigatewayv2.ApiGatewayJackson.auto
import org.http4k.connect.amazon.apigatewayv2.AwsApiGatewayV2Action
import org.http4k.connect.amazon.apigatewayv2.model.ApiDetails
import org.http4k.connect.amazon.apigatewayv2.model.ApiName
import org.http4k.connect.kClass
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with

class CreateApi(val name: ApiName) : AwsApiGatewayV2Action<ApiDetails>(kClass()) {
    private val createApiLens =
        Body.auto<Api>(contentType = ContentType.APPLICATION_JSON.withNoDirectives()).toLens()

    override fun toRequest(): Request = Request(Method.POST, "/v2/apis")
        .with(createApiLens of Api(name.value))

    private data class Api(val name: String, val protocolType: String = "HTTP")
}
