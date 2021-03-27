package org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiGatewayJackson.auto
import org.http4k.serverless.lambda.testing.setup.aws.kClass

class CreateApi(val name: ApiName) : AwsApiGatewayV2Action<ApiDetails>(kClass()) {
    override fun toRequest() = Request(Method.POST, "/v2/apis")
        .with(Body.auto<Api>().toLens() of Api(name.value))

    private data class Api(val name: String, val protocolType: String = "HTTP")
}
