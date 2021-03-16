package org.http4k.serverless.lambda

import org.http4k.aws.awsCliUserProfiles
import org.http4k.client.HttpClientContract
import org.http4k.client.OkHttp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.inIntelliJOnly
import org.http4k.serverless.lambda.testing.NoOpServerConfig
import org.http4k.serverless.lambda.testing.client.restApiGatewayApiClient
import org.http4k.serverless.lambda.testing.setup.DeployRestApiGateway
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiIntegrationVersion
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiIntegrationVersion.v1
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.opentest4j.TestAbortedException

private fun client(version: ApiIntegrationVersion): (Request) -> Response {
    val api = awsCliUserProfiles().profile("http4k-integration-test")
        .restApiGatewayApiClient().listApis()
        .find { it.name == DeployRestApiGateway.apiName() }
        ?: throw TestAbortedException("API hasn't been deployed")
    val apiClient = ClientFilters.SetBaseUriFrom(api.apiEndpoint)
        .then(DebuggingFilters.PrintRequestAndResponse().inIntelliJOnly())
        .then(OkHttp())
    return { request: Request -> apiClient(request) }
}

abstract class ApiGatewayRestHttpClientTest(version: ApiIntegrationVersion) :
    HttpClientContract({ NoOpServerConfig }, client(version), client(version)) {
    override fun `connection refused are converted into 503`() = assumeTrue(false, "Unsupported client feature")
    override fun `handles response with custom status message`() = assumeTrue(false, "Unsupported client feature")
    override fun `unknown host are converted into 503`() = assumeTrue(false, "Unsupported client feature")
}

class ApiGatewayRestV1ClientTest : ApiGatewayRestHttpClientTest(v1) {
    override fun `can send multiple headers with same name`() = assumeTrue(false, "Unsupported feature")
    override fun `can receive multiple headers with same name`() = assumeTrue(false, "Unsupported feature")
    override fun `can receive multiple cookies`() = assumeTrue(false, "Unsupported feature")
    override fun `socket timeouts are converted into 504`() = assumeTrue(false, "Unsupported feature")
    override fun `send binary data`() = assumeTrue(false, "Unsupported feature")
}
