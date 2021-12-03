package org.http4k.serverless.lambda.testing.client

import okhttp3.OkHttpClient
import org.http4k.aws.AwsProfile
import org.http4k.aws.awsClientFilterFor
import org.http4k.client.OkHttp
import org.http4k.core.then
import org.http4k.filter.Payload.Mode.Signed
import org.http4k.serverless.lambda.testing.setup.aws.apigateway.AwsApiGateway
import org.http4k.serverless.lambda.testing.setup.aws.apigateway.Http
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.AwsApiGatewayV2
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.Http
import org.http4k.serverless.lambda.testing.setup.aws.lambda.Http
import org.http4k.serverless.lambda.testing.setup.aws.lambda.Lambda
import org.http4k.serverless.lambda.testing.setup.aws.lambda.Region
import java.util.concurrent.TimeUnit.SECONDS

fun AwsProfile.apiGatewayApiClient() = AwsApiGatewayV2.Http(client("apigateway"), Region(region))

fun AwsProfile.restApiGatewayApiClient() = AwsApiGateway.Http(client("apigateway"), Region(region))

fun AwsProfile.awsLambdaApiClient() = Lambda.Http(client("lambda"), Region(region))

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
