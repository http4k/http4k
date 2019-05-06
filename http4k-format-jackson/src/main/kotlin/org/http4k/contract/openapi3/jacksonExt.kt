package org.http4k.contract.openapi3

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.contract.ApiInfo
import org.http4k.contract.SecurityRenderer
import org.http4k.format.ConfigurableJackson
import org.http4k.format.Jackson
import org.http4k.format.JsonErrorResponseRenderer
import org.http4k.util.JacksonJsonSchemaCreator

operator fun OpenApi3.Companion.invoke(
    apiInfo: ApiInfo,
    json: ConfigurableJackson = Jackson,
    securityRenderer: SecurityRenderer = OpenApi3SecurityRenderer,
    errorResponseRenderer: JsonErrorResponseRenderer<JsonNode> = JsonErrorResponseRenderer(json)
) = OpenApi3(apiInfo, json, JacksonJsonSchemaCreator(json), securityRenderer, errorResponseRenderer)
