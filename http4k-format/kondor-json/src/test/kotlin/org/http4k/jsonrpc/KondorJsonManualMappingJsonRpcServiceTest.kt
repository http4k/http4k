package org.http4k.jsonrpc

import com.ubertob.kondor.json.jsonnode.JsonNode
import org.http4k.format.KondorJson

class KondorJsonManualMappingJsonRpcServiceTest : ManualMappingJsonRpcServiceContract<JsonNode>(KondorJson())
