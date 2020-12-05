package org.http4k.serverless.lambda.client

import org.http4k.aws.FunctionName
import org.http4k.aws.LambdaIntegrationType
import org.http4k.aws.LambdaIntegrationType.ApiGatewayV1
import org.http4k.aws.LambdaIntegrationType.ApiGatewayV2
import org.http4k.aws.LambdaIntegrationType.ApplicationLoadBalancer
import org.http4k.aws.LambdaIntegrationType.Invocation
import org.http4k.aws.Region
import org.http4k.client.HttpClientContract
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Disabled

abstract class LambdaHttpClientTest(type: LambdaIntegrationType,
                                    clientFn: (FunctionName, Region) -> LambdaHttpClient<*, *>) :
    HttpClientContract({ NoOpServerConfig }, testFunctionClient(type, clientFn), testFunctionClient(type, clientFn)) {

    override fun `handles response with custom status message`() = unsupportedFeature()
    override fun `connection refused are converted into 503`() = unsupportedFeature()
    override fun `unknown host are converted into 503`() = unsupportedFeature()
    override fun `send binary data`() = unsupportedFeature()
}

class LambdaV1HttpClientTest : LambdaHttpClientTest(ApiGatewayV1, ::ApiGatewayV1LambdaClient){
    override fun `can send multiple headers with same name`() = unsupportedFeature()
    override fun `can receive multiple headers with same name`() = unsupportedFeature()
    override fun `can receive multiple cookies`() = unsupportedFeature()
}

class LambdaV2HttpClientTest : LambdaHttpClientTest(ApiGatewayV2, ::ApiGatewayV2LambdaClient)

class LambdaAlbHttpClientTest : LambdaHttpClientTest(ApplicationLoadBalancer, ::ApplicationLoadBalancerLambdaClient){
    override fun `can send multiple headers with same name`() = unsupportedFeature()
    override fun `can receive multiple headers with same name`() = unsupportedFeature()
    override fun `can receive multiple cookies`() = unsupportedFeature()
}

@Disabled
class InvocationLambdaClientTest : LambdaHttpClientTest(Invocation, ::InvocationLambdaClient)

private fun unsupportedFeature() = assumeTrue(false, "Unsupported feature")

