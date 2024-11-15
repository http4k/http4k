package org.http4k.jsonrpc

import argo.jdom.JsonNode
import org.http4k.format.Argo

class ArgoManualMappingJsonRpcServiceTest : ManualMappingJsonRpcServiceContract<JsonNode>(Argo)
