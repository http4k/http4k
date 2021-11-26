package org.http4k.contract.openapi.v3

import argo.jdom.JsonNode
import org.http4k.contract.ContractRendererContract
import org.http4k.contract.openapi.AddSimpleFieldToRootNode
import org.http4k.contract.openapi.ApiInfo
import org.http4k.core.Uri
import org.http4k.format.Argo

class OpenApi3Test : ContractRendererContract<JsonNode>(Argo, OpenApi3(
    ApiInfo("title", "1.2", "module description"),
    Argo,
    listOf(AddSimpleFieldToRootNode),
    servers = listOf(ApiServer(Uri.of("https://localhost:8000")))
))
