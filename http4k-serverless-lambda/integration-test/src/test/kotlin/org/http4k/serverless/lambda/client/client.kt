package org.http4k.serverless.lambda.client

import org.http4k.aws.AwsLambdaApiClient
import org.http4k.aws.FunctionName
import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.NoOp
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.junit.jupiter.api.Assumptions.assumeTrue

private val awsConfig by lazy {
    assumeTrue(AwsLambdaApiClient::class.java.getResourceAsStream("/local.properties") != null,
        "local.properties must exist for this test to run")
     Environment.ENV overrides Environment.fromResource("/local.properties")
}

private val awsClient by lazy {
    Filter.NoOp
        .then(ClientFilters.AwsAuth(Config.scope(awsConfig), Config.credentials(awsConfig)))
        .then(JavaHttpClient())
}

val lambdaClient by lazy {
    val region = Config.region(awsConfig)
    LambdaHttpClient(FunctionName("test-function"), region).then(awsClient)
}
