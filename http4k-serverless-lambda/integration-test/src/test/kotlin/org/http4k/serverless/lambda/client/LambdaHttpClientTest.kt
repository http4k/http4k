package org.http4k.serverless.lambda.client

import org.http4k.aws.ApiIntegrationVersion
import org.http4k.aws.ApiIntegrationVersion.v1
import org.http4k.aws.ApiIntegrationVersion.v2
import org.http4k.client.HttpClientContract
import org.http4k.core.Request
import org.http4k.core.Response
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Disabled

private fun client(version: ApiIntegrationVersion): (Request) -> Response {
    val functionClient = testFunctionClient(version)
    return { request: Request -> functionClient(request) }
}

abstract class LambdaHttpClientTest(version: ApiIntegrationVersion) :
    HttpClientContract({ NoOpServerConfig }, client(version), client(version)) {

    override fun `handles response with custom status message`() = assumeTrue(false, "Unsupported client feature")
    override fun `connection refused are converted into 503`() = assumeTrue(false, "Unsupported client feature")
    override fun `unknown host are converted into 503`() = assumeTrue(false, "Unsupported client feature")
    override fun `send binary data`() = assumeTrue(false, "Unsupported client feature")
}

class LambdaV1HttpClientTest : LambdaHttpClientTest(v1)

@Disabled("v2 is not supported by the client yet")
class LambdaV2HttpClientTest : LambdaHttpClientTest(v2)
