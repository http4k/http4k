package org.http4k.serverless.lambda.testing.setup.aws.apigateway

import dev.forkhandles.result4k.map
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.SetAwsServiceUrl
import org.http4k.serverless.lambda.testing.setup.Region
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiDetails
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiId
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiName
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.Stage
import org.http4k.serverless.lambda.testing.setup.aws.getOrThrow

class AwsRestApiGatewayApiClient(rawClient: HttpHandler, private val region: Region) {
    private val client = ClientFilters.SetAwsServiceUrl("apigateway", region.name).then(rawClient)

    private val gateway = AwsApiGateway.Http(client, region)

    fun createApi(name: ApiName) = gateway(CreateApi(name)).map(this@AwsRestApiGatewayApiClient::toApiDetails).getOrThrow()
    fun listApis(): List<ApiDetails> = gateway(ListApis()).map { it._embedded.item.map(this@AwsRestApiGatewayApiClient::toApiDetails) }.getOrThrow()
    fun delete(apiId: ApiId) = gateway(DeleteApi(apiId))
    fun listResources(apiId: ApiId) = gateway(ListRootResource(apiId)).getOrThrow()
    fun createResource(apiId: ApiId, parentResource: RestResourceDetails) = gateway(CreateResource(apiId, parentResource)).getOrThrow()
    fun createMethod(apiId: ApiId, resource: RestResourceDetails) = gateway(CreateMethod(apiId, resource)).getOrThrow()
    fun createIntegration(apiId: ApiId, resource: RestResourceDetails, functionArn: String, region: Region) = gateway(CreateIntegration(apiId, resource, functionArn, region)).getOrThrow()
    fun createIntegrationResponse(apiId: ApiId, resource: RestResourceDetails) = gateway(CreateIntegrationResponse(apiId, resource)).getOrThrow()
    fun createStage(apiId: ApiId, stage: Stage, deploymentId: DeploymentId) = gateway(CreateStage(apiId, stage, deploymentId)).getOrThrow()

    private fun toApiDetails(it: RestApiDetails) =
        ApiDetails(
            ApiName(it.name),
            ApiId(it.id),
            apiEndpoint = Uri.of("https://${it.id}.execute-api.${region.name}.amazonaws.com/default")
        )

    fun createStage(apiId: ApiId, stage: Stage, functionArn: String) {
        val rootResource = listResources(apiId)._embedded.item
        val proxyResource = createResource(apiId, rootResource)

        createMethod(apiId, proxyResource)
        createIntegration(apiId, proxyResource, functionArn, region)
        createIntegrationResponse(apiId, proxyResource)

        val deploymentId = gateway(CreateDeployment(apiId, DeploymentName(stage.stageName.value))).getOrThrow()

        createStage(apiId, stage, deploymentId)
    }
}

data class RestApiDetails(val name: String, val id: String)
data class ListApiResponse(val _embedded: EmbeddedDetails)
data class EmbeddedDetails(val item: List<RestApiDetails>)
data class RestResourceDetails(val id: String, val path: String)
data class ListResourcesResponse(val _embedded: EmbeddedResourceDetails)
data class EmbeddedResourceDetails(val item: RestResourceDetails)
data class DeploymentName(val value: String)
data class DeploymentId(val id: String)
