package org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiGatewayJackson.auto
import org.http4k.serverless.lambda.testing.setup.aws.kClass

class CreateLambdaIntegration(
    private val apiId: ApiId,
    private val functionArn: String,
    private val version: ApiIntegrationVersion) : AwsApiGatewayV2Action<IntegrationInfo>(kClass()) {
    override fun toRequest() = Request(Method.POST, "/v2/apis/${apiId.value}/integrations")
        .with(Body.auto<Integration>().toLens() of Integration(
            integrationUri = functionArn,
            payloadFormatVersion = when (version) {
                ApiIntegrationVersion.v1 -> "1.0"
                ApiIntegrationVersion.v2 -> "2.0"
            }
        ))
}
