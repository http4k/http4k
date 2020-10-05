package org.http4k.serverless.lambda.client

import org.http4k.client.HttpClientContract
import org.http4k.client.OkHttp
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.junit.jupiter.api.Disabled

val apiClient = ClientFilters.SetBaseUriFrom(Uri.of("https://something.execute-api.us-east-1.amazonaws.com/")).then(OkHttp())

@Disabled
class ApiGatewayHttpClientTest : HttpClientContract({ NoOpServerConfig }, apiClient, apiClient)
