package org.http4k.serverless.lambda.testing.setup.aws.apigateway

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.serverless.lambda.testing.setup.aws.apigateway.ApiGatewayJackson.auto
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiId
import org.http4k.serverless.lambda.testing.setup.aws.kClass
import org.http4k.serverless.lambda.testing.setup.aws.lambda.Region

class CreateIntegration(private val apiId: ApiId,
                        private val resource: RestResourceDetails,
                        private val functionArn: String,
                        private val region: Region) :
    AwsApiGatewayAction<Unit>(kClass()) {

    override fun toRequest() =
        Request(Method.PUT, "/restapis/${apiId.value}/resources/${resource.id}/methods/ANY/integration")
            .with(Body.auto<CreateMethodWithIntegration>().toLens() of
                CreateMethodWithIntegration(uri = functionArn.invocation(region)))

    private data class CreateMethodWithIntegration(
        val type: String = "AWS_PROXY",
        val uri: String,
        val httpMethod: String = "POST"
    )

    private fun String.invocation(region: Region): String =
        "arn:aws:apigateway:${region.name}:lambda:path/2015-03-31/functions/${this}/invocations"
}
