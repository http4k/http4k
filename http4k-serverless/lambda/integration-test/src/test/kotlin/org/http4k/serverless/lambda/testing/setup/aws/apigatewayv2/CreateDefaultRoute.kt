package org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiGatewayJackson.auto
import org.http4k.serverless.lambda.testing.setup.aws.kClass

class CreateDefaultRoute(private val apiId: ApiId,
                         private val integrationId: IntegrationId) : AwsApiGatewayV2Action<Unit>(kClass()) {
    override fun toRequest(): Request =
        Request(Method.POST, "/v2/apis/${apiId.value}/routes")
            .with(Body.auto<Route>().toLens() of Route("integrations/${integrationId.value}"))

    private data class Route(val target: String, val routeKey: String = "\$default")
}
