package org.http4k.connect.amazon.apigatewayv2.action

import org.http4k.connect.amazon.apigatewayv2.ApiGatewayJackson.auto
import org.http4k.connect.amazon.apigatewayv2.AwsApiGatewayV2Action
import org.http4k.connect.amazon.apigatewayv2.model.ApiId
import org.http4k.connect.amazon.apigatewayv2.model.ApiIntegrationVersion
import org.http4k.connect.amazon.apigatewayv2.model.Integration
import org.http4k.connect.amazon.apigatewayv2.model.IntegrationInfo
import org.http4k.connect.kClass
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with

class CreateLambdaIntegration(
    private val apiId: ApiId,
    private val functionArn: String,
    private val version: ApiIntegrationVersion
) : AwsApiGatewayV2Action<IntegrationInfo>(kClass()) {
    private val createIntegrationLens =
        Body.auto<Integration>(contentType = ContentType.APPLICATION_JSON.withNoDirectives()).toLens()

    override fun toRequest() = Request(Method.POST, "/v2/apis/${apiId.value}/integrations")
        .with(
            createIntegrationLens of Integration(
                integrationUri = functionArn,
                payloadFormatVersion = when (version) {
                    ApiIntegrationVersion.v1 -> "1.0"
                    ApiIntegrationVersion.v2 -> "2.0"
                }
            )
        )
}
