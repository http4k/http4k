package org.http4k.contract.openapi.v2

import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.OpenApiExtension
import org.http4k.core.Uri
import org.http4k.format.Jackson

/**
 * Sensible default objects for using Jackson with minimal fuss.
 */
fun OpenApi2(apiInfo: ApiInfo, baseUri: Uri, json: Jackson = Jackson, extensions: List<OpenApiExtension> = emptyList()) = OpenApi2(apiInfo, json, baseUri, extensions)
