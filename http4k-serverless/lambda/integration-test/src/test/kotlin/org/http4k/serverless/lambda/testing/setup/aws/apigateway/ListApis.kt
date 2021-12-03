package org.http4k.serverless.lambda.testing.setup.aws.apigateway

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.serverless.lambda.testing.setup.aws.kClass

class ListApis : AwsApiGatewayAction<ListApiResponse>(kClass()) {
    override fun toRequest() = Request(Method.GET, "/restapis")
}
