package org.http4k.http.formats

import argo.jdom.JsonNode
import argo.jdom.JsonRootNode

class ArgoTest : JsonContract<JsonRootNode, JsonNode>(Argo)
class ArgoJsonErrorResponseRendererContractTest : JsonErrorResponseRendererContract<JsonRootNode, JsonNode>(Argo)