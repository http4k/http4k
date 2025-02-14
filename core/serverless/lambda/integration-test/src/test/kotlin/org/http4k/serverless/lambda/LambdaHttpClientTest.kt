package org.http4k.serverless.lambda

import org.http4k.aws.awsCliUserProfiles
import org.http4k.aws.awsClientFor
import org.http4k.client.HttpClientContract
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.lambda.model.Function
import org.http4k.connect.amazon.lambda.model.LambdaIntegrationType
import org.http4k.connect.amazon.lambda.model.LambdaIntegrationType.ApiGatewayV1
import org.http4k.connect.amazon.lambda.model.LambdaIntegrationType.ApiGatewayV2
import org.http4k.connect.amazon.lambda.model.LambdaIntegrationType.ApplicationLoadBalancer
import org.http4k.connect.amazon.lambda.model.LambdaIntegrationType.Invocation
import org.http4k.core.then
import org.http4k.serverless.lambda.testing.NoOpServerConfig
import org.http4k.serverless.lambda.testing.client.ApiGatewayV1LambdaClient
import org.http4k.serverless.lambda.testing.client.ApiGatewayV2LambdaClient
import org.http4k.serverless.lambda.testing.client.ApplicationLoadBalancerLambdaClient
import org.http4k.serverless.lambda.testing.client.InvocationLambdaClient
import org.http4k.serverless.lambda.testing.client.LambdaHttpClient
import org.http4k.serverless.lambda.testing.setup.DeployServerAsLambdaForClientContract.functionName
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Disabled

abstract class LambdaHttpClientTest(
    type: LambdaIntegrationType,
    clientFn: (Function, Region) -> LambdaHttpClient
) : HttpClientContract(
    { _, _ -> NoOpServerConfig },
    clientFn(functionName(type), Region.of(awsCliUserProfiles().profile("http4k-integration-test").region))
        .then(awsCliUserProfiles().profile("http4k-integration-test").awsClientFor("lambda"))
) {
    override fun `handles response with custom status message`() = unsupportedFeature()
    override fun `connection refused are converted into 503`() = unsupportedFeature()
    override fun `unknown host are converted into 503`() = unsupportedFeature()
    override fun `send binary data`() = unsupportedFeature()
    override fun `supports query parameter list`() = unsupportedFeature()
    override fun `socket timeouts are converted into 504`() = unsupportedFeature()
    override fun `fails with no protocol`() = unsupportedFeature()
    override fun `unknown host is correctly reported`() = unsupportedFeature()
}

class LambdaV1HttpClientTest : LambdaHttpClientTest(ApiGatewayV1, ::ApiGatewayV1LambdaClient) {
    override fun `can send multiple headers with same name`() = unsupportedFeature()
    override fun `can receive multiple headers with same name`() = unsupportedFeature()
    override fun `can receive multiple cookies`() = unsupportedFeature()
}

class LambdaV2HttpClientTest : LambdaHttpClientTest(ApiGatewayV2, ::ApiGatewayV2LambdaClient)

class LambdaAlbHttpClientTest : LambdaHttpClientTest(ApplicationLoadBalancer, ::ApplicationLoadBalancerLambdaClient) {
    override fun `can send multiple headers with same name`() = unsupportedFeature()
    override fun `can receive multiple headers with same name`() = unsupportedFeature()
    override fun `can receive multiple cookies`() = unsupportedFeature()
}

@Disabled
class InvocationLambdaClientTest : LambdaHttpClientTest(Invocation, ::InvocationLambdaClient)

private fun unsupportedFeature() = assumeTrue(false, "Unsupported feature")
