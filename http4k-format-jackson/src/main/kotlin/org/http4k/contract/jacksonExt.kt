package org.http4k.contract

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.format.Jackson
import org.http4k.format.JsonErrorResponseRenderer
import org.http4k.util.JacksonJsonSchemaCreator

operator fun AutoOpenApi.Companion.invoke(
    apiInfo: ApiInfo,
    securityRenderer: SecurityRenderer<JsonNode> = SecurityRenderer.OpenApi(Jackson),
    errorResponseRenderer: JsonErrorResponseRenderer<JsonNode> = JsonErrorResponseRenderer(Jackson)
) = AutoOpenApi(apiInfo, Jackson, JacksonJsonSchemaCreator(Jackson), securityRenderer, errorResponseRenderer)
