package org.http4k.format

import argo.jdom.JsonNode
import org.http4k.jsonrpc.ManualMappingJsonRpcServiceContract

class ArgoTest : JsonContract<JsonNode>(Argo) {
    override val prettyString = """{
	"hello": "world"
}"""
}

class ArgoJsonErrorResponseRendererContractTest : JsonErrorResponseRendererContract<JsonNode>(Argo)
class ArgoGenerateDataClassesTest : GenerateDataClassesContract<JsonNode>(Argo)
class ArgoManualMappingJsonRpcServiceTest : ManualMappingJsonRpcServiceContract<JsonNode>(Argo)
