package org.http4k.serverless.lambda.client

import org.http4k.aws.LambdaIntegrationType
import org.http4k.aws.LambdaIntegrationType.ApiGatewayV1
import org.http4k.aws.LambdaIntegrationType.ApiGatewayV2
import org.http4k.aws.LambdaIntegrationType.ApplicationLoadBalancer
import org.http4k.aws.LambdaIntegrationType.Direct
import org.http4k.client.HttpClientContract
import org.http4k.core.Request
import org.http4k.core.Response
import org.junit.jupiter.api.Assumptions.assumeTrue

private fun client(version: LambdaIntegrationType): (Request) -> Response {
    val functionClient = testFunctionClient(version)
    return { request: Request -> functionClient(request) }
}

abstract class LambdaHttpClientTest(version: LambdaIntegrationType) :
    HttpClientContract({ NoOpServerConfig }, client(version), client(version)) {

    override fun `handles response with custom status message`() = assumeTrue(false, "Unsupported client feature")
    override fun `connection refused are converted into 503`() = assumeTrue(false, "Unsupported client feature")
    override fun `unknown host are converted into 503`() = assumeTrue(false, "Unsupported client feature")
    override fun `send binary data`() = assumeTrue(false, "Unsupported client feature")
}

class LambdaV1HttpClientTest : LambdaHttpClientTest(ApiGatewayV1)

class LambdaV2HttpClientTest : LambdaHttpClientTest(ApiGatewayV2)

class LambdaAlbHttpClientTest : LambdaHttpClientTest(ApplicationLoadBalancer)

class LambdaDirectHttpClientTest : LambdaHttpClientTest(Direct)
