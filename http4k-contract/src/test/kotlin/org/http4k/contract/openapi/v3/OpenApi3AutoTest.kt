package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.contract.AutoContractRendererContract
import org.http4k.contract.openapi.AddSimpleFieldToRootNode
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.OpenAPIJackson
import org.http4k.core.Uri

class OpenApi3AutoTest : AutoContractRendererContract<JsonNode>(
    OpenAPIJackson,
    OpenApi3(
        ApiInfo("title", "1.2", "module description"),
        OpenAPIJackson,
        listOf(AddSimpleFieldToRootNode),
        listOf(ApiServer(Uri.of("http://localhost:8000"), "a very simple API"))
    )
)
