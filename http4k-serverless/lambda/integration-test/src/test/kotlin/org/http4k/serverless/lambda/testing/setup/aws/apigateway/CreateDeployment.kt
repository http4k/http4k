package org.http4k.serverless.lambda.testing.setup.aws.apigateway

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.serverless.lambda.testing.setup.aws.apigateway.ApiGatewayJackson.auto
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiId
import org.http4k.serverless.lambda.testing.setup.aws.kClass

class CreateDeployment(private val apiId: ApiId, private val name: DeploymentName)
    : AwsApiGatewayAction<DeploymentId>(kClass()) {
    override fun toRequest() = Request(Method.POST, "/restapis/${apiId.value}/deployments")
        .with(Body.auto<CreateDeploymentRequest>().toLens() of CreateDeploymentRequest(name.value))

    private data class CreateDeploymentRequest(val name: String)
}
