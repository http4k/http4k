package org.http4k.jsonrpc

import tools.jackson.databind.JsonNode
import org.http4k.format.Jackson

class JacksonAutoMappingJsonRpcServiceTest : AutoMappingJsonRpcServiceContract<JsonNode>(Jackson)
