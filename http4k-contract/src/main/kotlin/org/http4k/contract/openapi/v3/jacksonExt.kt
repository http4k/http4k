package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.contract.openapi.SecurityRenderer
import org.http4k.format.ConfigurableJackson
import org.http4k.format.Jackson
import org.http4k.format.JsonErrorResponseRenderer
import org.http4k.util.JacksonJsonSchemaCreator

operator fun OpenApi3.Companion.invoke(
    apiInfo: ApiInfo,
    json: ConfigurableJackson = Jackson,
    securityRenderer: SecurityRenderer = org.http4k.contract.openapi.v3.SecurityRenderer,
    errorResponseRenderer: JsonErrorResponseRenderer<JsonNode> = JsonErrorResponseRenderer(json)
) = OpenApi3(apiInfo, json,
    ApiRenderer.Auto(json, JacksonJsonSchemaCreator(json, "components/schemas")),
    securityRenderer, errorResponseRenderer)
