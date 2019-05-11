package org.http4k.contract.openapi.v2

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.contract.JsonErrorResponseRenderer
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer.Companion.Auto
import org.http4k.contract.openapi.SecurityRenderer
import org.http4k.format.ConfigurableJackson
import org.http4k.format.Jackson
import org.http4k.util.JacksonJsonSchemaCreator

operator fun OpenApi2.Companion.invoke(
    apiInfo: ApiInfo,
    json: ConfigurableJackson = Jackson,
    securityRenderer: SecurityRenderer = SupportedSecurityRenderer,
    errorResponseRenderer: JsonErrorResponseRenderer<JsonNode> = JsonErrorResponseRenderer(json)
) = OpenApi2(apiInfo, Jackson,
    Auto(json, JacksonJsonSchemaCreator(json, "definitions")),
    securityRenderer, errorResponseRenderer)
