package org.http4k.serverless.lambda.client

import org.http4k.client.HttpClientContract
import org.http4k.client.OkHttp
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Disabled

private val apiClient = ClientFilters.SetBaseUriFrom(Uri.of("https://<apiId>.execute-api.us-east-1.amazonaws.com/")).then(OkHttp())

@Disabled
class ApiGatewayHttpClientTest : HttpClientContract({ NoOpServerConfig }, apiClient, apiClient) {
    override fun `connection refused are converted into 503`() = Assumptions.assumeTrue(false, "Unsupported client feature")
    override fun `handles response with custom status message`() = Assumptions.assumeTrue(false, "Unsupported client feature")
    override fun `unknown host are converted into 503`()  = Assumptions.assumeTrue(false, "Unsupported client feature")
}
