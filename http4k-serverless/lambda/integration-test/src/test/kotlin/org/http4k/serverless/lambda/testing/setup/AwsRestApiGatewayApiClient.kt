package org.http4k.serverless.lambda.testing.setup

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.filter.SetAwsServiceUrl
import org.http4k.serverless.lambda.testing.setup.ApiGatewayJackson.auto

class AwsRestApiGatewayApiClient(rawClient: HttpHandler, private val region: Region) {
    private val client = ClientFilters.SetAwsServiceUrl("apigateway", region.name).then(rawClient)

    fun createApi(name: ApiName) =
        apiDetailsLens(
            client(
                Request(
                    Method.POST,
                    "/restapis"
                ).with(createApiLens of RestApi(name.value))
            )
        ).let(this@AwsRestApiGatewayApiClient::toApiDetails)

    fun listApis(): List<ApiDetails> = listApiLens(
        client(
            Request(
                Method.GET,
                "/restapis"
            )
        )
    )._embedded.item.map(this@AwsRestApiGatewayApiClient::toApiDetails)

    private fun toApiDetails(it: RestApiDetails) =
        ApiDetails(
            ApiName(it.name),
            ApiId(it.id),
            apiEndpoint = Uri.of("https://${it.id}.execute-api.${region.name}.amazonaws.com/default")
        )

    fun delete(apiId: ApiId) {
        client(Request(Method.DELETE, "/restapis/${apiId.value}"))
    }

    fun createStage(apiId: ApiId, stage: Stage, functionArn: String) {
        val rootResource = listResourcesLens(
            client(
                Request(
                    Method.GET,
                    "/restapis/${apiId.value}/resources"
                )
            )
        )._embedded.item

        val proxyResource = resourceDetailsLens(
            client(
                Request(
                    Method.POST,
                    "/restapis/${apiId.value}/resources/${rootResource.id}"
                ).with(createProxyResource of CreateProxyResource())
            )
        )

        client(
            Request(Method.PUT, "/restapis/${apiId.value}/resources/${proxyResource.id}/methods/ANY")
                .with(createMethod of CreateMethod(authorizationType = "NONE"))
        )

        client(
            Request(Method.PUT, "/restapis/${apiId.value}/resources/${proxyResource.id}/methods/ANY/integration")
                .with(createMethodWithIntegrationLens of CreateMethodWithIntegration(uri = functionArn.invocation(region)))
        )

        client(
            Request(Method.PUT, "/restapis/${apiId.value}/resources/${proxyResource.id}/methods/ANY/integration/responses/200")
                .with(createIntegrationResponseLens of CreateIntegrationResponse())
        )

        val deploymentId =
            createDeploymentResponseLens(
                client(
                    Request(
                        Method.POST,
                        "/restapis/${apiId.value}/deployments"
                    ).with(createDeploymentLens of RestDeployment(stage.stageName.value))
                )
            )
        client(
            Request(
                Method.POST,
                "/restapis/${apiId.value}/stages"
            ).with(createStageLens of RestStage(stage.stageName.value, deploymentId.id))
        )
    }

    companion object {
        private val createApiLens = Body.auto<RestApi>().toLens()
        private val apiDetailsLens = Body.auto<RestApiDetails>().toLens()
        private val listApiLens = Body.auto<ListApiResponse>().toLens()
        private val listResourcesLens = Body.auto<ListResourcesResponse>().toLens()
        private val resourceDetailsLens = Body.auto<RestResourceDetails>().toLens()
        private val createProxyResource = Body.auto<CreateProxyResource>().toLens()
        private val createStageLens = Body.auto<RestStage>().toLens()
        private val createDeploymentLens = Body.auto<RestDeployment>().toLens()
        private val createDeploymentResponseLens = Body.auto<RestDeploymentResponse>().toLens()
        private val createMethodWithIntegrationLens = Body.auto<CreateMethodWithIntegration>().toLens()
        private val createIntegrationResponseLens = Body.auto<CreateIntegrationResponse>().toLens()
        private val createMethod = Body.auto<CreateMethod>().toLens()
    }

    private data class CreateProxyResource(val pathPart: String = "{proxy+}")
    private data class RestApi(val name: String)
    private data class ListApiResponse(val _embedded: EmbeddedDetails)
    private data class ListResourcesResponse(val _embedded: EmbeddedResourceDetails)
    private data class EmbeddedDetails(val item: List<RestApiDetails>)
    private data class EmbeddedResourceDetails(val item: RestResourceDetails)
    private data class RestResourceDetails(val id: String, val path: String)
    private data class RestApiDetails(val name: String, val id: String)
    private data class RestStage(val stageName: String, val deploymentId: String)
    private data class RestDeployment(val name: String)
    private data class RestDeploymentResponse(val id: String)
    private data class CreateMethodWithIntegration(
        val type: String = "AWS_PROXY",
        val uri: String,
        val httpMethod: String = "POST"
    )
    private data class CreateMethod(val authorizationType: String)
    private data class CreateIntegrationResponse(val selectionPattern:String = "\\d{3}", val contentHandling:String = "CONVERT_TO_TEXT")

    private fun String.invocation(region: Region): String =
        "arn:aws:apigateway:${region.name}:lambda:path/2015-03-31/functions/${this}/invocations"
}
