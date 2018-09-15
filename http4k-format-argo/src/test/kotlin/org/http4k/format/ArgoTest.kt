package org.http4k.format

import argo.jdom.JsonNode
import argo.jdom.JsonRootNode

class ArgoTest : JsonContract<JsonRootNode, JsonNode>(Argo) {
    override val prettyString = """{
	"hello": "world"
}"""
}
class ArgoJsonErrorResponseRendererContractTest : JsonErrorResponseRendererContract<JsonRootNode, JsonNode>(Argo)
class ArgoGenerateDataClassesTest : GenerateDataClassesContract<JsonRootNode, JsonNode>(Argo)
//class ArgoManualMappingJsonRpcServiceTest : ManualMappingJsonRpcServiceContract<JsonRootNode>(Argo)
