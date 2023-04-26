package org.http4k.format

import com.ubertob.kondor.json.jsonnode.JsonNode

class KondorJsonTest : JsonContract<JsonNode>(KondorJson) {
    override val prettyString = """{
  "hello": "world"
}"""
}
