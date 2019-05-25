package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.contract.ContractRendererContract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.format.Jackson
import org.http4k.util.AutoJsonToJsonSchema

class OpenApi3AutoTest : ContractRendererContract<JsonNode>(Jackson,
    OpenApi3(ApiInfo("title", "1.2", "module description"), Jackson,
        ApiRenderer.Auto(Jackson, AutoJsonToJsonSchema(Jackson))))