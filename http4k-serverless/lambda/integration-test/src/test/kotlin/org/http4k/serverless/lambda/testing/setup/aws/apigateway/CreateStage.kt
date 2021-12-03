package org.http4k.serverless.lambda.testing.setup.aws.apigateway

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.serverless.lambda.testing.setup.aws.apigateway.ApiGatewayJackson.auto
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiId
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.Stage
import org.http4k.serverless.lambda.testing.setup.aws.kClass

class CreateStage(private val apiId: ApiId, private val stage: Stage, private val deploymentId: DeploymentId)
    : AwsApiGatewayAction<Unit>(kClass()) {
    override fun toRequest() = Request(Method.POST, "/restapis/${apiId.value}/stages")
        .with(Body.auto<CreateStage>().toLens() of CreateStage(stage.stageName.value, deploymentId.id))

    private data class CreateStage(val stageName: String, val deploymentId: String)
}
