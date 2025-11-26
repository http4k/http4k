package org.http4k.jsonrpc

import org.http4k.format.Jackson
import tools.jackson.databind.JsonNode

class JacksonManualMappingJsonRpcServiceTest : ManualMappingJsonRpcServiceContract<JsonNode>(Jackson)
