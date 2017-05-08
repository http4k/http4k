package org.http4k.format

import argo.jdom.JsonNode
import argo.jdom.JsonRootNode

class ArgoTest : JsonContract<JsonRootNode, JsonNode>(Argo)
class ArgoJsonErrorResponseRendererContractTest : JsonErrorResponseRendererContract<JsonRootNode, JsonNode>(Argo)