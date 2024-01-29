package org.http4k.connect.amazon.apigateway.action

import dev.forkhandles.result4k.map
import org.http4k.connect.amazon.apigateway.AwsApiGateway
import org.http4k.connect.amazon.apigateway.model.ApiDetails
import org.http4k.connect.amazon.apigateway.model.ApiId
import org.http4k.connect.amazon.apigateway.model.ApiName
import org.http4k.connect.amazon.apigateway.model.DeploymentId
import org.http4k.connect.amazon.apigateway.model.Stage
import org.http4k.connect.amazon.getOrThrow
import org.http4k.connect.amazon.lambda.model.Region
import org.http4k.core.Uri

fun AwsApiGateway.createApi(name: ApiName, region: Region) =
    this(CreateApi(name)).map { toApiDetails(it, region) }.getOrThrow()

fun AwsApiGateway.listApis(region: Region): List<ApiDetails> =
    this(ListApis())
        .map { (it._embedded ?: EmbeddedDetails(emptyList())).item.map { item -> toApiDetails(item, region) } }.getOrThrow()

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

private fun toApiDetails(it: RestApiDetails, region: Region) =
    ApiDetails(
        ApiName(it.name),
        ApiId(it.id),
        apiEndpoint = Uri.of("https://${it.id}.execute-api.${region.name}.amazonaws.com/default")
    )
