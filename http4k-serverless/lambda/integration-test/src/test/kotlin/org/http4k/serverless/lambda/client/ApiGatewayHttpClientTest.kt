package org.http4k.serverless.lambda.client

import org.http4k.aws.ApiIntegrationVersion
import org.http4k.aws.ApiIntegrationVersion.v1
import org.http4k.aws.ApiIntegrationVersion.v2
import org.http4k.client.HttpClientContract
import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.serverless.lambda.DeployApiGateway.apiName
import org.http4k.serverless.lambda.inIntelliJOnly
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.opentest4j.TestAbortedException

private fun client(version: ApiIntegrationVersion): HttpHandler {
    val api = apiGatewayClient.listApis().find { it.name == apiName(version) }
        ?: throw TestAbortedException("API hasn't been deployed")
    return ClientFilters.SetBaseUriFrom(api.apiEndpoint)
        .then(inIntelliJOnly(DebuggingFilters.PrintRequestAndResponse()))
        .then(OkHttp())
}

abstract class ApiGatewayHttpClientTest(version: ApiIntegrationVersion) :
    HttpClientContract({ NoOpServerConfig }, client(version), client(version)) {
    override fun `connection refused are converted into 503`() = assumeTrue(false, "Unsupported client feature")
    override fun `handles response with custom status message`() = assumeTrue(false, "Unsupported client feature")
    override fun `unknown host are converted into 503`() = assumeTrue(false, "Unsupported client feature")
}

class ApiGatewayV1ClientTest : ApiGatewayHttpClientTest(v1)

class ApiGatewayV2ClientTest : ApiGatewayHttpClientTest(v2)
