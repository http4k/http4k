package org.http4k.connect.amazon.apigatewayv2.action

import dev.forkhandles.result4k.map
import org.http4k.connect.amazon.apigatewayv2.AwsApiGatewayV2
import org.http4k.connect.amazon.apigatewayv2.model.ApiDetails
import org.http4k.connect.amazon.apigatewayv2.model.ApiId
import org.http4k.connect.amazon.apigatewayv2.model.ApiIntegrationVersion
import org.http4k.connect.amazon.apigatewayv2.model.ApiName
import org.http4k.connect.amazon.apigatewayv2.model.IntegrationId
import org.http4k.connect.amazon.apigatewayv2.model.IntegrationInfo
import org.http4k.connect.amazon.apigatewayv2.model.Stage
import org.http4k.connect.amazon.getOrThrow

fun AwsApiGatewayV2.createApi(name: ApiName): ApiDetails = this(CreateApi(name)).getOrThrow()
fun AwsApiGatewayV2.listApis(): List<ApiDetails> = this(ListApis()).map(ListApiResponse::items).getOrThrow()
fun AwsApiGatewayV2.delete(apiId: ApiId) = this(DeleteApi(apiId)).getOrThrow()
fun AwsApiGatewayV2.createStage(apiId: ApiId, stage: Stage) = this(CreateStage(apiId, stage)).getOrThrow()
fun AwsApiGatewayV2.createLambdaIntegration(
    apiId: ApiId,
    functionArn: String,
    version: ApiIntegrationVersion
): IntegrationId = this(
    CreateLambdaIntegration(apiId, functionArn, version)
).map(IntegrationInfo::integrationId).getOrThrow()

fun AwsApiGatewayV2.createDefaultRoute(apiId: ApiId, integrationId: IntegrationId) =
    this(CreateDefaultRoute(apiId, integrationId)).getOrThrow()
