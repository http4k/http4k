package org.http4k.serverless.lambda.testing.client

import okhttp3.OkHttpClient
import org.http4k.aws.AwsProfile
import org.http4k.aws.awsClientFilterFor
import org.http4k.client.OkHttp
import org.http4k.connect.amazon.apigateway.AwsApiGateway
import org.http4k.connect.amazon.apigateway.Http
import org.http4k.connect.amazon.apigatewayv2.AwsApiGatewayV2
import org.http4k.connect.amazon.apigatewayv2.Http
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.lambda.Http
import org.http4k.connect.amazon.lambda.Lambda
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.Payload.Mode.Signed
import org.http4k.filter.inIntelliJOnly
import java.util.concurrent.TimeUnit.SECONDS

fun AwsProfile.apiGatewayApiClient() = AwsApiGatewayV2.Http(client("apigateway"), Region.of(region))

fun AwsProfile.restApiGatewayApiClient() = AwsApiGateway.Http(client("apigateway"), Region.of(region))

fun AwsProfile.awsLambdaApiClient() = Lambda.Http(client("lambda"), Region.of(region))

private fun AwsProfile.client(service: String) = awsClientFilterFor(service, Signed)
    .then(DebuggingFilters.PrintRequestAndResponse().inIntelliJOnly())
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
