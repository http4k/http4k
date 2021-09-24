package org.http4k.contract.openapi.v2

import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.OpenAPIJackson
import org.http4k.contract.openapi.OpenApiExtension
import org.http4k.core.Uri
import org.http4k.format.ConfigurableJackson

/**
 * Defaults for configuring OpenApi2 with Jackson
 */
fun OpenApi2(
    apiInfo: ApiInfo,
    baseUri: Uri,
    json: ConfigurableJackson = OpenAPIJackson,
    extensions: List<OpenApiExtension> = emptyList()
) = OpenApi2(apiInfo, json, baseUri, extensions)
