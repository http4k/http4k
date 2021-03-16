package org.http4k.serverless.lambda.testing.setup.aws.apigateway

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.serverless.lambda.testing.setup.aws.apigateway.ApiGatewayJackson.auto
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiId
import org.http4k.serverless.lambda.testing.setup.aws.kClass

class CreateResource(private val apiId: ApiId, private val parentResource: RestResourceDetails)
    : AwsApiGatewayAction<RestResourceDetails>(kClass()){
    override fun toRequest() = Request(Method.POST, "/restapis/${apiId.value}/resources/${parentResource.id}")
        .with(Body.auto<CreateProxyResource>().toLens() of CreateProxyResource())

    private data class CreateProxyResource(val pathPart: String = "{proxy+}")
}
