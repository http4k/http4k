package org.http4k.connect.amazon.apigateway.action

import org.http4k.connect.amazon.apigateway.ApiGatewayJackson.auto
import org.http4k.connect.amazon.apigateway.AwsApiGatewayAction
import org.http4k.connect.amazon.apigateway.model.ApiId
import org.http4k.connect.amazon.apigateway.model.DeploymentId
import org.http4k.connect.amazon.apigateway.model.Stage
import org.http4k.connect.kClass
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with

class CreateStage(private val apiId: ApiId, private val stage: Stage, private val deploymentId: DeploymentId) :
    AwsApiGatewayAction<Unit>(kClass()) {
    override fun toRequest() = Request(Method.POST, "/restapis/${apiId.value}/stages")
        .with(Body.auto<CreateStage>().toLens() of CreateStage(stage.stageName.value, deploymentId.id))

    private data class CreateStage(val stageName: String, val deploymentId: String)
}
