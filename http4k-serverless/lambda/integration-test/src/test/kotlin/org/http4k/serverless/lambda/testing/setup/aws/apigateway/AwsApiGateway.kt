package org.http4k.serverless.lambda.testing.setup.aws.apigateway

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.map
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.SetAwsServiceUrl
import org.http4k.serverless.lambda.testing.setup.Region
import org.http4k.serverless.lambda.testing.setup.aws.RemoteFailure
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiDetails
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiId
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiName
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.Stage
import org.http4k.serverless.lambda.testing.setup.aws.getOrThrow

interface AwsApiGateway {
    operator fun <R : Any> invoke(action: AwsApiGatewayAction<R>): Result<R, RemoteFailure>

    companion object
}

fun AwsApiGateway.Companion.Http(rawHttp: HttpHandler, region: Region) = object : AwsApiGateway {
    private val http = ClientFilters.SetAwsServiceUrl("apigateway", region.name).then(rawHttp)

    override fun <R : Any> invoke(action: AwsApiGatewayAction<R>) = action.toResult(
        http(action.toRequest())
    )
}

fun AwsApiGateway.createApi(name: ApiName, region: Region) =
    this(CreateApi(name)).map { toApiDetails(it, region) }.getOrThrow()
fun AwsApiGateway.listApis(region: Region): List<ApiDetails> =
    this(ListApis()).map { it._embedded.item.map { item -> toApiDetails(item, region) } }.getOrThrow()
fun AwsApiGateway.delete(apiId: ApiId) = this(DeleteApi(apiId))
fun AwsApiGateway.listResources(apiId: ApiId) = this(ListRootResource(apiId)).getOrThrow()
fun AwsApiGateway.createResource(apiId: ApiId, parentResource: RestResourceDetails) =
    this(CreateResource(apiId, parentResource)).getOrThrow()
fun AwsApiGateway.createMethod(apiId: ApiId, resource: RestResourceDetails) =
    this(CreateMethod(apiId, resource)).getOrThrow()
fun AwsApiGateway.createIntegration(apiId: ApiId, resource: RestResourceDetails, functionArn: String, region: Region) =
    this(CreateIntegration(apiId, resource, functionArn, region)).getOrThrow()
fun AwsApiGateway.createIntegrationResponse(apiId: ApiId, resource: RestResourceDetails) =
    this(CreateIntegrationResponse(apiId, resource)).getOrThrow()
fun AwsApiGateway.createStage(apiId: ApiId, stage: Stage, deploymentId: DeploymentId) =
    this(CreateStage(apiId, stage, deploymentId)).getOrThrow()

data class RestApiDetails(val name: String, val id: String)
data class ListApiResponse(val _embedded: EmbeddedDetails)
data class EmbeddedDetails(val item: List<RestApiDetails>)
data class RestResourceDetails(val id: String, val path: String)
data class ListResourcesResponse(val _embedded: EmbeddedResourceDetails)
data class EmbeddedResourceDetails(val item: RestResourceDetails)
data class DeploymentName(val value: String)
data class DeploymentId(val id: String)

private fun toApiDetails(it: RestApiDetails, region: Region) =
    ApiDetails(
        ApiName(it.name),
        ApiId(it.id),
        apiEndpoint = Uri.of("https://${it.id}.execute-api.${region.name}.amazonaws.com/default")
    )
