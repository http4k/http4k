package org.http4k.serverless.lambda.testing.setup.aws.apigateway

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.serverless.lambda.testing.setup.aws.apigateway.ApiGatewayJackson.auto
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiId
import org.http4k.serverless.lambda.testing.setup.aws.kClass

class CreateMethod(private val apiId: ApiId, private val resource: RestResourceDetails) :
    AwsApiGatewayAction<Unit>(kClass()) {

    override fun toRequest() = Request(Method.PUT, "/restapis/${apiId.value}/resources/${resource.id}/methods/ANY")
        .with(Body.auto<CreateMethodRequest>().toLens() of CreateMethodRequest(authorizationType = "NONE"))

    private data class CreateMethodRequest(val authorizationType: String)
}
