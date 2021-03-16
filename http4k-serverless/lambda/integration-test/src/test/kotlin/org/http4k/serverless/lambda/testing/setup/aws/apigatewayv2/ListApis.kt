package org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.serverless.lambda.testing.setup.aws.kClass

class ListApis : AwsApiGatewayV2Action<ListApiResponse>(kClass()) {
    override fun toRequest() = Request(Method.GET, "/v2/apis")
}
