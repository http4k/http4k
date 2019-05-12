package org.http4k.contract.openapi.v3

import argo.jdom.JsonNode
import org.http4k.contract.openapi.ApiInfo
import org.http4k.format.Argo

class OpenApi3Test : OpenApi3Contract<JsonNode>(OpenApi3(ApiInfo("title", "1.2", "module description"), Argo))