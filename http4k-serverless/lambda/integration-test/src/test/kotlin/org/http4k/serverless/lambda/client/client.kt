package org.http4k.serverless.lambda.client

import org.http4k.aws.AwsApiGatewayApiClient
import org.http4k.aws.AwsLambdaApiClient
import org.http4k.aws.AwsProfile
import org.http4k.aws.Region
import org.http4k.aws.awsClientFor

fun AwsProfile.apiGatewayApiClient() = AwsApiGatewayApiClient(awsClientFor("apigateway"), Region(region))

fun AwsProfile.awsLambdaApiClient() = AwsLambdaApiClient(awsClientFor("lambda"), Region(region))
