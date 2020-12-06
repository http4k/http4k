package org.http4k.serverless.lambda.client

import org.http4k.aws.AwsApiGatewayApiClient
import org.http4k.aws.AwsLambdaApiClient
import org.http4k.aws.AwsProfile
import org.http4k.aws.Function
import org.http4k.aws.LambdaIntegrationType
import org.http4k.aws.Region
import org.http4k.client.JavaHttpClient
import org.http4k.client.LambdaHttpClient
import org.http4k.core.then
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.filter.inIntelliJOnly
import org.http4k.serverless.lambda.DeployServerAsLambdaForClientContract.functionName

fun AwsProfile.testFunctionClient(type: LambdaIntegrationType, clientFn: (Function, Region) -> LambdaHttpClient) =
    clientFn(functionName(type), Region(region))
        .then(awsClientFor("lambda"))

fun AwsProfile.apiGatewayClient() = AwsApiGatewayApiClient(awsClientFor("apigateway"), Region(region))

fun AwsProfile.awsLambdaApiClient() = AwsLambdaApiClient(awsClientFor("lambda"), Region(region))

fun AwsProfile.awsClientFor(service: String) =
    ClientFilters.AwsAuth(scopeFor(service), credentials)
        .then(PrintRequestAndResponse().inIntelliJOnly())
        .then(JavaHttpClient())

