package org.http4k.connect.azure

import dev.forkhandles.values.StringValue
import org.http4k.client.JavaHttpClient
import org.http4k.connect.azure.ApiVersion.Companion.PREVIEW
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters.BearerAuth
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.lens.Header
import org.http4k.lens.enum
import org.http4k.lens.value

fun AzureAI.Companion.Http(
    token: AzureAIApiKey,
    host: AzureHost,
    region: Region,
    http: HttpHandler = JavaHttpClient(),
    apiVersion: ApiVersion = PREVIEW,
    extraParameters: ExtraParameters? = null,
    deployment: Deployment? = null
) = AzureAI(
    Uri.of("https://$host.$region.inference.ai.azure.com")
        .query("api-version", apiVersion.value),
    token, http,
    extraParameters, deployment
)

/**
 * Use this API with GitHubModels
 */
fun AzureAI.Companion.Http(token: GitHubToken, http: HttpHandler = JavaHttpClient()) =
    AzureAI(Uri.of("https://models.inference.ai.azure.com"), token, http)

/**
 * Use this API with Azure resources
 */
fun AzureAI.Companion.Http(resource: AzureResource, region: Region, token: AzureAIApiKey, http: HttpHandler = JavaHttpClient()) =
    AzureAI(Uri.of("https://$resource.$region.models.ai.azure.com"), token, http)

private fun AzureAI(
    baseUri: Uri,
    token: StringValue,
    http: HttpHandler,
    extraParameters: ExtraParameters? = null,
    deployment: Deployment? = null
) = object : AzureAI {
    private val routedHttp = SetBaseUriFrom(baseUri)
        .then(BearerAuth(token.value))
        .then(http)

    private val extra = Header.enum<ExtraParameters>().optional("extra-parameters")
    private val modelDeployment = Header.value(Deployment).optional("azureml-model-deployment")

    override fun <R> invoke(action: AzureAIAction<R>) = action.toResult(
        routedHttp(
            action.toRequest().with(modelDeployment of deployment, extra of extraParameters)
        )
    )
}
