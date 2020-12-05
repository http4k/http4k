package org.http4k.serverless.lambda.client

import org.http4k.aws.AwsApiGatewayApiClient
import org.http4k.aws.AwsCredentialScope
import org.http4k.aws.AwsCredentials
import org.http4k.aws.AwsLambdaApiClient
import org.http4k.aws.FunctionName
import org.http4k.aws.LambdaIntegrationType
import org.http4k.aws.Region
import org.http4k.aws.Role
import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.then
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.serverless.lambda.DeployServerAsLambdaForClientContract
import org.http4k.serverless.lambda.inIntelliJOnly
import org.junit.jupiter.api.Assumptions.assumeTrue

fun testFunctionClient(type: LambdaIntegrationType, clientFn: (FunctionName, Region) -> LambdaHttpClient<*, *>) =
    clientFn(DeployServerAsLambdaForClientContract.functionName(type), Config.region(awsConfig)).then(awsClient("lambda"))

val lambdaApiClient by lazy { AwsLambdaApiClient(awsClient("lambda"), Config.region(awsConfig)) }

val apiGatewayClient by lazy { AwsApiGatewayApiClient(awsClient("apigateway"), Config.region(awsConfig)) }

private fun awsClient(service: String) = Filter.NoOp
    .then(ClientFilters.AwsAuth(Config.scope(awsConfig, service), Config.credentials(awsConfig)))
    .then(inIntelliJOnly(DebuggingFilters.PrintRequestAndResponse()))
    .then(JavaHttpClient())

val awsConfig by lazy {
    assumeTrue(AwsLambdaApiClient::class.java.getResourceAsStream("/local.properties") != null,
        "local.properties must exist for this test to run")
    Environment.ENV overrides Environment.fromResource("/local.properties")
}

object Config {
    private val regionKey = EnvironmentKey.map(::Region).required("region")
    private val roleKey = EnvironmentKey.map(::Role).required("lambdaRuntimeRole")

    fun credentials(config: Environment) =
        AwsCredentials(EnvironmentKey.required("accessKey")(config), EnvironmentKey.required("secretKey")(config))

    fun region(config: Environment) = regionKey(config)
    fun role(config: Environment) = roleKey(config)
    fun scope(config: Environment, service: String) = AwsCredentialScope(region(config).name, service)
}
