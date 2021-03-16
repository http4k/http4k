package org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.map
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.SetAwsServiceUrl
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.http4k.lens.BiDiMapping
import org.http4k.serverless.lambda.testing.setup.aws.RemoteFailure
import org.http4k.serverless.lambda.testing.setup.aws.getOrThrow
import org.http4k.serverless.lambda.testing.setup.aws.lambda.Region


interface AwsApiGatewayV2 {
    operator fun <R : Any> invoke(action: AwsApiGatewayV2Action<R>): Result<R, RemoteFailure>

    companion object
}

fun AwsApiGatewayV2.Companion.Http(rawHttp: HttpHandler, region: Region) = object : AwsApiGatewayV2 {
    private val http = ClientFilters.SetAwsServiceUrl("apigateway", region.name).then(rawHttp)

    override fun <R : Any> invoke(action: AwsApiGatewayV2Action<R>) = action.toResult(
        http(action.toRequest())
    )
}

fun AwsApiGatewayV2.createApi(name: ApiName): ApiDetails = this(CreateApi(name)).getOrThrow()
fun AwsApiGatewayV2.listApis(): List<ApiDetails> = this(ListApis()).map(ListApiResponse::items).getOrThrow()
fun AwsApiGatewayV2.delete(apiId: ApiId) = this(DeleteApi(apiId)).getOrThrow()
fun AwsApiGatewayV2.createStage(apiId: ApiId, stage: Stage) = this(CreateStage(apiId, stage)).getOrThrow()
fun AwsApiGatewayV2.createLambdaIntegration(apiId: ApiId, functionArn: String, version: ApiIntegrationVersion): IntegrationId = this(CreateLambdaIntegration(apiId, functionArn, version)).map(IntegrationInfo::integrationId).getOrThrow()
fun AwsApiGatewayV2.createDefaultRoute(apiId: ApiId, integrationId: IntegrationId) = this(CreateDefaultRoute(apiId, integrationId)).getOrThrow()

data class ApiName(val value: String)

data class ApiId(val value: String)

data class StageName(val value: String)

data class Stage(val stageName: StageName, val autoDeploy: Boolean) {
    companion object {
        val restDefault = Stage(StageName("default"), true)
        val default = Stage(StageName("\$default"), true)
    }
}

data class ApiDetails(val name: ApiName, val apiId: ApiId, val apiEndpoint: Uri)

data class Integration(
    val integrationType: String = "AWS_PROXY",
    val integrationUri: String,
    val timeoutInMillis: Long = 30000,
    val payloadFormatVersion: String = "1.0"
)

data class IntegrationId(val value: String)

data class IntegrationInfo(val integrationId: IntegrationId)

data class ListApiResponse(val items: List<ApiDetails>)

enum class ApiIntegrationVersion { v1, v2 }

object ApiGatewayJackson : ConfigurableJackson(KotlinModule()
    .asConfigurable()
    .withStandardMappings()
    .text(BiDiMapping(::ApiName, ApiName::value))
    .text(BiDiMapping(::ApiId, ApiId::value))
    .text(BiDiMapping(::StageName, StageName::value))
    .text(BiDiMapping(::IntegrationId, IntegrationId::value))
    .done()
    .deactivateDefaultTyping()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
    .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
)
