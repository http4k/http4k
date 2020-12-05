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

abstract class LambdaHttpClientTest(type: LambdaIntegrationType,
                                    clientFn: (FunctionName, Region) -> LambdaHttpClient<*, *>) :
    HttpClientContract({ NoOpServerConfig }, testFunctionClient(type, clientFn), testFunctionClient(type, clientFn)) {

    override fun `handles response with custom status message`() = assumeTrue(false, "Unsupported client feature")
    override fun `connection refused are converted into 503`() = assumeTrue(false, "Unsupported client feature")
    override fun `unknown host are converted into 503`() = assumeTrue(false, "Unsupported client feature")
    override fun `send binary data`() = assumeTrue(false, "Unsupported client feature")
}

class LambdaV1HttpClientTest : LambdaHttpClientTest(ApiGatewayV1, ::ApiGatewayV1LambdaClient){
    override fun `can send multiple headers with same name`() = assumeTrue(false, "Unsupported feature")
    override fun `can receive multiple headers with same name`() = assumeTrue(false, "Unsupported feature")
    override fun `can receive multiple cookies`() = assumeTrue(false, "Unsupported feature")
}

class LambdaV2HttpClientTest : LambdaHttpClientTest(ApiGatewayV2, ::ApiGatewayV2LambdaClient)

class LambdaAlbHttpClientTest : LambdaHttpClientTest(ApplicationLoadBalancer, ::ApplicationLoadBalancerLambdaClient){
    override fun `can send multiple headers with same name`() = assumeTrue(false, "Unsupported feature")
    override fun `can receive multiple headers with same name`() = assumeTrue(false, "Unsupported feature")
    override fun `can receive multiple cookies`() = assumeTrue(false, "Unsupported feature")
}

class InvocationLambdaClientTest : LambdaHttpClientTest(Invocation, ::InvocationLambdaClient){
}
