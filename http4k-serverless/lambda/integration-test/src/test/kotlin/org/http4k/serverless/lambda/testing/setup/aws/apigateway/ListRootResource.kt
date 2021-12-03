package org.http4k.serverless.lambda.testing.setup.aws.apigateway

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiId
import org.http4k.serverless.lambda.testing.setup.aws.kClass

class ListRootResource(private val apiId: ApiId) : AwsApiGatewayAction<ListResourcesResponse>(kClass()) {
    override fun toRequest(): Request = Request(Method.GET, "/restapis/${apiId.value}/resources")
}
