package org.http4k.aws

import com.fasterxml.jackson.annotation.JsonProperty
import org.http4k.aws.ApiGatewayJackson.auto
import org.http4k.aws.ApiIntegrationVersion.v1
import org.http4k.aws.ApiIntegrationVersion.v2
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.filter.SetAwsServiceUrl

class AwsRestApiGatewayApiClient(rawClient: HttpHandler, private val region: Region) {
    private val client = ClientFilters.SetAwsServiceUrl("apigateway", region.name).then(rawClient)

    fun createApi(name: ApiName) =
        apiDetailsLens(client(Request(Method.POST, "/restapis").with(createApiLens of RestApi(name.value)))).let(this@AwsRestApiGatewayApiClient::toApiDetails)

    fun listApis(): List<ApiDetails> = listApiLens(client(Request(Method.GET, "/restapis"))).embedded.item.map(this@AwsRestApiGatewayApiClient::toApiDetails)

    private fun toApiDetails(it: RestApiDetails) =
        ApiDetails(ApiName(it.name), ApiId(it.id), apiEndpoint = Uri.of("https://${it.id}.execute-api.${region.name}.amazonaws.com/default"))

    fun delete(apiId: ApiId) {
        client(Request(Method.DELETE, "/v2/apis/${apiId.value}"))
    }

    fun createStage(apiId: ApiId, stage: Stage) {
        client(Request(Method.POST, "/v2/apis/${apiId.value}/stages").with(createStageLens of stage))
    }

    fun createLambdaIntegration(apiId: ApiId, functionArn: String, version: ApiIntegrationVersion): IntegrationId =
        integrationInfo(client(Request(Method.POST, "/v2/apis/${apiId.value}/integrations")
            .with(createIntegrationLens of Integration(
                integrationUri = functionArn,
                payloadFormatVersion = when (version) {
                    v1 -> "1.0"
                    v2 -> "2.0"
                }
            )))).integrationId

    fun createDefaultRoute(apiId: ApiId, integrationId: IntegrationId) =
        client(Request(Method.POST, "/v2/apis/${apiId.value}/routes")
            .with(createRouteLens of Route("integrations/${integrationId.value}")))

    companion object {
        private val createApiLens = Body.auto<RestApi>().toLens()
        private val apiDetailsLens = Body.auto<RestApiDetails>().toLens()
        private val listApiLens = Body.auto<ListApiResponse>().toLens()
        private val createStageLens = Body.auto<Stage>().toLens()
        private val createIntegrationLens = Body.auto<Integration>().toLens()
        private val integrationInfo = Body.auto<IntegrationInfo>().toLens()
        private val createRouteLens = Body.auto<Route>().toLens()
    }

    private data class RestApi(val name: String)
    private data class IntegrationInfo(val integrationId: IntegrationId)
    private data class ListApiResponse(@JsonProperty("_embedded") val embedded: EmbeddedDetails)
    private data class EmbeddedDetails(val item: List<RestApiDetails>)
    private data class RestApiDetails(val name: String, val id: String)
    private data class Route(val target: String, val routeKey: String = "\$default")
}
