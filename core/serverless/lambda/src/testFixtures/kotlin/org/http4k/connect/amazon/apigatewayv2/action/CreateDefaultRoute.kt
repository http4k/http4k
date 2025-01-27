package org.http4k.connect.amazon.apigatewayv2.action

import org.http4k.connect.amazon.apigatewayv2.ApiGatewayJackson.auto
import org.http4k.connect.amazon.apigatewayv2.AwsApiGatewayV2Action
import org.http4k.connect.amazon.apigatewayv2.model.ApiId
import org.http4k.connect.amazon.apigatewayv2.model.IntegrationId
import org.http4k.connect.kClass
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with

class CreateDefaultRoute(
    private val apiId: ApiId,
    private val integrationId: IntegrationId
) : AwsApiGatewayV2Action<Unit>(kClass()) {
    private val createRouteLens =
        Body.auto<Route>(contentType = ContentType.APPLICATION_JSON.withNoDirectives()).toLens()

    override fun toRequest() = Request(Method.POST, "/v2/apis/${apiId.value}/routes")
        .with(createRouteLens of Route("integrations/${integrationId.value}"))

    private data class Route(val target: String, val routeKey: String = "\$default")
}
