package org.http4k.contract

import argo.jdom.JsonNode
import org.http4k.format.Argo

class ArgoJsonErrorResponseRendererTest : JsonErrorResponseRendererContract<JsonNode>(Argo)
