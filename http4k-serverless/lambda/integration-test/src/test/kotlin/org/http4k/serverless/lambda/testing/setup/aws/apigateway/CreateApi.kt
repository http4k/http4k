package org.http4k.serverless.lambda.testing.setup.aws.apigateway

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.serverless.lambda.testing.setup.aws.apigateway.ApiGatewayJackson.auto
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiName
import org.http4k.serverless.lambda.testing.setup.aws.kClass

class CreateApi(val name: ApiName) : AwsApiGatewayAction<RestApiDetails>(kClass()) {
    override fun toRequest() = Request(Method.POST, "/restapis")
        .with(Body.auto<RestApi>().toLens() of RestApi(name.value))

    private data class RestApi(val name: String)
}
