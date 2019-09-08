package org.http4k.contract.openapi.v2

import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.OpenApiExtension
import org.http4k.core.Uri
import org.http4k.format.Jackson

/**
 * Defaults for configuring OpenApi2 with Jackson
 */
fun OpenApi2(apiInfo: ApiInfo, baseUri: Uri, extensions: List<OpenApiExtension> = emptyList(), json: Jackson = Jackson) = OpenApi2(apiInfo, json, baseUri, extensions)
