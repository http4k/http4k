package org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.serverless.lambda.testing.setup.aws.kClass

class DeleteApi(private val apiId: ApiId) : AwsApiGatewayV2Action<Unit>(kClass()) {
    override fun toRequest() = Request(Method.DELETE, "/v2/apis/${apiId.value}")
}
