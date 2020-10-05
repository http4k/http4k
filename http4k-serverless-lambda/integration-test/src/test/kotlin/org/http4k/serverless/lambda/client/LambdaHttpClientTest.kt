package org.http4k.serverless.lambda.client

import org.http4k.aws.FunctionName
import org.http4k.client.HttpClientContract
import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach

private val lazyClient by lazy {
        val config = Environment.ENV overrides Environment.fromResource("/local.properties")
        val region = Config.region(config)
        val client = Filter.NoOp
            .then(ClientFilters.AwsAuth(Config.scope(config), Config.credentials(config)))
            .then(JavaHttpClient())

        LambdaHttpClient(FunctionName("test-function"), region).then(client)
}

private val client = { request: Request -> lazyClient(request) }

class LambdaHttpClientTest : HttpClientContract({ NoOpServerConfig }, client, client) {

    @BeforeEach
    fun ensureLocalPropertiesExist(){
        assumeTrue(LambdaHttpClientTest::class.java.getResourceAsStream("/local.properties") != null,
            "local.properties must exist for this test to run")
    }

    override fun `handles response with custom status message`() = assumeTrue(false, "Unsupported client feature")
    override fun `connection refused are converted into 503`() = assumeTrue(false, "Unsupported client feature")
    override fun `unknown host are converted into 503`() = assumeTrue(false, "Unsupported client feature")
    override fun `send binary data`() = assumeTrue(false, "Unsupported client feature")
}
