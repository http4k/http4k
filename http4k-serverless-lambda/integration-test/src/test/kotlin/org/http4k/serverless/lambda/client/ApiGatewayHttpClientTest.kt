package org.http4k.serverless.lambda.client

import org.http4k.aws.ApiIntegrationVersion
import org.http4k.aws.ApiIntegrationVersion.v1
import org.http4k.aws.ApiIntegrationVersion.v2
import org.http4k.client.HttpClientContract
import org.http4k.client.OkHttp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.serverless.lambda.DeployApiGateway.apiName
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Disabled
import org.opentest4j.TestAbortedException

private fun client(version: ApiIntegrationVersion): (Request) -> Response {
    val api = apiGatewayClient.listApis().find { it.name == apiName(version) }
        ?: throw TestAbortedException("API hasn't been deployed")
    val apiClient = ClientFilters.SetBaseUriFrom(api.apiEndpoint).then(OkHttp())
    return { request: Request -> apiClient(request) }
}

abstract class ApiGatewayHttpClientTest(version: ApiIntegrationVersion) :
    HttpClientContract({ NoOpServerConfig }, client(version), client(version)) {
    override fun `connection refused are converted into 503`() = assumeTrue(false, "Unsupported client feature")
    override fun `handles response with custom status message`() = assumeTrue(false, "Unsupported client feature")
    override fun `unknown host are converted into 503`()  = assumeTrue(false, "Unsupported client feature")
}

class ApiGatewayV1ClientTest:ApiGatewayHttpClientTest(v1)

@Disabled("v2 is not supported by the client yet")
class ApiGatewayV2ClientTest:ApiGatewayHttpClientTest(v2)
