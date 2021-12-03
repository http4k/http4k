package org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiGatewayJackson.auto
import org.http4k.serverless.lambda.testing.setup.aws.kClass

class CreateStage(private val apiId: ApiId, private val stage: Stage) : AwsApiGatewayV2Action<Unit>(kClass()) {
    override fun toRequest() = Request(Method.POST, "/v2/apis/${apiId.value}/stages")
        .with(Body.auto<Stage>().toLens() of stage)
}
