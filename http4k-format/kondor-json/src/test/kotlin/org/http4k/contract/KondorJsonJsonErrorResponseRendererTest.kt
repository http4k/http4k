package org.http4k.contract

import com.ubertob.kondor.json.jsonnode.JsonNode
import org.http4k.format.KondorJson

class KondorJsonJsonErrorResponseRendererTest : JsonErrorResponseRendererContract<JsonNode>(KondorJson())
