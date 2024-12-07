package org.http4k.connect.amazon.apigateway.action

import org.http4k.connect.amazon.apigateway.ApiGatewayJackson.auto
import org.http4k.connect.amazon.apigateway.AwsApiGatewayAction
import org.http4k.connect.amazon.apigateway.model.ApiId
import org.http4k.connect.amazon.apigateway.model.DeploymentId
import org.http4k.connect.amazon.apigateway.model.DeploymentName
import org.http4k.connect.kClass
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with

class CreateDeployment(private val apiId: ApiId, private val name: DeploymentName) :
    AwsApiGatewayAction<DeploymentId>(kClass()) {
    override fun toRequest() = Request(Method.POST, "/restapis/${apiId.value}/deployments")
        .with(Body.auto<CreateDeploymentRequest>().toLens() of CreateDeploymentRequest(name.value))

    private data class CreateDeploymentRequest(val name: String)
}
