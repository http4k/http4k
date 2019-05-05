package org.http4k.contract.openapi2

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.contract.ApiInfo
import org.http4k.contract.OpenApi
import org.http4k.contract.SecurityRenderer
import org.http4k.contract.openapi3.OpenApi3
import org.http4k.format.Jackson
import org.http4k.format.JsonErrorResponseRenderer
import org.http4k.util.JacksonJsonSchemaCreator

operator fun OpenApi2.Companion.invoke(
    apiInfo: ApiInfo,
    securityRenderer: SecurityRenderer<JsonNode> = SecurityRenderer.OpenApi(Jackson),
    errorResponseRenderer: JsonErrorResponseRenderer<JsonNode> = JsonErrorResponseRenderer(Jackson)
) = OpenApi3(apiInfo, Jackson, JacksonJsonSchemaCreator(Jackson), securityRenderer, errorResponseRenderer)
