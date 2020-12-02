package org.http4k.serverless.lambda.client

import org.http4k.client.HttpClientContract
import org.http4k.client.OkHttp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.serverless.lambda.inIntelliJOnly
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Disabled

private fun client(): (Request) -> Response {
    val apiClient = ClientFilters.SetBaseUriFrom(Uri.of("http://http4k-load-balancer-<something>.eu-west-2.elb.amazonaws.com"))
        .then(inIntelliJOnly(DebuggingFilters.PrintRequestAndResponse()))
        .then(OkHttp())
    return { request: Request -> apiClient(request) }
}

@Disabled
class AppLoadBalancerHttpClientTest :
    HttpClientContract({ NoOpServerConfig }, client(), client()) {
    override fun `connection refused are converted into 503`() = assumeTrue(false, "Unsupported client feature")
    override fun `handles response with custom status message`() = assumeTrue(false, "Unsupported client feature")
    override fun `unknown host are converted into 503`()  = assumeTrue(false, "Unsupported client feature")
    override fun `can send multiple headers with same name`() = assumeTrue(false, "Unsupported feature")
    override fun `can receive multiple headers with same name`() = assumeTrue(false, "Unsupported feature")
}
