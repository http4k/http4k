package org.http4k.serverless.lambda.client

import org.http4k.client.HttpClientContract
import org.http4k.core.Request
import org.junit.jupiter.api.Assumptions.assumeTrue

private val client = { request: Request -> testFunctionClient(request) }

class LambdaHttpClientTest : HttpClientContract({ NoOpServerConfig }, client, client) {
    override fun `handles response with custom status message`() = assumeTrue(false, "Unsupported client feature")
    override fun `connection refused are converted into 503`() = assumeTrue(false, "Unsupported client feature")
    override fun `unknown host are converted into 503`() = assumeTrue(false, "Unsupported client feature")
    override fun `send binary data`() = assumeTrue(false, "Unsupported client feature")
}
