package org.http4k.serverless.lambda.client

import org.http4k.client.HttpClientContract
import org.http4k.client.OkHttp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.serverless.lambda.inIntelliJOnly
import org.junit.jupiter.api.Disabled

private fun client(): (Request) -> Response {
    val apiClient = SetBaseUriFrom(Uri.of("http://http4k-direct-<something>.eu-west-2.amazonaws.com"))
        .then(inIntelliJOnly(PrintRequestAndResponse()))
        .then(OkHttp())
    return { request: Request -> apiClient(request) }
}

@Disabled
class DirectHttpClientTest : HttpClientContract({ NoOpServerConfig }, client(), client())
