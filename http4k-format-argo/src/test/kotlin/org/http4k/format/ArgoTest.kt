package org.http4k.format

import argo.jdom.JsonNode
import org.http4k.jsonrpc.ManualMappingJsonRpcServiceContract

class ArgoTest : JsonContract<JsonNode, JsonNode>(Argo) {
    override val prettyString = """{
	"hello": "world"
}"""
}

class ArgoJsonErrorResponseRendererContractTest : JsonErrorResponseRendererContract<JsonNode, JsonNode>(Argo)
class ArgoGenerateDataClassesTest : GenerateDataClassesContract<JsonNode, JsonNode>(Argo)
class ArgoManualMappingJsonRpcServiceTest : ManualMappingJsonRpcServiceContract<JsonNode, JsonNode>(Argo)
