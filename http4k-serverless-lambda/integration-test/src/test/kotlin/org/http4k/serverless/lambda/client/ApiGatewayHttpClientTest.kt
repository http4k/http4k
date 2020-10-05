package org.http4k.serverless.lambda.client

import org.http4k.aws.ApiIntegrationVersion
import org.http4k.aws.ApiIntegrationVersion.v1
import org.http4k.client.HttpClientContract
import org.http4k.client.OkHttp
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.serverless.lambda.DeployApiGateway.apiName
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Disabled
import org.opentest4j.TestAbortedException

private val apiClient by lazy {
    val api = apiGatewayClient.listApis().find { it.name == apiName(v1) }
        ?: throw TestAbortedException("API hasn't been deployed")
    ClientFilters.SetBaseUriFrom(api.apiEndpoint).then(OkHttp())
}

private val client = { request: Request -> apiClient(request) }

class ApiGatewayHttpClientTest : HttpClientContract({ NoOpServerConfig }, client, client) {
    override fun `connection refused are converted into 503`() = assumeTrue(false, "Unsupported client feature")
    override fun `handles response with custom status message`() = assumeTrue(false, "Unsupported client feature")
    override fun `unknown host are converted into 503`()  = assumeTrue(false, "Unsupported client feature")
}

