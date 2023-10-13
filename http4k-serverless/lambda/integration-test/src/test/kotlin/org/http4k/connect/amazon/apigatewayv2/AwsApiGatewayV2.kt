package org.http4k.connect.amazon.apigatewayv2

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.map
import org.http4k.connect.amazon.RemoteFailure
import org.http4k.connect.amazon.apigatewayv2.action.CreateApi
import org.http4k.connect.amazon.apigatewayv2.action.CreateDefaultRoute
import org.http4k.connect.amazon.apigatewayv2.action.CreateLambdaIntegration
import org.http4k.connect.amazon.apigatewayv2.action.CreateStage
import org.http4k.connect.amazon.apigatewayv2.action.DeleteApi
import org.http4k.connect.amazon.apigatewayv2.action.ListApis
import org.http4k.connect.amazon.apigatewayv2.model.ApiDetails
import org.http4k.connect.amazon.apigatewayv2.model.ApiId
import org.http4k.connect.amazon.apigatewayv2.model.ApiIntegrationVersion
import org.http4k.connect.amazon.apigatewayv2.model.ApiName
import org.http4k.connect.amazon.apigatewayv2.model.IntegrationId
import org.http4k.connect.amazon.apigatewayv2.model.IntegrationInfo
import org.http4k.connect.amazon.apigatewayv2.model.Stage
import org.http4k.connect.amazon.apigatewayv2.model.StageName
import org.http4k.connect.amazon.getOrThrow
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.http4k.lens.BiDiMapping

interface AwsApiGatewayV2 {
    operator fun <R : Any> invoke(action: AwsApiGatewayV2Action<R>): Result<R, RemoteFailure>

    companion object
}

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

data class ListApiResponse(val items: List<ApiDetails>)

object ApiGatewayJackson : ConfigurableJackson(
    KotlinModule.Builder().build()
        .asConfigurable()
        .withStandardMappings()
        .text(BiDiMapping(::ApiName, ApiName::value))
        .text(BiDiMapping(::ApiId, ApiId::value))
        .text(BiDiMapping(::StageName, StageName::value))
        .text(BiDiMapping(::IntegrationId, IntegrationId::value))
        .done()
        .deactivateDefaultTyping()
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(FAIL_ON_IGNORED_PROPERTIES, false)
        .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
        .configure(USE_BIG_INTEGER_FOR_INTS, true)
)
