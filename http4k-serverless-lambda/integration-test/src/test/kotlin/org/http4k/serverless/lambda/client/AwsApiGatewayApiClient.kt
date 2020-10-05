package org.http4k.serverless.lambda.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.http4k.lens.BiDiMapping
import org.http4k.serverless.lambda.client.ApiGatewayJackson.auto

class AwsApiGatewayApiClient(rawClient: HttpHandler, region: Region) {
    private val client = ApiGatewayApi(region).then(rawClient)

    fun createApi(name: ApiName) =
        apiDetailsLens(client(Request(Method.POST, "/v2/apis").with(createApiLens of Api(name.value))))

    fun listApis(): List<ApiDetails> = listApiLens(client(Request(Method.GET, "/v2/apis"))).items

    fun delete(apiId: ApiId) {
        client(Request(Method.DELETE, "/v2/apis/${apiId.value}"))
    }

    fun createStage(apiId: ApiId, stage: Stage) {
        client(Request(Method.POST, "/v2/apis/${apiId.value}/stages").with(createStageLens of stage))
    }

    fun createLambdaIntegration(apiId: ApiId, functionArn: String): IntegrationId =
        integrationInfo(client(Request(Method.POST, "/v2/apis/${apiId.value}/integrations")
            .with(createIntegrationLens of Integration(integrationUri = functionArn)))).integrationId

    fun createDefaultRoute(apiId: ApiId, integrationId: IntegrationId) =
        client(Request(Method.POST, "/v2/apis/${apiId.value}/routes")
            .with(createRouteLens of Route("integrations/${integrationId.value}")))

    companion object {
        private val createApiLens = Body.auto<Api>().toLens()
        private val apiDetailsLens = Body.auto<ApiDetails>().toLens()
        private val listApiLens = Body.auto<ListApiResponse>().toLens()
        private val createStageLens = Body.auto<Stage>().toLens()
        private val createIntegrationLens = Body.auto<Integration>().toLens()
        private val integrationInfo = Body.auto<IntegrationInfo>().toLens()
        private val createRouteLens = Body.auto<Route>().toLens()
    }

    private data class Api(val name: String, val protocolType: String = "HTTP")
    private data class IntegrationInfo(val integrationId: IntegrationId)
    private data class ListApiResponse(val items: List<ApiDetails>)
    private data class Route(val target: String, val routeKey: String = "\$default")

    object ApiGatewayApi {
        operator fun invoke(region: Region): Filter = Filter { next ->
            { request -> next(request.uri(request.uri.host("apigateway.${region.name}.amazonaws.com").scheme("https"))) }
        }
    }
}


data class ApiName(val value: String)
data class ApiId(val value: String)

data class StageName(val value: String)

data class Stage(val stageName: StageName, val autoDeploy: Boolean) {
    companion object {
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
