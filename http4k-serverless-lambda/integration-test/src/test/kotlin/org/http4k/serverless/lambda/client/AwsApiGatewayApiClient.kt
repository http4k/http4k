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

    fun create(name: ApiName) =
        apiDetailsLens(client(Request(Method.POST, "/v2/apis").with(createApiLens of Api(name.value))))

    fun listApis(): List<ApiDetails> = listApiLens(client(Request(Method.GET, "/v2/apis"))).items

    fun delete(apiId: ApiId) {
        client(Request(Method.DELETE, "/v2/apis/${apiId.value}"))
    }

    companion object {
        private val createApiLens = Body.auto<Api>().toLens()
        private val apiDetailsLens = Body.auto<ApiDetails>().toLens()
        private val listApiLens = Body.auto<ListApiResponse>().toLens()
    }

    private data class Api(val name: String, val protocolType: String = "HTTP")

    private data class ListApiResponse(val items: List<ApiDetails>)

    object ApiGatewayApi {
        operator fun invoke(region: Region): Filter = Filter { next ->
            { request -> next(request.uri(request.uri.host("apigateway.${region.name}.amazonaws.com").scheme("https"))) }
        }
    }
}

data class ApiName(val value:String)
data class ApiId(val value:String)

data class Stage(val stageName: String, val autoDeploy: Boolean = true) {
    companion object {
        val lens = Body.auto<Stage>().toLens()
    }
}

data class ApiDetails(val name: ApiName, val apiId: ApiId, val apiEndpoint: Uri)


data class Integration(
    val integrationType: String = "AWS_PROXY",
    val integrationUri: String,
    val timeoutInMillis: Long = 30000,
    val payloadFormatVersion: String = "1.0"
) {
    companion object {
        val lens = Body.auto<Integration>().toLens()
    }
}

data class IntegrationInfo(val integrationId: String) {
    companion object {
        val lens = Body.auto<IntegrationInfo>().toLens()
    }
}

data class Route(val target: String, val routeKey: String = "\$default") {
    companion object {
        val lens = Body.auto<Route>().toLens()
    }
}

object ApiGatewayJackson : ConfigurableJackson(KotlinModule()
    .asConfigurable()
    .withStandardMappings()
    .text(BiDiMapping(::ApiName, ApiName::value))
    .text(BiDiMapping(::ApiId, ApiId::value))
    .done()
    .deactivateDefaultTyping()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
    .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
)
