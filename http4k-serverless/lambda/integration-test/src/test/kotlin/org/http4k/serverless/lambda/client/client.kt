package org.http4k.serverless.lambda.client

import okhttp3.OkHttpClient
import org.http4k.aws.AwsApiGatewayApiClient
import org.http4k.aws.AwsLambdaApiClient
import org.http4k.aws.AwsProfile
import org.http4k.aws.AwsRestApiGatewayApiClient
import org.http4k.aws.Region
import org.http4k.aws.awsClientFilterFor
import org.http4k.client.OkHttp
import org.http4k.core.then
import org.http4k.filter.Payload.Mode.Signed
import java.util.concurrent.TimeUnit.SECONDS

fun AwsProfile.apiGatewayApiClient(): AwsApiGatewayApiClient =
    AwsApiGatewayApiClient(client("apigateway"), Region(region))

fun AwsProfile.restApiGatewayApiClient() = AwsRestApiGatewayApiClient(client("apigateway"), Region(region))

fun AwsProfile.awsLambdaApiClient() = AwsLambdaApiClient(client("lambda"), Region(region))

private fun AwsProfile.client(service: String) = awsClientFilterFor(service, Signed)
    .then(
        OkHttp(
            OkHttpClient.Builder()
                .callTimeout(120, SECONDS)
                .readTimeout(120, SECONDS)
                .connectTimeout(120, SECONDS)
                .writeTimeout(120, SECONDS)
                .followRedirects(false)
                .build()
        )
    )
